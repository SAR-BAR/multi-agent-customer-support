package com.sarbar.support.adapter.out.agent;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.model.VerdictType;
import com.sarbar.support.domain.port.out.RiskAgent;
import com.sarbar.support.domain.record.post.ProposalContent;
import com.sarbar.support.domain.record.post.VerdictContent;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("scripted")
public class ScriptedRiskAgent implements RiskAgent {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final int HIGH_RISK_THRESHOLD_PAISE = 1_000_000;

  @Override
  public CasePost evaluate(List<CasePost> boardView, CasePost proposal) {
    ProposalContent pc = parse(proposal, ProposalContent.class);
    VerdictType verdict;
    List<String> signals;

    if (pc.amountPaise() >= HIGH_RISK_THRESHOLD_PAISE) {
      verdict = VerdictType.HIGH_RISK;
      signals = List.of("amount_exceeds_threshold", "amount=" + pc.amountPaise());
    } else if (pc.amountPaise() == 0) {
      verdict = VerdictType.ALLOW;
      signals = List.of("no_money_involved");
    } else {
      verdict = VerdictType.ALLOW;
      signals = List.of("amount_within_limits", "amount=" + pc.amountPaise());
    }

    VerdictContent content = new VerdictContent(proposal.id(), verdict, signals);
    return new CasePost(
        null,
        proposal.caseId(),
        PostAuthor.RISK,
        PostType.VERDICT,
        toJson(content),
        List.of(),
        proposal.id(),
        Instant.now());
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
