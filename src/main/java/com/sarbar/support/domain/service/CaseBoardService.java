package com.sarbar.support.domain.service;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.CasePostRepository;
import java.util.List;
import java.util.Set;

public class CaseBoardService {

  private static final Set<PostType> CITABLE_TYPES =
      Set.of(
          PostType.FACTS,
          PostType.PROPOSAL,
          PostType.CHALLENGE,
          PostType.EVIDENCE_REPLY,
          PostType.VERDICT);

  private final CasePostRepository casePostRepository;

  public CaseBoardService(CasePostRepository casePostRepository) {
    this.casePostRepository = casePostRepository;
  }

  public CasePost addPost(CasePost post) {
    validateEvidenceIds(post);
    validateReplyTo(post);
    return casePostRepository.append(post);
  }

  public List<CasePost> allPosts(Long caseId) {
    return casePostRepository.findByCaseId(caseId);
  }

  private void validateEvidenceIds(CasePost post) {
    if (post.evidenceIds() == null || post.evidenceIds().isEmpty()) {
      return;
    }
    for (String evidenceIdStr : post.evidenceIds()) {
      Long evidenceId = parsePostId(evidenceIdStr, "evidenceId");
      CasePost cited =
          casePostRepository
              .findById(evidenceId)
              .orElseThrow(
                  () -> new IllegalArgumentException("Evidence post not found: " + evidenceId));
      if (!cited.caseId().equals(post.caseId())) {
        throw new IllegalArgumentException(
            "Evidence post " + evidenceId + " belongs to a different case");
      }
      if (!CITABLE_TYPES.contains(cited.type())) {
        throw new IllegalArgumentException(
            "Post type " + cited.type() + " cannot be cited as evidence");
      }
    }
  }

  private void validateReplyTo(CasePost post) {
    if (post.replyTo() == null) {
      return;
    }
    CasePost parent =
        casePostRepository
            .findById(post.replyTo())
            .orElseThrow(
                () -> new IllegalArgumentException("replyTo post not found: " + post.replyTo()));
    if (!parent.caseId().equals(post.caseId())) {
      throw new IllegalArgumentException(
          "replyTo post " + post.replyTo() + " belongs to a different case");
    }
  }

  private static Long parsePostId(String value, String fieldName) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(fieldName + " is not a valid post ID: " + value);
    }
  }
}
