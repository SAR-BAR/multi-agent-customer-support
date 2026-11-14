package com.sarbar.support.domain.service;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.model.ProblemType;
import com.sarbar.support.domain.record.NextAction;
import com.sarbar.support.domain.record.NextAction.Actor;
import com.sarbar.support.domain.record.post.ChallengeContent;
import com.sarbar.support.domain.record.post.HandoffContent;
import com.sarbar.support.domain.record.post.ProblemContent;
import com.sarbar.support.domain.record.post.ProposalContent;
import com.sarbar.support.domain.record.post.VerdictContent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class Referee {

  static final int MAX_TURNS = 20;
  static final int MAX_CHALLENGE_ROUNDS = 2;
  static final int MAX_HANDOFFS = 1;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public NextAction decideNext(List<CasePost> board) {
    if (board.isEmpty()) {
      return new NextAction(Actor.ESCALATE, List.of(), "Empty board");
    }

    int agentTurns = countAgentTurns(board);
    if (agentTurns >= MAX_TURNS) {
      return new NextAction(
          Actor.ESCALATE, List.of(), "Max agent turns reached (" + MAX_TURNS + ")");
    }

    CasePost latest = board.getLast();

    return switch (latest.type()) {
      case PROBLEM -> routeProblem(board, latest);
      case FACTS -> routeAfterFacts(board);
      case PROPOSAL -> routeProposal(board, latest);
      case WITHDRAWAL -> checkAllCovered(board);
      case HANDOFF -> routeHandoff(board, latest);
      case CHALLENGE -> routeChallenge(latest);
      case EVIDENCE_REPLY ->
          new NextAction(Actor.RISK, List.of(latest.id()), "Evidence reply needs Risk verdict");
      case VERDICT -> checkAllCovered(board);
      case DECISION, ESCALATED ->
          new NextAction(
              Actor.COMMUNICATOR, List.of(latest.id()), "Decision made, Communicator drafts reply");
      case REPLY, HANDOFF_BRIEF -> new NextAction(Actor.DONE, List.of(), "Case complete");
      case HUMAN_DECISION ->
          new NextAction(
              Actor.COMMUNICATOR, List.of(latest.id()), "Human decided, Communicator drafts reply");
      case MOOD, SAGA_STEP -> decideNext(board.subList(0, board.size() - 1));
    };
  }

  private NextAction routeProblem(List<CasePost> board, CasePost problemPost) {
    ProblemContent content = parseContent(problemPost, ProblemContent.class);
    if (content.type() == ProblemType.ORDER_STATUS) {
      return new NextAction(
          Actor.DECISION_ENGINE,
          List.of(problemPost.id()),
          "ORDER_STATUS answered from FACTS directly");
    }

    boolean hasFacts = board.stream().anyMatch(p -> p.type() == PostType.FACTS);
    if (!hasFacts) {
      return new NextAction(
          Actor.DECISION_ENGINE,
          List.of(problemPost.id()),
          "Need FACTS before routing to specialist");
    }

    return routeToSpecialist(content.type(), problemPost.id());
  }

  private NextAction routeAfterFacts(List<CasePost> board) {
    List<CasePost> problems = board.stream().filter(p -> p.type() == PostType.PROBLEM).toList();
    if (problems.isEmpty()) {
      return new NextAction(Actor.ESCALATE, List.of(), "FACTS posted but no problems found");
    }

    boolean allOrderStatus =
        problems.stream()
            .allMatch(
                p -> {
                  ProblemContent c = parseContent(p, ProblemContent.class);
                  return c.type() == ProblemType.ORDER_STATUS;
                });
    if (allOrderStatus) {
      return new NextAction(
          Actor.DECISION_ENGINE,
          problems.stream().map(CasePost::id).toList(),
          "All problems are ORDER_STATUS");
    }

    CasePost firstUnhandled =
        problems.stream().filter(p -> !isHandled(board, p)).findFirst().orElse(null);
    if (firstUnhandled == null) {
      return checkAllCovered(board);
    }

    ProblemContent content = parseContent(firstUnhandled, ProblemContent.class);
    return routeToSpecialist(content.type(), firstUnhandled.id());
  }

  private NextAction routeProposal(List<CasePost> board, CasePost proposalPost) {
    ProposalContent content = parseContent(proposalPost, ProposalContent.class);
    if (content.involvesMoney()) {
      return new NextAction(
          Actor.RISK, List.of(proposalPost.id()), "Money proposal needs Risk review");
    }
    return checkAllCovered(board);
  }

  private NextAction routeHandoff(List<CasePost> board, CasePost handoffPost) {
    long handoffCount = board.stream().filter(p -> p.type() == PostType.HANDOFF).count();
    if (handoffCount > MAX_HANDOFFS) {
      return new NextAction(
          Actor.ESCALATE,
          List.of(handoffPost.id()),
          "Max handoffs exceeded (" + MAX_HANDOFFS + ")");
    }
    HandoffContent content = parseContent(handoffPost, HandoffContent.class);
    Actor target =
        content.targetSpecialist() == PostAuthor.BILLING ? Actor.BILLING : Actor.RESOLUTION;
    return new NextAction(target, List.of(handoffPost.id()), "Handoff to " + target);
  }

  private NextAction routeChallenge(CasePost challengePost) {
    return new NextAction(
        Actor.RESOLUTION, List.of(challengePost.id()), "Challenge needs specialist response");
  }

  NextAction checkAllCovered(List<CasePost> board) {
    List<CasePost> problems = board.stream().filter(p -> p.type() == PostType.PROBLEM).toList();
    List<CasePost> proposals = board.stream().filter(p -> p.type() == PostType.PROPOSAL).toList();
    List<CasePost> withdrawals =
        board.stream().filter(p -> p.type() == PostType.WITHDRAWAL).toList();

    Set<Long> withdrawnProposalIds =
        withdrawals.stream()
            .map(
                w ->
                    parseContent(w, com.sarbar.support.domain.record.post.WithdrawalContent.class)
                        .proposalPostId())
            .collect(Collectors.toSet());

    List<CasePost> activeProposals =
        proposals.stream().filter(p -> !withdrawnProposalIds.contains(p.id())).toList();

    boolean specialistAttempted = !proposals.isEmpty();
    if (activeProposals.isEmpty() && specialistAttempted && !problems.isEmpty()) {
      return new NextAction(
          Actor.ESCALATE, List.of(), "All proposals withdrawn, no resolution possible");
    }

    boolean allProblemsHandled = problems.stream().allMatch(p -> isHandled(board, p));
    if (!allProblemsHandled) {
      CasePost unhandled = problems.stream().filter(p -> !isHandled(board, p)).findFirst().get();
      ProblemContent content = parseContent(unhandled, ProblemContent.class);
      return routeToSpecialist(content.type(), unhandled.id());
    }

    boolean allMoneyProposalsHaveVerdict =
        activeProposals.stream()
            .allMatch(
                p -> {
                  ProposalContent content = parseContent(p, ProposalContent.class);
                  if (!content.involvesMoney()) return true;
                  return hasVerdict(board, p.id());
                });

    if (!allMoneyProposalsHaveVerdict) {
      CasePost unreviewed =
          activeProposals.stream()
              .filter(
                  p -> {
                    ProposalContent c = parseContent(p, ProposalContent.class);
                    return c.involvesMoney() && !hasVerdict(board, p.id());
                  })
              .findFirst()
              .get();
      return new NextAction(
          Actor.RISK, List.of(unreviewed.id()), "Money proposal awaiting Risk verdict");
    }

    return new NextAction(
        Actor.DECISION_ENGINE,
        activeProposals.stream().map(CasePost::id).toList(),
        "All problems covered and all money proposals have verdicts");
  }

  private boolean isHandled(List<CasePost> board, CasePost problemPost) {
    ProblemContent problemContent = parseContent(problemPost, ProblemContent.class);
    ProblemType problemType = problemContent.type();

    boolean hasActiveProposal =
        board.stream()
            .filter(p -> p.type() == PostType.PROPOSAL)
            .anyMatch(
                p -> {
                  ProposalContent c = parseContent(p, ProposalContent.class);
                  return c.problemType() == problemType && !isWithdrawn(board, p.id());
                });

    boolean hasHandoff =
        board.stream()
            .filter(p -> p.type() == PostType.HANDOFF)
            .anyMatch(
                p -> {
                  HandoffContent c = parseContent(p, HandoffContent.class);
                  return c.problemType() == problemType;
                });

    return hasActiveProposal || hasHandoff;
  }

  private boolean isWithdrawn(List<CasePost> board, Long proposalId) {
    return board.stream()
        .filter(p -> p.type() == PostType.WITHDRAWAL)
        .anyMatch(
            w ->
                parseContent(w, com.sarbar.support.domain.record.post.WithdrawalContent.class)
                    .proposalPostId()
                    .equals(proposalId));
  }

  private boolean hasVerdict(List<CasePost> board, Long proposalId) {
    return board.stream()
        .filter(p -> p.type() == PostType.VERDICT)
        .anyMatch(v -> parseContent(v, VerdictContent.class).proposalPostId().equals(proposalId));
  }

  int challengeRoundsForProposal(List<CasePost> board, Long proposalId) {
    return (int)
        board.stream()
            .filter(p -> p.type() == PostType.CHALLENGE)
            .filter(
                p -> parseContent(p, ChallengeContent.class).proposalPostId().equals(proposalId))
            .count();
  }

  private NextAction routeToSpecialist(ProblemType type, Long postId) {
    if (type.isPaymentProblem()) {
      return new NextAction(Actor.BILLING, List.of(postId), "Payment problem routed to Billing");
    }
    return new NextAction(Actor.RESOLUTION, List.of(postId), "Order problem routed to Resolution");
  }

  private int countAgentTurns(List<CasePost> board) {
    return (int)
        board.stream()
            .filter(p -> p.author() != PostAuthor.SYSTEM && p.author() != PostAuthor.HUMAN)
            .count();
  }

  private <T> T parseContent(CasePost post, Class<T> type) {
    try {
      return MAPPER.readValue(post.contentJson(), type);
    } catch (JacksonException e) {
      throw new IllegalStateException(
          "Failed to parse post " + post.id() + " content as " + type.getSimpleName(), e);
    }
  }
}
