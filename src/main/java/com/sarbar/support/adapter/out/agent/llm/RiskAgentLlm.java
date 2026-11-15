package com.sarbar.support.adapter.out.agent.llm;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.model.VerdictType;
import com.sarbar.support.domain.port.out.RiskAgent;
import com.sarbar.support.domain.record.post.ChallengeContent;
import com.sarbar.support.domain.record.post.VerdictContent;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("local")
public class RiskAgentLlm implements RiskAgent {

  private static final Logger log = LoggerFactory.getLogger(RiskAgentLlm.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final RiskAssistant assistant;

  public RiskAgentLlm(RiskAssistant assistant) {
    this.assistant = assistant;
  }

  @Override
  public CasePost evaluate(List<CasePost> boardView, CasePost proposal) {
    String context = buildContext(boardView, proposal);
    Long caseId = proposal.caseId();
    try {
      RiskResponse response = assistant.evaluate(context);
      return toPost(response, caseId, proposal);
    } catch (Exception e) {
      log.warn("Risk agent failed, retrying: {}", e.getMessage());
      try {
        RiskResponse response = assistant.evaluate(context);
        return toPost(response, caseId, proposal);
      } catch (Exception retryEx) {
        log.error("Risk agent retry failed: {}", retryEx.getMessage());
        throw new AgentFailureException("Risk agent failed after retry", retryEx);
      }
    }
  }

  private String buildContext(List<CasePost> boardView, CasePost proposal) {
    var sb = new StringBuilder();
    sb.append("=== Case Board ===\n");
    sb.append(BoardFormatter.format(boardView));
    sb.append("\n=== Proposal to Evaluate ===\n");
    sb.append("[Post #").append(proposal.id()).append("] ");
    sb.append(proposal.contentJson());
    return sb.toString();
  }

  private CasePost toPost(RiskResponse response, Long caseId, CasePost proposal) {
    Instant now = Instant.now();

    if ("CHALLENGE".equals(response.type())) {
      ChallengeContent content = new ChallengeContent(proposal.id(), response.question());
      return new CasePost(
          null,
          caseId,
          PostAuthor.RISK,
          PostType.CHALLENGE,
          toJson(content),
          List.of(),
          proposal.id(),
          now);
    }

    VerdictType verdict = parseVerdict(response.verdict());
    List<String> signals = response.signalsCited() != null ? response.signalsCited() : List.of();
    VerdictContent content = new VerdictContent(proposal.id(), verdict, signals);
    return new CasePost(
        null,
        caseId,
        PostAuthor.RISK,
        PostType.VERDICT,
        toJson(content),
        List.of(),
        proposal.id(),
        now);
  }

  private VerdictType parseVerdict(String verdict) {
    try {
      return VerdictType.valueOf(verdict);
    } catch (Exception e) {
      return VerdictType.REVIEW;
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
