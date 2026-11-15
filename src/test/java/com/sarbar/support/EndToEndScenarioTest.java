package com.sarbar.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.CaseStatus;
import com.sarbar.support.domain.model.Conversation;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.CasePostRepository;
import com.sarbar.support.domain.port.out.CaseRepository;
import com.sarbar.support.domain.port.out.ConversationRepository;
import com.sarbar.support.domain.port.out.EscalationRepository;
import com.sarbar.support.domain.service.CaseProcessor;
import com.sarbar.support.domain.service.CaseProcessor.ProcessResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class EndToEndScenarioTest {

  @Autowired private CaseProcessor caseProcessor;
  @Autowired private ConversationRepository conversationRepository;
  @Autowired private CaseRepository caseRepository;
  @Autowired private CasePostRepository casePostRepository;
  @Autowired private EscalationRepository escalationRepository;

  @Test
  void scenario1_faqQuery_returnsApprovedAnswer() {
    Conversation conv = conversationRepository.save(new Conversation(null, 1L, Instant.now()));
    ProcessResult result = caseProcessor.process(conv.id(), "What is your return policy?");

    assertThat(result.caseId()).isNull();
    assertThat(result.customerMessage()).contains("return");
    assertThat(result.needsHuman()).isFalse();
  }

  @Test
  void scenario2_accountChange_returnsSettingsMessage() {
    Conversation conv = conversationRepository.save(new Conversation(null, 1L, Instant.now()));
    ProcessResult result =
        caseProcessor.process(conv.id(), "I want to change my email and update my account");

    assertThat(result.caseId()).isNull();
    assertThat(result.customerMessage()).contains("account settings");
    assertThat(result.needsHuman()).isFalse();
  }

  @Test
  void scenario3_lateDelivery_refundsDeliveryFee() {
    Conversation conv = conversationRepository.save(new Conversation(null, 1L, Instant.now()));
    ProcessResult result =
        caseProcessor.process(
            conv.id(), "My order ORD-001 is late, it was supposed to arrive days ago");

    assertThat(result.caseId()).isNotNull();
    assertThat(result.needsHuman()).isFalse();
    assertThat(result.customerMessage()).contains("resolved");

    List<CasePost> posts = casePostRepository.findByCaseId(result.caseId());
    assertThat(posts)
        .extracting(CasePost::type)
        .contains(
            PostType.PROBLEM,
            PostType.MOOD,
            PostType.FACTS,
            PostType.PROPOSAL,
            PostType.VERDICT,
            PostType.DECISION,
            PostType.REPLY);

    CasePost proposal =
        posts.stream().filter(p -> p.type() == PostType.PROPOSAL).findFirst().orElseThrow();
    assertThat(proposal.contentJson()).contains("LATE_DELIVERY");
    assertThat(proposal.contentJson()).contains("refund_delivery_fee");

    var supportCase = caseRepository.findById(result.caseId()).orElseThrow();
    assertThat(supportCase.status()).isEqualTo(CaseStatus.CLOSED);
  }

  @Test
  void scenario4_doubleCharge_refundsDuplicatePayment() {
    Conversation conv = conversationRepository.save(new Conversation(null, 1L, Instant.now()));
    ProcessResult result =
        caseProcessor.process(
            conv.id(), "I was charged twice for order ORD-002, please check the double charge");

    assertThat(result.caseId()).isNotNull();
    assertThat(result.needsHuman()).isFalse();

    List<CasePost> posts = casePostRepository.findByCaseId(result.caseId());
    assertThat(posts)
        .extracting(CasePost::type)
        .contains(
            PostType.PROBLEM,
            PostType.FACTS,
            PostType.PROPOSAL,
            PostType.VERDICT,
            PostType.DECISION,
            PostType.REPLY);

    CasePost proposal =
        posts.stream().filter(p -> p.type() == PostType.PROPOSAL).findFirst().orElseThrow();
    assertThat(proposal.contentJson()).contains("DOUBLE_CHARGE");
    assertThat(proposal.contentJson()).contains("refund_duplicate_payment");
  }

  @Test
  void scenario5_lateDeliveryAndWrongFee_twoProblems() {
    Conversation conv = conversationRepository.save(new Conversation(null, 2L, Instant.now()));
    ProcessResult result =
        caseProcessor.process(
            conv.id(),
            "My order ORD-003 was late and I was charged a wrong delivery fee even though I have membership");

    assertThat(result.caseId()).isNotNull();

    List<CasePost> posts = casePostRepository.findByCaseId(result.caseId());
    List<CasePost> problems = posts.stream().filter(p -> p.type() == PostType.PROBLEM).toList();
    assertThat(problems).hasSizeGreaterThanOrEqualTo(2);

    List<CasePost> proposals = posts.stream().filter(p -> p.type() == PostType.PROPOSAL).toList();
    assertThat(proposals).hasSizeGreaterThanOrEqualTo(1);

    boolean hasDeliveryFeeProposal =
        proposals.stream().anyMatch(p -> p.contentJson().contains("WRONG_FEE"));
    boolean hasLateDeliveryProposal =
        proposals.stream().anyMatch(p -> p.contentJson().contains("LATE_DELIVERY"));
    assertThat(hasDeliveryFeeProposal || hasLateDeliveryProposal).isTrue();
  }

  @Test
  void scenario6_riskEvaluatesProposal_verdictAllows() {
    Conversation conv = conversationRepository.save(new Conversation(null, 2L, Instant.now()));
    ProcessResult result =
        caseProcessor.process(
            conv.id(), "My order ORD-004 failed but money was debited from my account");

    assertThat(result.caseId()).isNotNull();

    List<CasePost> posts = casePostRepository.findByCaseId(result.caseId());
    List<CasePost> verdicts = posts.stream().filter(p -> p.type() == PostType.VERDICT).toList();
    assertThat(verdicts).isNotEmpty();

    CasePost verdict = verdicts.getFirst();
    assertThat(verdict.contentJson()).contains("ALLOW");
  }

  @Test
  void scenario7_overLimitAmount_escalates() {
    Conversation conv = conversationRepository.save(new Conversation(null, 1L, Instant.now()));
    ProcessResult result =
        caseProcessor.process(
            conv.id(),
            "I need a refund for order ORD-002, I was charged twice for the double charge");

    assertThat(result.caseId()).isNotNull();

    List<CasePost> posts = casePostRepository.findByCaseId(result.caseId());
    boolean hasDecisionOrEscalation =
        posts.stream()
            .anyMatch(p -> p.type() == PostType.DECISION || p.type() == PostType.ESCALATED);
    assertThat(hasDecisionOrEscalation).isTrue();
  }

  @Test
  void scenario8_legalThreat_escalatesImmediately() {
    Conversation conv = conversationRepository.save(new Conversation(null, 1L, Instant.now()));
    ProcessResult result =
        caseProcessor.process(
            conv.id(),
            "Order ORD-001 is late, I will take legal action and contact my lawyer if this is not resolved");

    assertThat(result.caseId()).isNotNull();
    assertThat(result.needsHuman()).isTrue();

    var escalation = escalationRepository.findByCaseId(result.caseId());
    assertThat(escalation).isPresent();

    var supportCase = caseRepository.findById(result.caseId()).orElseThrow();
    assertThat(supportCase.status()).isEqualTo(CaseStatus.ESCALATED);
  }
}
