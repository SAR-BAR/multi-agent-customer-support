package com.sarbar.support.domain.service.saga;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarbar.support.adapter.out.external.FakeCouponService;
import com.sarbar.support.adapter.out.external.FakeNotifier;
import com.sarbar.support.adapter.out.external.FakePaymentGateway;
import com.sarbar.support.domain.model.Coupon;
import com.sarbar.support.domain.model.CreditNote;
import com.sarbar.support.domain.model.Refund;
import com.sarbar.support.domain.model.SagaInstance;
import com.sarbar.support.domain.model.SagaStatus;
import com.sarbar.support.domain.model.SagaStepRecord;
import com.sarbar.support.domain.model.SagaType;
import com.sarbar.support.domain.port.out.CouponRepository;
import com.sarbar.support.domain.port.out.CreditNoteRepository;
import com.sarbar.support.domain.port.out.RefundRepository;
import com.sarbar.support.domain.port.out.SagaInstanceRepository;
import com.sarbar.support.domain.port.out.SagaStepRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SagaOrchestratorTest {

  private static final long CASE_ID = 42L;
  private static final String ORDER_ID = "ORD-1";
  private static final int AMOUNT = 50000;

  private FakePaymentGateway gateway;
  private FakeCouponService couponService;
  private FakeNotifier notifier;
  private InMemoryCreditNotes creditNotes;
  private InMemoryCoupons coupons;
  private InMemoryRefunds refunds;
  private InMemorySagaInstances instances;
  private InMemorySagaSteps steps;
  private SagaOrchestrator orchestrator;

  @BeforeEach
  void setUp() {
    gateway = new FakePaymentGateway();
    couponService = new FakeCouponService();
    notifier = new FakeNotifier();
    creditNotes = new InMemoryCreditNotes();
    coupons = new InMemoryCoupons();
    refunds = new InMemoryRefunds();
    instances = new InMemorySagaInstances();
    steps = new InMemorySagaSteps();
    SagaDefinition definition =
        new SagaDefinition(gateway, couponService, notifier, creditNotes, coupons, refunds);
    orchestrator = new SagaOrchestrator(instances, steps, definition);
  }

  @Test
  void refundSagaCompletesAndMovesMoneyExactlyOnce() {
    SagaResult result = orchestrator.run(CASE_ID, SagaType.REFUND, ORDER_ID, AMOUNT);

    assertThat(result.status()).isEqualTo(SagaStatus.DONE);
    assertThat(refunds.findByOrderId(ORDER_ID)).hasSize(1);
    assertThat(gateway.debitCount()).isEqualTo(1);
    assertThat(creditNotes.findByCreditNoteNumber("CN-" + CASE_ID)).isPresent();
  }

  @Test
  void couponCreditSagaCompletes() {
    SagaResult result = orchestrator.run(CASE_ID, SagaType.COUPON_CREDIT, ORDER_ID, AMOUNT);

    assertThat(result.status()).isEqualTo(SagaStatus.DONE);
    assertThat(coupons.findByCode("CPN-" + CASE_ID)).isPresent();
    assertThat(refunds.findByOrderId(ORDER_ID)).isEmpty();
  }

  @Test
  void compensatesWhenAReversibleStepFailsBeforePivot() {
    creditNotes.failNextSave();

    SagaResult result = orchestrator.run(CASE_ID, SagaType.REFUND, ORDER_ID, AMOUNT);

    assertThat(result.status()).isEqualTo(SagaStatus.COMPENSATED);
    assertThat(refunds.findByOrderId(ORDER_ID)).isEmpty();
    assertThat(gateway.debitCount()).isZero();
    assertThat(creditNotes.findByCreditNoteNumber("CN-" + CASE_ID)).isEmpty();
  }

  @Test
  void compensatesCreditNoteWhenPivotFails() {
    gateway.failOn("REFUND-case-" + CASE_ID);

    SagaResult result = orchestrator.run(CASE_ID, SagaType.REFUND, ORDER_ID, AMOUNT);

    assertThat(result.status()).isEqualTo(SagaStatus.COMPENSATED);
    assertThat(refunds.findByOrderId(ORDER_ID)).isEmpty();
    assertThat(creditNotes.findByCreditNoteNumber("CN-" + CASE_ID)).isEmpty();
  }

  @Test
  void failsWithoutCompensationWhenStepAfterPivotFails() {
    notifier.failOn("NOTIFY-case-" + CASE_ID);

    SagaResult result = orchestrator.run(CASE_ID, SagaType.REFUND, ORDER_ID, AMOUNT);

    assertThat(result.status()).isEqualTo(SagaStatus.FAILED);
    assertThat(refunds.findByOrderId(ORDER_ID)).hasSize(1);
    assertThat(creditNotes.findByCreditNoteNumber("CN-" + CASE_ID)).isPresent();
  }

  @Test
  void resumeAfterCrashDoesNotRefundTwice() {
    notifier.failOn("NOTIFY-case-" + CASE_ID);
    SagaResult first = orchestrator.run(CASE_ID, SagaType.REFUND, ORDER_ID, AMOUNT);
    assertThat(first.status()).isEqualTo(SagaStatus.FAILED);
    assertThat(refunds.findByOrderId(ORDER_ID)).hasSize(1);

    notifier.clearFailures();
    SagaResult second = orchestrator.run(CASE_ID, SagaType.REFUND, ORDER_ID, AMOUNT);

    assertThat(second.status()).isEqualTo(SagaStatus.DONE);
    assertThat(refunds.findByOrderId(ORDER_ID)).hasSize(1);
    assertThat(gateway.debitCount()).isEqualTo(1);
  }

  @Test
  void concurrentIdenticalRequestsProduceExactlyOneRefund() throws InterruptedException {
    int threads = 16;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threads);
    for (int i = 0; i < threads; i++) {
      pool.submit(
          () -> {
            try {
              start.await();
              orchestrator.run(CASE_ID, SagaType.REFUND, ORDER_ID, AMOUNT);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              done.countDown();
            }
          });
    }
    start.countDown();
    done.await();
    pool.shutdown();

    assertThat(refunds.findByOrderId(ORDER_ID)).hasSize(1);
    assertThat(gateway.debitCount()).isEqualTo(1);
  }

  // ---- in-memory test doubles ----

  private static final class InMemoryCreditNotes implements CreditNoteRepository {
    private final Map<String, CreditNote> byNumber = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    private volatile boolean failNextSave;

    void failNextSave() {
      this.failNextSave = true;
    }

    @Override
    public CreditNote save(CreditNote cn) {
      if (failNextSave) {
        failNextSave = false;
        throw new IllegalStateException("credit note save failed");
      }
      CreditNote stored =
          new CreditNote(
              ids.incrementAndGet(),
              cn.creditNoteNumber(),
              cn.orderId(),
              cn.amountPaise(),
              cn.issuedAt());
      byNumber.put(cn.creditNoteNumber(), stored);
      return stored;
    }

    @Override
    public Optional<CreditNote> findByCreditNoteNumber(String number) {
      return Optional.ofNullable(byNumber.get(number));
    }

    @Override
    public void deleteByCreditNoteNumber(String number) {
      byNumber.remove(number);
    }
  }

  private static final class InMemoryCoupons implements CouponRepository {
    private final Map<String, Coupon> byCode = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    @Override
    public Coupon save(Coupon c) {
      Coupon stored =
          new Coupon(
              ids.incrementAndGet(),
              c.code(),
              c.orderId(),
              c.discountPaise(),
              c.issuedAt(),
              c.expiresAt());
      byCode.put(c.code(), stored);
      return stored;
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
      return Optional.ofNullable(byCode.get(code));
    }
  }

  private static final class InMemoryRefunds implements RefundRepository {
    private final List<Refund> rows = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();

    @Override
    public synchronized Refund save(Refund refund) {
      boolean exists =
          rows.stream().anyMatch(r -> r.idempotencyKey().equals(refund.idempotencyKey()));
      if (exists) {
        throw new IllegalStateException("duplicate idempotency key " + refund.idempotencyKey());
      }
      Refund stored =
          new Refund(
              ids.incrementAndGet(),
              refund.orderId(),
              refund.amountPaise(),
              refund.idempotencyKey(),
              refund.refundedAt());
      rows.add(stored);
      return stored;
    }

    @Override
    public synchronized List<Refund> findByOrderId(String orderId) {
      return rows.stream().filter(r -> r.orderId().equals(orderId)).toList();
    }

    @Override
    public synchronized boolean existsByIdempotencyKey(String key) {
      return rows.stream().anyMatch(r -> r.idempotencyKey().equals(key));
    }
  }

  private static final class InMemorySagaInstances implements SagaInstanceRepository {
    private final Map<Long, SagaInstance> byId = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    @Override
    public synchronized SagaInstance save(SagaInstance instance) {
      Long id = instance.id() != null ? instance.id() : ids.incrementAndGet();
      SagaInstance stored =
          new SagaInstance(
              id,
              instance.caseId(),
              instance.sagaType(),
              instance.status(),
              instance.startedAt(),
              instance.endedAt());
      byId.put(id, stored);
      return stored;
    }

    @Override
    public Optional<SagaInstance> findById(Long id) {
      return Optional.ofNullable(byId.get(id));
    }

    @Override
    public synchronized Optional<SagaInstance> findByCaseId(Long caseId) {
      return byId.values().stream().filter(i -> i.caseId().equals(caseId)).findFirst();
    }
  }

  private static final class InMemorySagaSteps implements SagaStepRepository {
    private final Map<String, SagaStepRecord> byKey = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    private static String key(Long sagaId, String stepName) {
      return sagaId + ":" + stepName;
    }

    @Override
    public synchronized SagaStepRecord save(SagaStepRecord step) {
      Long id = step.id() != null ? step.id() : ids.incrementAndGet();
      SagaStepRecord stored =
          new SagaStepRecord(
              id,
              step.sagaId(),
              step.stepName(),
              step.status(),
              step.idempotencyKey(),
              step.startedAt(),
              step.completedAt());
      byKey.put(key(step.sagaId(), step.stepName()), stored);
      return stored;
    }

    @Override
    public List<SagaStepRecord> findBySagaId(Long sagaId) {
      return byKey.values().stream().filter(s -> s.sagaId().equals(sagaId)).toList();
    }

    @Override
    public Optional<SagaStepRecord> findBySagaIdAndStepName(Long sagaId, String stepName) {
      return Optional.ofNullable(byKey.get(key(sagaId, stepName)));
    }
  }
}
