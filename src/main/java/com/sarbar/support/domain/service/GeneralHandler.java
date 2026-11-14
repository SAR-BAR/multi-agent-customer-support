package com.sarbar.support.domain.service;

import com.sarbar.support.domain.model.GeneralType;
import com.sarbar.support.domain.port.out.ApprovedAnswerRepository;
import com.sarbar.support.domain.record.GeneralResponse;

public class GeneralHandler {

  private final ApprovedAnswerRepository approvedAnswerRepository;

  public GeneralHandler(ApprovedAnswerRepository approvedAnswerRepository) {
    this.approvedAnswerRepository = approvedAnswerRepository;
  }

  public GeneralResponse handle(GeneralType type, String topic) {
    return switch (type) {
      case FAQ -> handleFaq(topic);
      case ACCOUNT_CHANGE ->
          new GeneralResponse(
              "For account changes such as email, phone, or address updates, please visit your account settings page. Changes cannot be made through chat.",
              false,
              "ACCOUNT_CHANGE");
      case PRE_SALES ->
          new GeneralResponse(
              "Pre-sales enquiries are outside the scope of this assistant. Please visit our product pages or contact our sales team.",
              false,
              "PRE_SALES");
      case FEEDBACK ->
          new GeneralResponse(
              "Thank you for your feedback. We have noted it for our team.", true, "FEEDBACK");
      case OFF_TOPIC ->
          new GeneralResponse(
              "I'm sorry, but I can only help with order and payment related queries.",
              false,
              "OFF_TOPIC");
      case UNCLEAR ->
          new GeneralResponse(
              "Could you please clarify your question? I want to make sure I help you with the right thing.",
              false,
              "UNCLEAR");
      case NONE ->
          new GeneralResponse(
              "I'm sorry, I couldn't understand your message. Could you please rephrase it?",
              false,
              "NONE");
    };
  }

  private GeneralResponse handleFaq(String topic) {
    if (topic == null || topic.isBlank()) {
      return approvedAnswerRepository
          .findByGeneralType(GeneralType.FAQ)
          .map(a -> new GeneralResponse(a.answer(), false, "FAQ"))
          .orElse(
              new GeneralResponse(
                  "I don't have an answer for that question. Let me connect you with a support agent.",
                  true,
                  "FAQ_NOT_FOUND"));
    }
    return approvedAnswerRepository
        .findByGeneralTypeAndTopic(GeneralType.FAQ, topic)
        .map(a -> new GeneralResponse(a.answer(), false, "FAQ"))
        .orElse(
            new GeneralResponse(
                "I don't have an answer for that question. Let me connect you with a support agent.",
                true,
                "FAQ_NOT_FOUND"));
  }
}
