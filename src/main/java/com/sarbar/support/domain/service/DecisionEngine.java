package com.sarbar.support.domain.service;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.DecisionType;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.model.VerdictType;
import com.sarbar.support.domain.record.Decision;
import com.sarbar.support.domain.record.post.MoodContent;
import com.sarbar.support.domain.record.post.ProposalContent;
import com.sarbar.support.domain.record.post.VerdictContent;
import com.sarbar.support.domain.record.post.WithdrawalContent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class DecisionEngine {

  static final int DEFAULT_AUTO_LIMIT_PAISE = 500000;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final int autoLimitPaise;

  public DecisionEngine() {
    this(DEFAULT_AUTO_LIMIT_PAISE);
  }

  public DecisionEngine(int autoLimitPaise) {
    this.autoLimitPaise = autoLimitPaise;
  }

  public Decision decide(List<CasePost> board) {
    Decision escalationOverride = checkEscalationTriggers(board);
    if (escalationOverride != null) {
      return escalationOverride;
    }

    List<CasePost> proposals = board.stream().filter(p -> p.type() == PostType.PROPOSAL).toList();
    List<CasePost> withdrawals =
        board.stream().filter(p -> p.type() == PostType.WITHDRAWAL).toList();
    Set<Long> withdrawnIds =
        withdrawals.stream()
            .map(w -> parse(w, WithdrawalContent.class).proposalPostId())
            .collect(Collectors.toSet());
    List<CasePost> activeProposals =
        proposals.stream().filter(p -> !withdrawnIds.contains(p.id())).toList();

    if (activeProposals.isEmpty()) {
      boolean hasProblems = board.stream().anyMatch(p -> p.type() == PostType.PROBLEM);
      if (!hasProblems) {
        return new Decision(DecisionType.DENY, List.of(), "No problems found on the board");
      }
      return new Decision(DecisionType.ESCALATE, List.of(), "No active proposals to act on");
    }

    List<CasePost> moneyProposals =
        activeProposals.stream()
            .filter(p -> parse(p, ProposalContent.class).involvesMoney())
            .toList();

    if (moneyProposals.isEmpty()) {
      return new Decision(
          DecisionType.EXECUTE,
          activeProposals.stream().map(CasePost::id).toList(),
          "No money involved, safe to execute");
    }

    List<CasePost> verdicts = board.stream().filter(p -> p.type() == PostType.VERDICT).toList();

    for (CasePost moneyProposal : moneyProposals) {
      VerdictContent verdict = findVerdictForProposal(verdicts, moneyProposal.id());
      if (verdict == null) {
        return new Decision(
            DecisionType.ESCALATE,
            List.of(),
            "Money proposal " + moneyProposal.id() + " has no verdict");
      }
      if (verdict.verdict() == VerdictType.HIGH_RISK) {
        return new Decision(
            DecisionType.ESCALATE,
            List.of(),
            "Risk flagged proposal " + moneyProposal.id() + " as HIGH_RISK");
      }
      if (verdict.verdict() == VerdictType.REVIEW) {
        return new Decision(
            DecisionType.ESCALATE,
            List.of(),
            "Risk flagged proposal " + moneyProposal.id() + " for REVIEW");
      }
    }

    int totalMoneyPaise =
        moneyProposals.stream().mapToInt(p -> parse(p, ProposalContent.class).amountPaise()).sum();

    if (totalMoneyPaise > autoLimitPaise) {
      return new Decision(
          DecisionType.ESCALATE,
          List.of(),
          "Total amount " + totalMoneyPaise + " exceeds auto-limit " + autoLimitPaise);
    }

    return new Decision(
        DecisionType.EXECUTE,
        activeProposals.stream().map(CasePost::id).toList(),
        "All proposals approved, total " + totalMoneyPaise + " within limit");
  }

  private Decision checkEscalationTriggers(List<CasePost> board) {
    List<CasePost> moods = board.stream().filter(p -> p.type() == PostType.MOOD).toList();
    for (CasePost mood : moods) {
      MoodContent content = parse(mood, MoodContent.class);
      if (content.legalOrSocialThreat()) {
        return new Decision(
            DecisionType.ESCALATE, List.of(), "Legal or social media threat detected");
      }
      if (content.asksForHuman()) {
        return new Decision(
            DecisionType.ESCALATE, List.of(), "Customer explicitly asked for a human");
      }
    }
    return null;
  }

  private VerdictContent findVerdictForProposal(List<CasePost> verdicts, Long proposalId) {
    return verdicts.stream()
        .map(v -> parse(v, VerdictContent.class))
        .filter(v -> v.proposalPostId().equals(proposalId))
        .findFirst()
        .orElse(null);
  }

  private <T> T parse(CasePost post, Class<T> type) {
    try {
      return MAPPER.readValue(post.contentJson(), type);
    } catch (JacksonException e) {
      throw new IllegalStateException(
          "Failed to parse post " + post.id() + " as " + type.getSimpleName(), e);
    }
  }
}
