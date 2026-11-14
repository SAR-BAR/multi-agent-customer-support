package com.sarbar.support.adapter.out.agent;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.ResolutionAgent;
import com.sarbar.support.domain.record.post.ChallengeContent;
import com.sarbar.support.domain.record.post.EvidenceReplyContent;
import com.sarbar.support.domain.record.post.FactsContent;
import com.sarbar.support.domain.record.post.HandoffContent;
import com.sarbar.support.domain.record.post.ProblemContent;
import com.sarbar.support.domain.record.post.ProposalContent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ScriptedResolutionAgent implements ResolutionAgent {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public List<CasePost> investigate(List<CasePost> boardView) {
    List<CasePost> responses = new ArrayList<>();
    CasePost challenge = findLatest(boardView, PostType.CHALLENGE);
    if (challenge != null) {
      responses.add(replyToChallenge(challenge, boardView));
      return responses;
    }

    CasePost handoff = findLatest(boardView, PostType.HANDOFF);
    if (handoff != null) {
      HandoffContent hc = parse(handoff, HandoffContent.class);
      if (hc.problemType().isPaymentProblem()) {
        return responses;
      }
    }

    FactsContent facts = findFacts(boardView);
    List<ProblemContent> problems = findProblems(boardView);

    for (ProblemContent problem : problems) {
      if (problem.type().isOrderProblem()) {
        responses.add(makeProposal(problem, facts, boardView));
      }
    }

    return responses;
  }

  private CasePost makeProposal(ProblemContent problem, FactsContent facts, List<CasePost> board) {
    Long caseId = board.getFirst().caseId();
    int amount = 0;
    String action = "acknowledge";
    String reasoning = "Scripted resolution for " + problem.type();
    List<String> evidenceIds = collectFactsEvidence(board);

    if (facts != null) {
      switch (problem.type()) {
        case LATE_DELIVERY -> {
          action = "refund_delivery_fee";
          amount = facts.facts().order().deliveryFeePaise();
          reasoning =
              "Late delivery by "
                  + facts.facts().delivery().delayDays()
                  + " days, refunding delivery fee";
        }
        case WRONG_ITEM, NOT_RECEIVED -> {
          action = "full_refund";
          amount = facts.facts().refundableAmountPaise();
          reasoning = "Item issue, full refund of refundable amount";
        }
        case REFUND_REQUEST -> {
          action = "full_refund";
          amount = facts.facts().refundableAmountPaise();
          reasoning = "Customer requested refund";
        }
        default -> {
          action = "investigate";
          reasoning = "Order problem requires investigation: " + problem.type();
        }
      }
    }

    ProposalContent content =
        new ProposalContent(problem.type(), action, amount, evidenceIds, reasoning);
    return new CasePost(
        null,
        caseId,
        PostAuthor.RESOLUTION,
        PostType.PROPOSAL,
        toJson(content),
        evidenceIds,
        null,
        Instant.now());
  }

  private CasePost replyToChallenge(CasePost challenge, List<CasePost> board) {
    ChallengeContent cc = parse(challenge, ChallengeContent.class);
    List<String> evidenceIds = collectFactsEvidence(board);
    EvidenceReplyContent reply =
        new EvidenceReplyContent(
            challenge.id(), evidenceIds, "Evidence supports the proposal as stated");
    return new CasePost(
        null,
        challenge.caseId(),
        PostAuthor.RESOLUTION,
        PostType.EVIDENCE_REPLY,
        toJson(reply),
        evidenceIds,
        challenge.id(),
        Instant.now());
  }

  private FactsContent findFacts(List<CasePost> board) {
    return board.stream()
        .filter(p -> p.type() == PostType.FACTS)
        .reduce((a, b) -> b)
        .map(p -> parse(p, FactsContent.class))
        .orElse(null);
  }

  private List<ProblemContent> findProblems(List<CasePost> board) {
    return board.stream()
        .filter(p -> p.type() == PostType.PROBLEM)
        .map(p -> parse(p, ProblemContent.class))
        .toList();
  }

  private List<String> collectFactsEvidence(List<CasePost> board) {
    return board.stream()
        .filter(p -> p.type() == PostType.FACTS)
        .map(p -> String.valueOf(p.id()))
        .toList();
  }

  private CasePost findLatest(List<CasePost> board, PostType type) {
    return board.stream().filter(p -> p.type() == type).reduce((a, b) -> b).orElse(null);
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
}
