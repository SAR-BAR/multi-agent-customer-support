package com.sarbar.support.adapter.out.agent.llm;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.model.ProblemType;
import com.sarbar.support.domain.port.out.BillingAgent;
import com.sarbar.support.domain.record.post.EvidenceReplyContent;
import com.sarbar.support.domain.record.post.HandoffContent;
import com.sarbar.support.domain.record.post.ProposalContent;
import com.sarbar.support.domain.record.post.WithdrawalContent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("local")
public class BillingAgentLlm implements BillingAgent {

  private static final Logger log = LoggerFactory.getLogger(BillingAgentLlm.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final BillingAssistant assistant;

  public BillingAgentLlm(BillingAssistant assistant) {
    this.assistant = assistant;
  }

  @Override
  public List<CasePost> investigate(List<CasePost> boardView) {
    String formatted = BoardFormatter.format(boardView);
    Long caseId = boardView.getFirst().caseId();
    try {
      SpecialistResponse response = assistant.investigate(formatted);
      return toPosts(response, caseId);
    } catch (Exception e) {
      log.warn("Billing agent failed, retrying: {}", e.getMessage());
      try {
        SpecialistResponse response = assistant.investigate(formatted);
        return toPosts(response, caseId);
      } catch (Exception retryEx) {
        log.error("Billing agent retry failed: {}", retryEx.getMessage());
        throw new AgentFailureException("Billing agent failed after retry", retryEx);
      }
    }
  }

  private List<CasePost> toPosts(SpecialistResponse response, Long caseId) {
    List<CasePost> posts = new ArrayList<>();
    if (response.actions() == null) return posts;

    for (SpecialistResponse.Action action : response.actions()) {
      CasePost post = toPost(action, caseId);
      if (post != null) posts.add(post);
    }
    return posts;
  }

  private CasePost toPost(SpecialistResponse.Action action, Long caseId) {
    Instant now = Instant.now();
    List<String> evidenceIds = action.evidenceIds() != null ? action.evidenceIds() : List.of();

    return switch (action.type()) {
      case "PROPOSAL" -> {
        ProblemType pt = parseProblemType(action.problemType());
        ProposalContent content =
            new ProposalContent(
                pt, action.action(), action.amountPaise(), evidenceIds, action.reasoning());
        yield new CasePost(
            null,
            caseId,
            PostAuthor.BILLING,
            PostType.PROPOSAL,
            toJson(content),
            evidenceIds,
            null,
            now);
      }
      case "WITHDRAWAL" -> {
        WithdrawalContent content =
            new WithdrawalContent(action.proposalPostId(), action.reasoning());
        yield new CasePost(
            null,
            caseId,
            PostAuthor.BILLING,
            PostType.WITHDRAWAL,
            toJson(content),
            List.of(),
            action.proposalPostId(),
            now);
      }
      case "HANDOFF" -> {
        ProblemType pt = parseProblemType(action.problemType());
        HandoffContent content = new HandoffContent(pt, PostAuthor.RESOLUTION, action.reasoning());
        yield new CasePost(
            null,
            caseId,
            PostAuthor.BILLING,
            PostType.HANDOFF,
            toJson(content),
            List.of(),
            null,
            now);
      }
      case "EVIDENCE_REPLY" -> {
        EvidenceReplyContent content =
            new EvidenceReplyContent(action.challengePostId(), evidenceIds, action.reasoning());
        yield new CasePost(
            null,
            caseId,
            PostAuthor.BILLING,
            PostType.EVIDENCE_REPLY,
            toJson(content),
            evidenceIds,
            action.challengePostId(),
            now);
      }
      default -> {
        log.warn("Unknown action type from Billing LLM: {}", action.type());
        yield null;
      }
    };
  }

  private ProblemType parseProblemType(String type) {
    try {
      return ProblemType.valueOf(type);
    } catch (Exception e) {
      return ProblemType.OTHER;
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
