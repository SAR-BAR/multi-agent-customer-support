package com.sarbar.support.domain.model;

import java.time.Instant;

public record SagaInstance(
    Long id,
    Long caseId,
    SagaType sagaType,
    SagaStatus status,
    Instant startedAt,
    Instant endedAt) {

  public SagaInstance withStatus(SagaStatus newStatus, Instant endedAt) {
    return new SagaInstance(id, caseId, sagaType, newStatus, startedAt, endedAt);
  }
}
