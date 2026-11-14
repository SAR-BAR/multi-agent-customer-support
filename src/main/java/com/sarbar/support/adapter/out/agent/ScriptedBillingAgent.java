package com.sarbar.support.adapter.out.agent;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.BillingAgent;
import com.sarbar.support.domain.record.post.EvidenceReplyContent;
import com.sarbar.support.domain.record.post.FactsContent;
import com.sarbar.support.domain.record.post.ProblemContent;
import com.sarbar.support.domain.record.post.ProposalContent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ScriptedBillingAgent implements BillingAgent {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public List<CasePost> investigate(List<CasePost> boardView) {
    List<CasePost> responses = new ArrayList<>();
    CasePost challenge = findLatest(boardView, PostType.CHALLENGE);
    if (challenge != null) {
      responses.add(replyToChallenge(challenge, boardView));
      return responses;
    }

    FactsContent facts = findFacts(boardView);
    List<ProblemContent> problems = findProblems(boardView);

    for (ProblemContent problem : problems) {
      if (problem.type().isPaymentProblem()) {
        responses.add(makeProposal(problem, facts, boardView));
      }
    }

    return responses;
  }

  private CasePost makeProposal(ProblemContent problem, FactsContent facts, List<CasePost> board) {
    Long caseId = board.getFirst().caseId();
    int amount = 0;
    String action = "billing_review";
    String reasoning = "Scripted billing resolution for " + problem.type();
    List<String> evidenceIds = collectFactsEvidence(board);

    if (facts != null) {
      switch (problem.type()) {
        case DOUBLE_CHARGE -> {
          int totalPaid = facts.facts().payments().stream().mapToInt(p -> p.amountPaise()).sum();
          amount = totalPaid - facts.facts().order().totalPaise();
          if (amount <= 0) amount = facts.facts().order().totalPaise();
          action = "refund_duplicate_payment";
          reasoning = "Double charge detected, refunding excess payment";
        }
        case WRONG_FEE -> {
          boolean freeDelivery =
              facts.facts().membership() != null && facts.facts().membership().freeDelivery();
          if (freeDelivery) {
            amount = facts.facts().order().deliveryFeePaise();
            action = "refund_delivery_fee";
            reasoning = "Member with free delivery was charged delivery fee";
          } else {
            action = "review_fee";
            reasoning = "Delivery fee dispute, no free delivery membership";
          }
        }
        case DEBITED_ORDER_FAILED -> {
          amount = facts.facts().refundableAmountPaise();
          action = "refund_failed_order";
          reasoning = "Payment debited but order failed, full refund";
        }
        case INVOICE_COPY -> {
          action = "send_invoice_copy";
          reasoning = "Customer requested invoice copy";
        }
        default -> {
          action = "billing_investigation";
          reasoning = "Payment issue requires investigation: " + problem.type();
        }
      }
    }

    ProposalContent content =
        new ProposalContent(problem.type(), action, amount, evidenceIds, reasoning);
    return new CasePost(
        null,
        caseId,
        PostAuthor.BILLING,
        PostType.PROPOSAL,
        toJson(content),
        evidenceIds,
        null,
        Instant.now());
  }

  private CasePost replyToChallenge(CasePost challenge, List<CasePost> board) {
    List<String> evidenceIds = collectFactsEvidence(board);
    EvidenceReplyContent reply =
        new EvidenceReplyContent(
            challenge.id(), evidenceIds, "Billing evidence confirms the proposal");
    return new CasePost(
        null,
        challenge.caseId(),
        PostAuthor.BILLING,
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
