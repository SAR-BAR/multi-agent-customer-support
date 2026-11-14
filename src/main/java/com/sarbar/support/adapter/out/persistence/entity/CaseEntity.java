package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Case;
import com.sarbar.support.domain.model.CaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cases")
public class CaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "conversation_id", nullable = false)
  private Long conversationId;

  @Column(name = "order_id", nullable = false, length = 36)
  private String orderId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CaseStatus status;

  @Column(name = "opened_at", nullable = false)
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  protected CaseEntity() {}

  public static CaseEntity fromDomain(Case c) {
    CaseEntity e = new CaseEntity();
    e.id = c.id();
    e.conversationId = c.conversationId();
    e.orderId = c.orderId();
    e.status = c.status();
    e.openedAt = c.openedAt();
    e.closedAt = c.closedAt();
    return e;
  }

  public Case toDomain() {
    return new Case(id, conversationId, orderId, status, openedAt, closedAt);
  }

  public Long getId() {
    return id;
  }
}
