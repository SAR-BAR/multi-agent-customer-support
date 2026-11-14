package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.CasePost;
import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.PostType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Entity
@Table(name = "case_posts")
public class CasePostEntity {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "case_id", nullable = false)
  private Long caseId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PostAuthor author;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PostType type;

  @Column(name = "content_json", nullable = false, columnDefinition = "JSON")
  private String contentJson;

  @Column(name = "evidence_ids", columnDefinition = "JSON")
  private String evidenceIdsJson;

  @Column(name = "reply_to")
  private Long replyTo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected CasePostEntity() {}

  public static CasePostEntity fromDomain(CasePost cp) {
    CasePostEntity e = new CasePostEntity();
    e.id = cp.id();
    e.caseId = cp.caseId();
    e.author = cp.author();
    e.type = cp.type();
    e.contentJson = cp.contentJson();
    e.evidenceIdsJson = toJson(cp.evidenceIds());
    e.replyTo = cp.replyTo();
    e.createdAt = cp.createdAt();
    return e;
  }

  public CasePost toDomain() {
    return new CasePost(
        id, caseId, author, type, contentJson, parseEvidenceIds(), replyTo, createdAt);
  }

  public Long getId() {
    return id;
  }

  private List<String> parseEvidenceIds() {
    if (evidenceIdsJson == null || evidenceIdsJson.isBlank()) {
      return List.of();
    }
    try {
      return MAPPER.readValue(evidenceIdsJson, new TypeReference<List<String>>() {});
    } catch (JacksonException e) {
      return List.of();
    }
  }

  private static String toJson(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(ids);
    } catch (JacksonException e) {
      return null;
    }
  }
}
