package com.sarbar.support.domain.service.saga;

import java.util.function.Consumer;

public final class SagaStep {

  private final String name;
  private final boolean pivot;
  private final Consumer<SagaContext> execute;
  private final Consumer<SagaContext> compensate;

  private SagaStep(
      String name, boolean pivot, Consumer<SagaContext> execute, Consumer<SagaContext> compensate) {
    this.name = name;
    this.pivot = pivot;
    this.execute = execute;
    this.compensate = compensate;
  }

  public static SagaStep compensable(
      String name, Consumer<SagaContext> execute, Consumer<SagaContext> compensate) {
    return new SagaStep(name, false, execute, compensate);
  }

  public static SagaStep pivot(String name, Consumer<SagaContext> execute) {
    return new SagaStep(name, true, execute, null);
  }

  public static SagaStep retryOnly(String name, Consumer<SagaContext> execute) {
    return new SagaStep(name, false, execute, null);
  }

  public String name() {
    return name;
  }

  public boolean isPivot() {
    return pivot;
  }

  public void execute(SagaContext ctx) {
    execute.accept(ctx);
  }

  public boolean hasCompensation() {
    return compensate != null;
  }

  public void compensate(SagaContext ctx) {
    if (compensate != null) {
      compensate.accept(ctx);
    }
  }
}
