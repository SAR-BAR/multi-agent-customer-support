package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.SagaStepRecord;
import com.sarbar.support.domain.model.StepStatus;
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
@Table(name = "saga_steps")
public class SagaStepEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "saga_id", nullable = false)
  private Long sagaId;

  @Column(name = "step_name", nullable = false, length = 50)
  private String stepName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StepStatus status;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
  private String idempotencyKey;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected SagaStepEntity() {}

  public static SagaStepEntity fromDomain(SagaStepRecord s) {
    SagaStepEntity e = new SagaStepEntity();
    e.id = s.id();
    e.sagaId = s.sagaId();
    e.stepName = s.stepName();
    e.status = s.status();
    e.idempotencyKey = s.idempotencyKey();
    e.startedAt = s.startedAt();
    e.completedAt = s.completedAt();
    return e;
  }

  public SagaStepRecord toDomain() {
    return new SagaStepRecord(id, sagaId, stepName, status, idempotencyKey, startedAt, completedAt);
  }
}
