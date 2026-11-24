package com.sarbar.support.domain.service.saga;

import com.sarbar.support.domain.model.SagaInstance;
import com.sarbar.support.domain.model.SagaStatus;
import com.sarbar.support.domain.model.SagaStepRecord;
import com.sarbar.support.domain.model.SagaType;
import com.sarbar.support.domain.model.StepStatus;
import com.sarbar.support.domain.port.out.SagaInstanceRepository;
import com.sarbar.support.domain.port.out.SagaStepRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SagaOrchestrator {

  private final SagaInstanceRepository instanceRepository;
  private final SagaStepRepository stepRepository;
  private final SagaDefinition definition;
  private final ConcurrentHashMap<Long, Lock> caseLocks = new ConcurrentHashMap<>();

  public SagaOrchestrator(
      SagaInstanceRepository instanceRepository,
      SagaStepRepository stepRepository,
      SagaDefinition definition) {
    this.instanceRepository = instanceRepository;
    this.stepRepository = stepRepository;
    this.definition = definition;
  }

  public SagaResult run(long caseId, SagaType type, String orderId, int amountPaise) {
    Lock lock = caseLocks.computeIfAbsent(caseId, k -> new ReentrantLock());
    lock.lock();
    try {
      return runLocked(caseId, type, orderId, amountPaise);
    } finally {
      lock.unlock();
    }
  }

  private SagaResult runLocked(long caseId, SagaType type, String orderId, int amountPaise) {
    SagaInstance instance =
        instanceRepository
            .findByCaseId(caseId)
            .orElseGet(
                () ->
                    instanceRepository.save(
                        new SagaInstance(
                            null, caseId, type, SagaStatus.PENDING, Instant.now(), null)));

    if (instance.status() == SagaStatus.DONE) {
      return new SagaResult(instance.id(), type, SagaStatus.DONE, "Already completed");
    }
    if (instance.status() == SagaStatus.COMPENSATED) {
      return new SagaResult(instance.id(), type, SagaStatus.COMPENSATED, "Already compensated");
    }

    SagaContext ctx = new SagaContext(caseId, orderId, amountPaise);
    List<SagaStep> steps = definition.stepsFor(type);
    List<SagaStep> completed = new ArrayList<>();
    boolean pivotPassed = false;

    for (SagaStep step : steps) {
      if (isAlreadyDone(instance.id(), step.name())) {
        completed.add(step);
        pivotPassed = pivotPassed || step.isPivot();
        continue;
      }
      try {
        markStep(instance.id(), step.name(), StepStatus.PENDING, ctx);
        step.execute(ctx);
        markStep(instance.id(), step.name(), StepStatus.DONE, ctx);
        completed.add(step);
        pivotPassed = pivotPassed || step.isPivot();
      } catch (RuntimeException e) {
        markStep(instance.id(), step.name(), StepStatus.FAILED, ctx);
        if (pivotPassed) {
          return finish(instance, SagaStatus.FAILED, type, "Failed after pivot at " + step.name());
        }
        compensate(instance.id(), completed, ctx);
        return finish(
            instance, SagaStatus.COMPENSATED, type, "Compensated after failure at " + step.name());
      }
    }
    return finish(instance, SagaStatus.DONE, type, "Completed");
  }

  private boolean isAlreadyDone(Long sagaId, String stepName) {
    return stepRepository
        .findBySagaIdAndStepName(sagaId, stepName)
        .map(s -> s.status() == StepStatus.DONE)
        .orElse(false);
  }

  private void compensate(Long sagaId, List<SagaStep> completed, SagaContext ctx) {
    instanceRepository
        .findById(sagaId)
        .ifPresent(i -> instanceRepository.save(i.withStatus(SagaStatus.COMPENSATING, null)));
    for (int i = completed.size() - 1; i >= 0; i--) {
      SagaStep step = completed.get(i);
      if (step.hasCompensation()) {
        step.compensate(ctx);
        markStep(sagaId, step.name(), StepStatus.COMPENSATED, ctx);
      }
    }
  }

  private void markStep(Long sagaId, String stepName, StepStatus status, SagaContext ctx) {
    Optional<SagaStepRecord> existing = stepRepository.findBySagaIdAndStepName(sagaId, stepName);
    Instant now = Instant.now();
    SagaStepRecord record =
        existing
            .map(
                s ->
                    new SagaStepRecord(
                        s.id(),
                        sagaId,
                        stepName,
                        status,
                        s.idempotencyKey(),
                        s.startedAt() != null ? s.startedAt() : now,
                        status == StepStatus.DONE ? now : s.completedAt()))
            .orElseGet(
                () ->
                    new SagaStepRecord(
                        null, sagaId, stepName, status, sagaId + ":" + stepName, now, null));
    stepRepository.save(record);
  }

  private SagaResult finish(
      SagaInstance instance, SagaStatus status, SagaType type, String detail) {
    SagaInstance updated = instance.withStatus(status, Instant.now());
    instanceRepository.save(updated);
    return new SagaResult(instance.id(), type, status, detail);
  }
}
