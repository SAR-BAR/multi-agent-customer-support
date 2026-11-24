package com.sarbar.support.domain.model;

import java.time.Instant;

public record SagaStepRecord(
    Long id,
    Long sagaId,
    String stepName,
    StepStatus status,
    String idempotencyKey,
    Instant startedAt,
    Instant completedAt) {}
