package com.sarbar.support.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.DecisionType;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.record.Decision;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecisionEngineTest {

  private DecisionEngine engine;
  private final AtomicLong idGen = new AtomicLong(1);
  private static final Long CASE_ID = 1L;

  @BeforeEach
  void setUp() {
    engine = new DecisionEngine(500000);
  }

  @Test
  void noMoneyProposal_executes() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"ORDER_STATUS\",\"orderId\":\"ORD-001\",\"description\":\"where is my order\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.PROPOSAL,
                "{\"problemType\":\"ORDER_STATUS\",\"action\":\"explain\",\"amountPaise\":0,\"evidenceIds\":[],\"reasoning\":\"info only\"}"));

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.EXECUTE);
    assertThat(decision.approvedProposalPostIds()).hasSize(1);
  }

  @Test
  void moneyProposal_underLimit_allowed_executes() {
    CasePost proposal =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":50000,\"evidenceIds\":[],\"reasoning\":\"late\"}");
    CasePost verdict =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal.id()
                + ",\"verdict\":\"ALLOW\",\"signalsCited\":[\"low\"]}");

    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"),
            proposal,
            verdict);

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.EXECUTE);
    assertThat(decision.approvedProposalPostIds()).contains(proposal.id());
  }

  @Test
  void moneyProposal_overLimit_escalates() {
    CasePost proposal =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":600000,\"evidenceIds\":[],\"reasoning\":\"late\"}");
    CasePost verdict =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal.id()
                + ",\"verdict\":\"ALLOW\",\"signalsCited\":[\"low\"]}");

    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            proposal,
            verdict);

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.ESCALATE);
    assertThat(decision.explanation()).contains("exceeds auto-limit");
  }

  @Test
  void highRiskVerdict_escalates() {
    CasePost proposal =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"REFUND_REQUEST\",\"action\":\"refund\",\"amountPaise\":100000,\"evidenceIds\":[],\"reasoning\":\"refund\"}");
    CasePost verdict =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal.id()
                + ",\"verdict\":\"HIGH_RISK\",\"signalsCited\":[\"suspicious\"]}");

    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"REFUND_REQUEST\",\"orderId\":\"ORD-001\",\"description\":\"refund please\"}"),
            proposal,
            verdict);

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.ESCALATE);
    assertThat(decision.explanation()).contains("HIGH_RISK");
  }

  @Test
  void reviewVerdict_escalates() {
    CasePost proposal =
        post(
            PostAuthor.BILLING,
            PostType.PROPOSAL,
            "{\"problemType\":\"DOUBLE_CHARGE\",\"action\":\"refund\",\"amountPaise\":180000,\"evidenceIds\":[],\"reasoning\":\"double\"}");
    CasePost verdict =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal.id()
                + ",\"verdict\":\"REVIEW\",\"signalsCited\":[\"unusual\"]}");

    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"DOUBLE_CHARGE\",\"orderId\":\"ORD-002\",\"description\":\"charged twice\"}"),
            proposal,
            verdict);

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.ESCALATE);
    assertThat(decision.explanation()).contains("REVIEW");
  }

  @Test
  void legalThreat_alwaysEscalates() {
    CasePost proposal =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":500,\"evidenceIds\":[],\"reasoning\":\"late\"}");
    CasePost verdict =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":" + proposal.id() + ",\"verdict\":\"ALLOW\",\"signalsCited\":[]}");

    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(
                PostAuthor.TRIAGE,
                PostType.MOOD,
                "{\"frustration\":0.9,\"legalOrSocialThreat\":true,\"asksForHuman\":false}"),
            proposal,
            verdict);

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.ESCALATE);
    assertThat(decision.explanation()).contains("Legal or social media threat");
  }

  @Test
  void customerAsksForHuman_escalates() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(
                PostAuthor.TRIAGE,
                PostType.MOOD,
                "{\"frustration\":0.5,\"legalOrSocialThreat\":false,\"asksForHuman\":true}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.PROPOSAL,
                "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":500,\"evidenceIds\":[],\"reasoning\":\"late\"}"));

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.ESCALATE);
    assertThat(decision.explanation()).contains("asked for a human");
  }

  @Test
  void noActiveProposals_noProblems_denies() {
    List<CasePost> board = List.of(post(PostAuthor.SYSTEM, PostType.FACTS, "{}"));
    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.DENY);
  }

  @Test
  void noActiveProposals_withProblems_escalates() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"));
    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.ESCALATE);
    assertThat(decision.explanation()).contains("No active proposals");
  }

  @Test
  void moneyProposal_noVerdict_escalates() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.PROPOSAL,
                "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":500,\"evidenceIds\":[],\"reasoning\":\"late\"}"));

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.ESCALATE);
    assertThat(decision.explanation()).contains("no verdict");
  }

  @Test
  void withdrawnProposal_isIgnored() {
    CasePost proposal =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":100000,\"evidenceIds\":[],\"reasoning\":\"late\"}");
    CasePost withdrawal =
        post(
            PostAuthor.RESOLUTION,
            PostType.WITHDRAWAL,
            "{\"proposalPostId\":" + proposal.id() + ",\"reason\":\"covered elsewhere\"}");
    CasePost proposal2 =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"explain\",\"amountPaise\":0,\"evidenceIds\":[],\"reasoning\":\"just info\"}");

    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            proposal,
            withdrawal,
            proposal2);

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.EXECUTE);
    assertThat(decision.approvedProposalPostIds()).containsExactly(proposal2.id());
    assertThat(decision.approvedProposalPostIds()).doesNotContain(proposal.id());
  }

  @Test
  void multipleMoneyProposals_combinedUnderLimit_executes() {
    CasePost proposal1 =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":200000,\"evidenceIds\":[],\"reasoning\":\"late\"}");
    CasePost verdict1 =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal1.id()
                + ",\"verdict\":\"ALLOW\",\"signalsCited\":[]}");
    CasePost proposal2 =
        post(
            PostAuthor.BILLING,
            PostType.PROPOSAL,
            "{\"problemType\":\"WRONG_FEE\",\"action\":\"refund\",\"amountPaise\":5000,\"evidenceIds\":[],\"reasoning\":\"wrong fee\"}");
    CasePost verdict2 =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal2.id()
                + ",\"verdict\":\"ALLOW\",\"signalsCited\":[]}");

    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"WRONG_FEE\",\"orderId\":\"ORD-001\",\"description\":\"wrong fee\"}"),
            proposal1,
            verdict1,
            proposal2,
            verdict2);

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.EXECUTE);
    assertThat(decision.approvedProposalPostIds()).contains(proposal1.id(), proposal2.id());
  }

  @Test
  void multipleMoneyProposals_combinedOverLimit_escalates() {
    CasePost proposal1 =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":300000,\"evidenceIds\":[],\"reasoning\":\"late\"}");
    CasePost verdict1 =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal1.id()
                + ",\"verdict\":\"ALLOW\",\"signalsCited\":[]}");
    CasePost proposal2 =
        post(
            PostAuthor.BILLING,
            PostType.PROPOSAL,
            "{\"problemType\":\"DOUBLE_CHARGE\",\"action\":\"refund\",\"amountPaise\":300000,\"evidenceIds\":[],\"reasoning\":\"double\"}");
    CasePost verdict2 =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal2.id()
                + ",\"verdict\":\"ALLOW\",\"signalsCited\":[]}");

    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"DOUBLE_CHARGE\",\"orderId\":\"ORD-001\",\"description\":\"double\"}"),
            proposal1,
            verdict1,
            proposal2,
            verdict2);

    Decision decision = engine.decide(board);
    assertThat(decision.type()).isEqualTo(DecisionType.ESCALATE);
    assertThat(decision.explanation()).contains("exceeds auto-limit");
  }

  private CasePost post(PostAuthor author, PostType type, String contentJson) {
    return new CasePost(
        idGen.getAndIncrement(), CASE_ID, author, type, contentJson, null, null, Instant.now());
  }
}
