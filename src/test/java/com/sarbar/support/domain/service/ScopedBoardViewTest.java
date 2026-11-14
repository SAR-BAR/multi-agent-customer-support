package com.sarbar.support.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopedBoardViewTest {

  private CaseBoardServiceTest.InMemoryCasePostRepository repository;
  private ScopedBoardView scopedView;

  private static final Long CASE_ID = 1L;

  @BeforeEach
  void setUp() {
    repository = new CaseBoardServiceTest.InMemoryCasePostRepository();
    scopedView = new ScopedBoardView(repository);

    repository.append(post(PostAuthor.SYSTEM, PostType.PROBLEM, "{}"));
    repository.append(post(PostAuthor.TRIAGE, PostType.MOOD, "{}"));
    repository.append(post(PostAuthor.SYSTEM, PostType.FACTS, "{}"));
    repository.append(
        post(
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            "{\"action\":\"refund\",\"reasoning\":\"late delivery\",\"amountPaise\":500}"));
    repository.append(post(PostAuthor.RISK, PostType.CHALLENGE, "{}"));
    repository.append(post(PostAuthor.RESOLUTION, PostType.EVIDENCE_REPLY, "{}"));
    repository.append(post(PostAuthor.RISK, PostType.VERDICT, "{\"verdict\":\"ALLOW\"}"));
    repository.append(
        post(
            PostAuthor.SYSTEM,
            PostType.DECISION,
            "{\"type\":\"EXECUTE\",\"approvedProposalPostIds\":[4],\"explanation\":\"approved\"}"));
    repository.append(post(PostAuthor.SYSTEM, PostType.SAGA_STEP, "{}"));
    repository.append(post(PostAuthor.COMMUNICATOR, PostType.REPLY, "{}"));
  }

  @Test
  void resolution_seesSpecialistTypes() {
    List<CasePost> posts = scopedView.forResolution(CASE_ID);
    List<PostType> types = posts.stream().map(CasePost::type).toList();

    assertThat(types)
        .containsExactly(
            PostType.PROBLEM,
            PostType.FACTS,
            PostType.PROPOSAL,
            PostType.CHALLENGE,
            PostType.EVIDENCE_REPLY,
            PostType.VERDICT);
    assertThat(types).doesNotContain(PostType.MOOD, PostType.DECISION, PostType.REPLY);
  }

  @Test
  void billing_seesSpecialistTypes() {
    List<CasePost> posts = scopedView.forBilling(CASE_ID);
    List<PostType> types = posts.stream().map(CasePost::type).toList();

    assertThat(types)
        .containsExactly(
            PostType.PROBLEM,
            PostType.FACTS,
            PostType.PROPOSAL,
            PostType.CHALLENGE,
            PostType.EVIDENCE_REPLY,
            PostType.VERDICT);
  }

  @Test
  void risk_seesProposalWithoutReasoning() {
    List<CasePost> posts = scopedView.forRisk(CASE_ID);
    List<PostType> types = posts.stream().map(CasePost::type).toList();

    assertThat(types).contains(PostType.PROPOSAL);

    CasePost proposal = posts.stream().filter(p -> p.type() == PostType.PROPOSAL).findFirst().get();
    assertThat(proposal.contentJson()).doesNotContain("reasoning");
    assertThat(proposal.contentJson()).contains("amountPaise");
  }

  @Test
  void risk_seesMoodPost() {
    List<CasePost> posts = scopedView.forRisk(CASE_ID);
    List<PostType> types = posts.stream().map(CasePost::type).toList();

    assertThat(types).contains(PostType.MOOD);
  }

  @Test
  void communicator_seesOnlyDecisionAndOutcome() {
    List<CasePost> posts = scopedView.forCommunicator(CASE_ID);
    List<PostType> types = posts.stream().map(CasePost::type).toList();

    assertThat(types)
        .containsExactly(PostType.PROBLEM, PostType.FACTS, PostType.DECISION, PostType.SAGA_STEP);
    assertThat(types)
        .doesNotContain(PostType.PROPOSAL, PostType.CHALLENGE, PostType.VERDICT, PostType.MOOD);
  }

  @Test
  void communicator_seesDecisionWithoutInternalIds() {
    List<CasePost> posts = scopedView.forCommunicator(CASE_ID);

    CasePost decision = posts.stream().filter(p -> p.type() == PostType.DECISION).findFirst().get();
    assertThat(decision.contentJson()).doesNotContain("approvedProposalPostIds");
    assertThat(decision.contentJson()).contains("EXECUTE");
  }

  @Test
  void referee_seesEverything() {
    List<CasePost> posts = scopedView.forReferee(CASE_ID);
    assertThat(posts).hasSize(10);
  }

  @Test
  void removeJsonField_removesMiddleField() {
    String json = "{\"a\":1,\"reasoning\":\"some text\",\"b\":2}";
    String result = ScopedBoardView.removeJsonField(json, "reasoning");
    assertThat(result).doesNotContain("reasoning");
  }

  @Test
  void removeJsonField_removesArrayField() {
    String json = "{\"type\":\"EXECUTE\",\"approvedProposalPostIds\":[4,5],\"explanation\":\"ok\"}";
    String result = ScopedBoardView.removeJsonField(json, "approvedProposalPostIds");
    assertThat(result).doesNotContain("approvedProposalPostIds");
    assertThat(result).contains("EXECUTE");
  }

  private static CasePost post(PostAuthor author, PostType type, String contentJson) {
    return new CasePost(null, CASE_ID, author, type, contentJson, null, null, Instant.now());
  }
}
