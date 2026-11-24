package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.SagaInstance;
import com.sarbar.support.domain.model.SagaStatus;
import com.sarbar.support.domain.model.SagaType;
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
@Table(name = "saga_instances")
public class SagaInstanceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "case_id", nullable = false)
  private Long caseId;

  @Enumerated(EnumType.STRING)
  @Column(name = "saga_type", nullable = false, length = 50)
  private SagaType sagaType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SagaStatus status;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  protected SagaInstanceEntity() {}

  public static SagaInstanceEntity fromDomain(SagaInstance s) {
    SagaInstanceEntity e = new SagaInstanceEntity();
    e.id = s.id();
    e.caseId = s.caseId();
    e.sagaType = s.sagaType();
    e.status = s.status();
    e.startedAt = s.startedAt();
    e.endedAt = s.endedAt();
    return e;
  }

  public SagaInstance toDomain() {
    return new SagaInstance(id, caseId, sagaType, status, startedAt, endedAt);
  }
}
