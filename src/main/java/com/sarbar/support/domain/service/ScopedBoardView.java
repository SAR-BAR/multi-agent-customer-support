package com.sarbar.support.domain.service;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.CasePostRepository;
import java.util.List;
import java.util.Set;

public class ScopedBoardView {

  private static final Set<PostType> SPECIALIST_TYPES =
      Set.of(
          PostType.FACTS,
          PostType.PROBLEM,
          PostType.PROPOSAL,
          PostType.WITHDRAWAL,
          PostType.HANDOFF,
          PostType.CHALLENGE,
          PostType.EVIDENCE_REPLY,
          PostType.VERDICT);

  private static final Set<PostType> RISK_TYPES =
      Set.of(
          PostType.FACTS,
          PostType.PROBLEM,
          PostType.MOOD,
          PostType.PROPOSAL,
          PostType.CHALLENGE,
          PostType.EVIDENCE_REPLY,
          PostType.VERDICT);

  private static final Set<PostType> COMMUNICATOR_TYPES =
      Set.of(
          PostType.DECISION,
          PostType.SAGA_STEP,
          PostType.ESCALATED,
          PostType.PROBLEM,
          PostType.FACTS);

  private final CasePostRepository casePostRepository;

  public ScopedBoardView(CasePostRepository casePostRepository) {
    this.casePostRepository = casePostRepository;
  }

  public List<CasePost> forResolution(Long caseId) {
    return filterByTypes(caseId, SPECIALIST_TYPES);
  }

  public List<CasePost> forBilling(Long caseId) {
    return filterByTypes(caseId, SPECIALIST_TYPES);
  }

  public List<CasePost> forRisk(Long caseId) {
    List<CasePost> posts = filterByTypes(caseId, RISK_TYPES);
    return posts.stream().map(ScopedBoardView::redactReasoningForRisk).toList();
  }

  public List<CasePost> forCommunicator(Long caseId) {
    List<CasePost> posts = filterByTypes(caseId, COMMUNICATOR_TYPES);
    return posts.stream().map(ScopedBoardView::redactInternalsForCommunicator).toList();
  }

  public List<CasePost> forReferee(Long caseId) {
    return casePostRepository.findByCaseId(caseId);
  }

  private List<CasePost> filterByTypes(Long caseId, Set<PostType> allowedTypes) {
    return casePostRepository.findByCaseId(caseId).stream()
        .filter(post -> allowedTypes.contains(post.type()))
        .toList();
  }

  private static CasePost redactReasoningForRisk(CasePost post) {
    if (post.type() != PostType.PROPOSAL) {
      return post;
    }
    String redacted = removeJsonField(post.contentJson(), "reasoning");
    return new CasePost(
        post.id(),
        post.caseId(),
        post.author(),
        post.type(),
        redacted,
        post.evidenceIds(),
        post.replyTo(),
        post.createdAt());
  }

  private static CasePost redactInternalsForCommunicator(CasePost post) {
    if (post.type() != PostType.DECISION) {
      return post;
    }
    String redacted = removeJsonField(post.contentJson(), "approvedProposalPostIds");
    return new CasePost(
        post.id(),
        post.caseId(),
        post.author(),
        post.type(),
        redacted,
        post.evidenceIds(),
        post.replyTo(),
        post.createdAt());
  }

  static String removeJsonField(String json, String fieldName) {
    String pattern =
        "\"" + fieldName + "\"\\s*:\\s*(?:\"[^\"]*\"|\\[[^]]*]|\\{[^}]*}|[^,}]+)\\s*,?";
    String result = json.replaceAll(pattern, "");
    result = result.replaceAll(",\\s*}", "}");
    return result;
  }
}
