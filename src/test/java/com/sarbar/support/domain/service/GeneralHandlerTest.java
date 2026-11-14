package com.sarbar.support.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarbar.support.domain.model.ApprovedAnswer;
import com.sarbar.support.domain.model.GeneralType;
import com.sarbar.support.domain.port.out.ApprovedAnswerRepository;
import com.sarbar.support.domain.record.GeneralResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeneralHandlerTest {

  private StubApprovedAnswerRepository answerRepo;
  private GeneralHandler handler;

  @BeforeEach
  void setUp() {
    answerRepo = new StubApprovedAnswerRepository();
    handler = new GeneralHandler(answerRepo);
  }

  @Test
  void faq_withMatchingTopic_returnsApprovedAnswer() {
    answerRepo.add(
        new ApprovedAnswer(
            1L, GeneralType.FAQ, "return_policy", "You can return within 7 days.", 1));

    GeneralResponse response = handler.handle(GeneralType.FAQ, "return_policy");
    assertThat(response.text()).contains("return within 7 days");
    assertThat(response.needsHuman()).isFalse();
  }

  @Test
  void faq_withNoMatchingTopic_needsHuman() {
    GeneralResponse response = handler.handle(GeneralType.FAQ, "unknown_topic");
    assertThat(response.needsHuman()).isTrue();
    assertThat(response.reason()).isEqualTo("FAQ_NOT_FOUND");
  }

  @Test
  void faq_withNullTopic_fallsBackToGeneralType() {
    answerRepo.add(new ApprovedAnswer(1L, GeneralType.FAQ, "general", "Here is our FAQ.", 1));

    GeneralResponse response = handler.handle(GeneralType.FAQ, null);
    assertThat(response.text()).contains("FAQ");
    assertThat(response.needsHuman()).isFalse();
  }

  @Test
  void accountChange_returnsSettingsLink() {
    GeneralResponse response = handler.handle(GeneralType.ACCOUNT_CHANGE, null);
    assertThat(response.text()).contains("account settings");
    assertThat(response.needsHuman()).isFalse();
  }

  @Test
  void preSales_returnsOutOfScope() {
    GeneralResponse response = handler.handle(GeneralType.PRE_SALES, null);
    assertThat(response.text()).contains("outside the scope");
    assertThat(response.needsHuman()).isFalse();
  }

  @Test
  void feedback_needsHuman() {
    GeneralResponse response = handler.handle(GeneralType.FEEDBACK, null);
    assertThat(response.text()).contains("feedback");
    assertThat(response.needsHuman()).isTrue();
  }

  @Test
  void offTopic_returnsRefusal() {
    GeneralResponse response = handler.handle(GeneralType.OFF_TOPIC, null);
    assertThat(response.text()).contains("order and payment");
    assertThat(response.needsHuman()).isFalse();
  }

  @Test
  void unclear_asksClarification() {
    GeneralResponse response = handler.handle(GeneralType.UNCLEAR, null);
    assertThat(response.text()).contains("clarify");
    assertThat(response.needsHuman()).isFalse();
  }

  @Test
  void none_asksClarification() {
    GeneralResponse response = handler.handle(GeneralType.NONE, null);
    assertThat(response.text()).contains("rephrase");
    assertThat(response.needsHuman()).isFalse();
  }

  static class StubApprovedAnswerRepository implements ApprovedAnswerRepository {

    private final List<ApprovedAnswer> answers = new ArrayList<>();

    void add(ApprovedAnswer answer) {
      answers.add(answer);
    }

    @Override
    public Optional<ApprovedAnswer> findByGeneralType(GeneralType generalType) {
      return answers.stream().filter(a -> a.generalType() == generalType).findFirst();
    }

    @Override
    public Optional<ApprovedAnswer> findByGeneralTypeAndTopic(
        GeneralType generalType, String topic) {
      return answers.stream()
          .filter(a -> a.generalType() == generalType && a.topic().equals(topic))
          .findFirst();
    }
  }
}
