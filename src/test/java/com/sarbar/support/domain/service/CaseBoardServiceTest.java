package com.sarbar.support.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import com.sarbar.support.domain.port.out.CasePostRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseBoardServiceTest {

  private InMemoryCasePostRepository repository;
  private CaseBoardService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryCasePostRepository();
    service = new CaseBoardService(repository);
  }

  @Test
  void addPost_withNoEvidence_succeeds() {
    CasePost post = makePost(1L, PostAuthor.SYSTEM, PostType.FACTS, null, null);
    CasePost saved = service.addPost(post);
    assertThat(saved.id()).isNotNull();
  }

  @Test
  void addPost_withValidEvidence_succeeds() {
    CasePost facts = service.addPost(makePost(1L, PostAuthor.SYSTEM, PostType.FACTS, null, null));

    CasePost proposal =
        makePost(
            1L, PostAuthor.RESOLUTION, PostType.PROPOSAL, List.of(facts.id().toString()), null);
    CasePost saved = service.addPost(proposal);
    assertThat(saved.id()).isNotNull();
  }

  @Test
  void addPost_withNonExistentEvidence_throws() {
    CasePost post = makePost(1L, PostAuthor.RESOLUTION, PostType.PROPOSAL, List.of("999"), null);
    assertThatThrownBy(() -> service.addPost(post))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Evidence post not found: 999");
  }

  @Test
  void addPost_withEvidenceFromDifferentCase_throws() {
    CasePost otherCaseFacts =
        service.addPost(makePost(2L, PostAuthor.SYSTEM, PostType.FACTS, null, null));

    CasePost post =
        makePost(
            1L,
            PostAuthor.RESOLUTION,
            PostType.PROPOSAL,
            List.of(otherCaseFacts.id().toString()),
            null);
    assertThatThrownBy(() -> service.addPost(post))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("belongs to a different case");
  }

  @Test
  void addPost_citingUncitableType_throws() {
    CasePost mood = service.addPost(makePost(1L, PostAuthor.TRIAGE, PostType.MOOD, null, null));

    CasePost post =
        makePost(1L, PostAuthor.RESOLUTION, PostType.PROPOSAL, List.of(mood.id().toString()), null);
    assertThatThrownBy(() -> service.addPost(post))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be cited as evidence");
  }

  @Test
  void addPost_withValidReplyTo_succeeds() {
    CasePost challenge =
        service.addPost(makePost(1L, PostAuthor.RISK, PostType.CHALLENGE, null, null));

    CasePost reply =
        makePost(1L, PostAuthor.RESOLUTION, PostType.EVIDENCE_REPLY, null, challenge.id());
    CasePost saved = service.addPost(reply);
    assertThat(saved.id()).isNotNull();
  }

  @Test
  void addPost_withReplyToFromDifferentCase_throws() {
    CasePost otherPost =
        service.addPost(makePost(2L, PostAuthor.RISK, PostType.CHALLENGE, null, null));

    CasePost reply =
        makePost(1L, PostAuthor.RESOLUTION, PostType.EVIDENCE_REPLY, null, otherPost.id());
    assertThatThrownBy(() -> service.addPost(reply))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("belongs to a different case");
  }

  @Test
  void addPost_withInvalidEvidenceId_throws() {
    CasePost post =
        makePost(1L, PostAuthor.RESOLUTION, PostType.PROPOSAL, List.of("not-a-number"), null);
    assertThatThrownBy(() -> service.addPost(post))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not a valid post ID");
  }

  @Test
  void allPosts_returnsPostsInOrder() {
    service.addPost(makePost(1L, PostAuthor.SYSTEM, PostType.PROBLEM, null, null));
    service.addPost(makePost(1L, PostAuthor.SYSTEM, PostType.FACTS, null, null));
    service.addPost(makePost(1L, PostAuthor.RESOLUTION, PostType.PROPOSAL, null, null));

    List<CasePost> posts = service.allPosts(1L);
    assertThat(posts).hasSize(3);
    assertThat(posts.get(0).type()).isEqualTo(PostType.PROBLEM);
    assertThat(posts.get(1).type()).isEqualTo(PostType.FACTS);
    assertThat(posts.get(2).type()).isEqualTo(PostType.PROPOSAL);
  }

  private static CasePost makePost(
      Long caseId, PostAuthor author, PostType type, List<String> evidenceIds, Long replyTo) {
    return new CasePost(null, caseId, author, type, "{}", evidenceIds, replyTo, Instant.now());
  }

  static class InMemoryCasePostRepository implements CasePostRepository {

    private final List<CasePost> posts = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<CasePost> findByCaseId(Long caseId) {
      return posts.stream().filter(p -> p.caseId().equals(caseId)).toList();
    }

    @Override
    public List<CasePost> findByCaseIdAndType(Long caseId, PostType type) {
      return posts.stream().filter(p -> p.caseId().equals(caseId) && p.type() == type).toList();
    }

    @Override
    public Optional<CasePost> findById(Long id) {
      return posts.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    @Override
    public CasePost append(CasePost post) {
      CasePost saved =
          new CasePost(
              idGenerator.getAndIncrement(),
              post.caseId(),
              post.author(),
              post.type(),
              post.contentJson(),
              post.evidenceIds(),
              post.replyTo(),
              post.createdAt());
      posts.add(saved);
      return saved;
    }
  }
}
