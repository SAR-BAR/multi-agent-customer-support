package com.sarbar.support.domain.service;

import com.sarbar.support.domain.model.Case;
import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.CaseStatus;
import com.sarbar.support.domain.model.DecisionType;
import com.sarbar.support.domain.model.Escalation;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.BillingAgent;
import com.sarbar.support.domain.port.out.CaseRepository;
import com.sarbar.support.domain.port.out.CommunicatorAgent;
import com.sarbar.support.domain.port.out.EscalationRepository;
import com.sarbar.support.domain.port.out.ResolutionAgent;
import com.sarbar.support.domain.port.out.RiskAgent;
import com.sarbar.support.domain.port.out.TriageAgent;
import com.sarbar.support.domain.record.Decision;
import com.sarbar.support.domain.record.GeneralResponse;
import com.sarbar.support.domain.record.NextAction;
import com.sarbar.support.domain.record.ProblemRef;
import com.sarbar.support.domain.record.TriageFinding;
import com.sarbar.support.domain.record.post.DecisionContent;
import com.sarbar.support.domain.record.post.EscalatedContent;
import com.sarbar.support.domain.record.post.FactsContent;
import com.sarbar.support.domain.record.post.MoodContent;
import com.sarbar.support.domain.record.post.ProblemContent;
import java.time.Instant;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class CaseProcessor {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final TriageAgent triageAgent;
  private final ResolutionAgent resolutionAgent;
  private final BillingAgent billingAgent;
  private final RiskAgent riskAgent;
  private final CommunicatorAgent communicatorAgent;
  private final CaseBoardService caseBoardService;
  private final ScopedBoardView scopedBoardView;
  private final FactsComputer factsComputer;
  private final Referee referee;
  private final DecisionEngine decisionEngine;
  private final GeneralHandler generalHandler;
  private final CaseRepository caseRepository;
  private final EscalationRepository escalationRepository;

  public CaseProcessor(
      TriageAgent triageAgent,
      ResolutionAgent resolutionAgent,
      BillingAgent billingAgent,
      RiskAgent riskAgent,
      CommunicatorAgent communicatorAgent,
      CaseBoardService caseBoardService,
      ScopedBoardView scopedBoardView,
      FactsComputer factsComputer,
      Referee referee,
      DecisionEngine decisionEngine,
      GeneralHandler generalHandler,
      CaseRepository caseRepository,
      EscalationRepository escalationRepository) {
    this.triageAgent = triageAgent;
    this.resolutionAgent = resolutionAgent;
    this.billingAgent = billingAgent;
    this.riskAgent = riskAgent;
    this.communicatorAgent = communicatorAgent;
    this.caseBoardService = caseBoardService;
    this.scopedBoardView = scopedBoardView;
    this.factsComputer = factsComputer;
    this.referee = referee;
    this.decisionEngine = decisionEngine;
    this.generalHandler = generalHandler;
    this.caseRepository = caseRepository;
    this.escalationRepository = escalationRepository;
  }

  public ProcessResult process(Long conversationId, String message) {
    TriageFinding finding = triageAgent.analyze(List.of(), message);

    if (finding.generalType() != null
        && finding.generalType() != com.sarbar.support.domain.model.GeneralType.NONE) {
      String topic = finding.orderIds().isEmpty() ? null : finding.orderIds().getFirst();
      GeneralResponse response = generalHandler.handle(finding.generalType(), topic);
      return new ProcessResult(null, response.text(), response.needsHuman());
    }

    if (finding.orderIds().isEmpty()) {
      return new ProcessResult(null, "Please provide your order ID so I can help you.", false);
    }

    String orderId = finding.orderIds().getFirst();
    Case supportCase =
        caseRepository
            .findByConversationIdAndOrderId(conversationId, orderId)
            .orElseGet(
                () ->
                    caseRepository.save(
                        new Case(
                            null, conversationId, orderId, CaseStatus.OPEN, Instant.now(), null)));

    postTriageFindings(supportCase, finding);

    return runLoop(supportCase);
  }

  private void postTriageFindings(Case supportCase, TriageFinding finding) {
    Long caseId = supportCase.id();
    Instant now = Instant.now();

    for (ProblemRef ref : finding.problems()) {
      ProblemContent content = new ProblemContent(ref.type(), ref.orderId(), ref.description());
      caseBoardService.addPost(
          new CasePost(
              null,
              caseId,
              PostAuthor.SYSTEM,
              PostType.PROBLEM,
              toJson(content),
              List.of(),
              null,
              now));
    }

    MoodContent mood =
        new MoodContent(
            finding.frustration(), finding.legalOrSocialThreat(), finding.asksForHuman());
    caseBoardService.addPost(
        new CasePost(
            null, caseId, PostAuthor.SYSTEM, PostType.MOOD, toJson(mood), List.of(), null, now));

    if (!finding.orderIds().isEmpty()) {
      String orderId = finding.orderIds().getFirst();
      var facts = factsComputer.compute(orderId, now);
      FactsContent factsContent = new FactsContent(facts);
      caseBoardService.addPost(
          new CasePost(
              null,
              caseId,
              PostAuthor.SYSTEM,
              PostType.FACTS,
              toJson(factsContent),
              List.of(),
              null,
              now));
    }
  }

  private ProcessResult runLoop(Case supportCase) {
    Long caseId = supportCase.id();
    int maxIterations = 30;

    for (int i = 0; i < maxIterations; i++) {
      List<CasePost> board = caseBoardService.allPosts(caseId);
      NextAction next = referee.decideNext(board);

      switch (next.actor()) {
        case RESOLUTION -> {
          List<CasePost> scopedView = scopedBoardView.forResolution(caseId);
          List<CasePost> posts = resolutionAgent.investigate(scopedView);
          for (CasePost post : posts) {
            caseBoardService.addPost(post);
          }
        }
        case BILLING -> {
          List<CasePost> scopedView = scopedBoardView.forBilling(caseId);
          List<CasePost> posts = billingAgent.investigate(scopedView);
          for (CasePost post : posts) {
            caseBoardService.addPost(post);
          }
        }
        case RISK -> {
          List<CasePost> scopedView = scopedBoardView.forRisk(caseId);
          for (Long targetId : next.targetPostIds()) {
            CasePost proposal =
                board.stream().filter(p -> p.id().equals(targetId)).findFirst().orElseThrow();
            CasePost verdict = riskAgent.evaluate(scopedView, proposal);
            caseBoardService.addPost(verdict);
          }
        }
        case DECISION_ENGINE -> {
          Decision decision = decisionEngine.decide(board);
          DecisionContent content =
              new DecisionContent(
                  decision.type(), decision.approvedProposalPostIds(), decision.explanation());
          PostType postType =
              decision.type() == DecisionType.ESCALATE ? PostType.ESCALATED : PostType.DECISION;
          caseBoardService.addPost(
              new CasePost(
                  null,
                  caseId,
                  PostAuthor.SYSTEM,
                  postType,
                  toJson(content),
                  List.of(),
                  null,
                  Instant.now()));
          if (decision.type() == DecisionType.ESCALATE) {
            escalationRepository.save(
                new Escalation(null, caseId, decision.explanation(), Instant.now(), null));
            caseRepository.save(
                new Case(
                    supportCase.id(),
                    supportCase.conversationId(),
                    supportCase.orderId(),
                    CaseStatus.ESCALATED,
                    supportCase.openedAt(),
                    null));
          }
        }
        case COMMUNICATOR -> {
          List<CasePost> scopedView = scopedBoardView.forCommunicator(caseId);
          Decision latestDecision = findLatestDecision(board);
          CasePost reply = communicatorAgent.compose(scopedView, latestDecision);
          caseBoardService.addPost(reply);
        }
        case ESCALATE -> {
          EscalatedContent content = new EscalatedContent(next.reason());
          caseBoardService.addPost(
              new CasePost(
                  null,
                  caseId,
                  PostAuthor.SYSTEM,
                  PostType.ESCALATED,
                  toJson(content),
                  List.of(),
                  null,
                  Instant.now()));
          escalationRepository.save(
              new Escalation(null, caseId, next.reason(), Instant.now(), null));
          caseRepository.save(
              new Case(
                  supportCase.id(),
                  supportCase.conversationId(),
                  supportCase.orderId(),
                  CaseStatus.ESCALATED,
                  supportCase.openedAt(),
                  null));
          return new ProcessResult(
              caseId, "Your case has been escalated to a senior support specialist.", true);
        }
        case DONE -> {
          caseRepository.save(
              new Case(
                  supportCase.id(),
                  supportCase.conversationId(),
                  supportCase.orderId(),
                  CaseStatus.CLOSED,
                  supportCase.openedAt(),
                  Instant.now()));
          CasePost lastReply =
              board.stream()
                  .filter(p -> p.type() == PostType.REPLY)
                  .reduce((a, b) -> b)
                  .orElse(null);
          String customerMessage =
              lastReply != null ? extractReplyMessage(lastReply) : "Your case has been resolved.";
          return new ProcessResult(caseId, customerMessage, false);
        }
      }
    }

    return new ProcessResult(caseId, "Your case is being reviewed by our team.", true);
  }

  private Decision findLatestDecision(List<CasePost> board) {
    CasePost decisionPost =
        board.stream()
            .filter(p -> p.type() == PostType.DECISION || p.type() == PostType.ESCALATED)
            .reduce((a, b) -> b)
            .orElseThrow(() -> new IllegalStateException("No decision on board"));
    DecisionContent dc = parse(decisionPost, DecisionContent.class);
    return new Decision(dc.type(), dc.approvedProposalPostIds(), dc.explanation());
  }

  private String extractReplyMessage(CasePost replyPost) {
    try {
      var node = MAPPER.readTree(replyPost.contentJson());
      return node.get("message").asText();
    } catch (JacksonException e) {
      return "Your case has been resolved.";
    }
  }

  private <T> T parse(CasePost post, Class<T> type) {
    try {
      return MAPPER.readValue(post.contentJson(), type);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to parse post " + post.id(), e);
    }
  }

  private String toJson(Object obj) {
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize", e);
    }
  }

  public record ProcessResult(Long caseId, String customerMessage, boolean needsHuman) {}
}
