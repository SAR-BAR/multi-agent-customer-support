package com.sarbar.support.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.record.NextAction;
import com.sarbar.support.domain.record.NextAction.Actor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefereeTest {

  private Referee referee;
  private final AtomicLong idGen = new AtomicLong(1);
  private static final Long CASE_ID = 1L;

  @BeforeEach
  void setUp() {
    referee = new Referee();
  }

  @Test
  void emptyBoard_escalates() {
    NextAction action = referee.decideNext(List.of());
    assertThat(action.actor()).isEqualTo(Actor.ESCALATE);
  }

  @Test
  void orderStatusProblem_goesDirectlyToDecisionEngine() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"ORDER_STATUS\",\"orderId\":\"ORD-001\",\"description\":\"where is my order\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.DECISION_ENGINE);
  }

  @Test
  void lateDeliveryProblem_afterFacts_routesToResolution() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.RESOLUTION);
  }

  @Test
  void doubleChargeProblem_afterFacts_routesToBilling() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"DOUBLE_CHARGE\",\"orderId\":\"ORD-002\",\"description\":\"charged twice\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.BILLING);
  }

  @Test
  void moneyProposal_routesToRisk() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.PROPOSAL,
                "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":500,\"evidenceIds\":[],\"reasoning\":\"late\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.RISK);
  }

  @Test
  void noMoneyProposal_checksIfAllCovered() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.PROPOSAL,
                "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"explain\",\"amountPaise\":0,\"evidenceIds\":[],\"reasoning\":\"info\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.DECISION_ENGINE);
  }

  @Test
  void evidenceReply_routesToRisk() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.PROPOSAL,
                "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":500,\"evidenceIds\":[],\"reasoning\":\"late\"}"),
            post(
                PostAuthor.RISK,
                PostType.CHALLENGE,
                "{\"proposalPostId\":3,\"question\":\"prove it\"}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.EVIDENCE_REPLY,
                "{\"challengePostId\":4,\"evidenceIds\":[\"2\"],\"explanation\":\"see facts\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.RISK);
  }

  @Test
  void decision_routesToCommunicator() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.DECISION,
                "{\"type\":\"EXECUTE\",\"approvedProposalPostIds\":[3],\"explanation\":\"approved\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.COMMUNICATOR);
  }

  @Test
  void escalated_routesToCommunicator() {
    List<CasePost> board =
        List.of(post(PostAuthor.SYSTEM, PostType.ESCALATED, "{\"reason\":\"over limit\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.COMMUNICATOR);
  }

  @Test
  void reply_marksDone() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.COMMUNICATOR,
                PostType.REPLY,
                "{\"message\":\"We have refunded you.\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.DONE);
  }

  @Test
  void handoff_routesToTargetSpecialist() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"WRONG_FEE\",\"orderId\":\"ORD-003\",\"description\":\"wrong fee\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.HANDOFF,
                "{\"problemType\":\"WRONG_FEE\",\"targetSpecialist\":\"BILLING\",\"reason\":\"payment problem\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.BILLING);
  }

  @Test
  void tooManyHandoffs_escalates() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"WRONG_FEE\",\"orderId\":\"ORD-003\",\"description\":\"wrong fee\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.HANDOFF,
                "{\"problemType\":\"WRONG_FEE\",\"targetSpecialist\":\"BILLING\",\"reason\":\"payment problem\"}"),
            post(
                PostAuthor.BILLING,
                PostType.HANDOFF,
                "{\"problemType\":\"WRONG_FEE\",\"targetSpecialist\":\"RESOLUTION\",\"reason\":\"actually order problem\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.ESCALATE);
  }

  @Test
  void maxTurns_escalates() {
    List<CasePost> board = new ArrayList<>();
    for (int i = 0; i < Referee.MAX_TURNS; i++) {
      board.add(
          post(
              PostAuthor.RESOLUTION,
              PostType.PROPOSAL,
              "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":0,\"evidenceIds\":[],\"reasoning\":\"x\"}"));
    }
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.ESCALATE);
    assertThat(action.reason()).contains("Max agent turns");
  }

  @Test
  void challenge_routesToChallengedSpecialist() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.PROBLEM,
                "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}"),
            post(PostAuthor.SYSTEM, PostType.FACTS, "{}"),
            post(
                PostAuthor.RESOLUTION,
                PostType.PROPOSAL,
                "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":500,\"evidenceIds\":[],\"reasoning\":\"late\"}"),
            post(
                PostAuthor.RISK,
                PostType.CHALLENGE,
                "{\"proposalPostId\":3,\"question\":\"prove it\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.RESOLUTION);
  }

  @Test
  void withdrawal_allWithdrawn_escalates() {
    CasePost problem =
        post(
            PostAuthor.SYSTEM,
            PostType.PROBLEM,
            "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}");
    CasePost facts = post(PostAuthor.SYSTEM, PostType.FACTS, "{}");
    CasePost proposal =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":500,\"evidenceIds\":[],\"reasoning\":\"late\"}");
    CasePost withdrawal =
        post(
            PostAuthor.RESOLUTION,
            PostType.WITHDRAWAL,
            "{\"proposalPostId\":" + proposal.id() + ",\"reason\":\"covered by other\"}");

    List<CasePost> board = List.of(problem, facts, proposal, withdrawal);
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.ESCALATE);
  }

  @Test
  void fullFlow_lateDelivery_reachesDecisionEngine() {
    CasePost problem =
        post(
            PostAuthor.SYSTEM,
            PostType.PROBLEM,
            "{\"type\":\"LATE_DELIVERY\",\"orderId\":\"ORD-001\",\"description\":\"late\"}");
    CasePost facts = post(PostAuthor.SYSTEM, PostType.FACTS, "{}");
    CasePost proposal =
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"problemType\":\"LATE_DELIVERY\",\"action\":\"refund\",\"amountPaise\":500,\"evidenceIds\":[],\"reasoning\":\"late\"}");
    CasePost verdict =
        post(
            PostAuthor.RISK,
            PostType.VERDICT,
            "{\"proposalPostId\":"
                + proposal.id()
                + ",\"verdict\":\"ALLOW\",\"signalsCited\":[\"low amount\"]}");

    List<CasePost> board = List.of(problem, facts, proposal, verdict);
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.DECISION_ENGINE);
  }

  @Test
  void humanDecision_routesToCommunicator() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.HUMAN,
                PostType.HUMAN_DECISION,
                "{\"decision\":\"approve\",\"explanation\":\"customer is right\"}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.COMMUNICATOR);
  }

  @Test
  void moodPost_isSkipped_looksBehind() {
    List<CasePost> board =
        List.of(
            post(
                PostAuthor.SYSTEM,
                PostType.DECISION,
                "{\"type\":\"EXECUTE\",\"approvedProposalPostIds\":[],\"explanation\":\"ok\"}"),
            post(
                PostAuthor.TRIAGE,
                PostType.MOOD,
                "{\"frustration\":0.3,\"legalOrSocialThreat\":false,\"asksForHuman\":false}"));
    NextAction action = referee.decideNext(board);
    assertThat(action.actor()).isEqualTo(Actor.COMMUNICATOR);
  }

  private CasePost post(PostAuthor author, PostType type, String contentJson) {
    return new CasePost(
        idGen.getAndIncrement(), CASE_ID, author, type, contentJson, null, null, Instant.now());
  }
}
