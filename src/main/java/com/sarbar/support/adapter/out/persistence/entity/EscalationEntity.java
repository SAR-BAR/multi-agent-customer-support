package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Escalation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "escalations")
public class EscalationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "case_id", nullable = false)
  private Long caseId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String reason;

  @Column(name = "escalated_at", nullable = false)
  private Instant escalatedAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  protected EscalationEntity() {}

  public static EscalationEntity fromDomain(Escalation esc) {
    EscalationEntity e = new EscalationEntity();
    e.id = esc.id();
    e.caseId = esc.caseId();
    e.reason = esc.reason();
    e.escalatedAt = esc.escalatedAt();
    e.resolvedAt = esc.resolvedAt();
    return e;
  }

  public Escalation toDomain() {
    return new Escalation(id, caseId, reason, escalatedAt, resolvedAt);
  }

  public Long getId() {
    return id;
  }
}
