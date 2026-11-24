package com.sarbar.support.domain.service.saga;

import com.sarbar.support.domain.model.Coupon;
import com.sarbar.support.domain.model.CreditNote;
import com.sarbar.support.domain.model.Refund;
import com.sarbar.support.domain.model.SagaType;
import com.sarbar.support.domain.port.out.CouponRepository;
import com.sarbar.support.domain.port.out.CouponService;
import com.sarbar.support.domain.port.out.CreditNoteRepository;
import com.sarbar.support.domain.port.out.Notifier;
import com.sarbar.support.domain.port.out.PaymentGateway;
import com.sarbar.support.domain.port.out.RefundRepository;
import java.time.Instant;
import java.util.List;

/** Builds the ordered steps for each saga type. Reversible steps first, the pivot last but one. */
public class SagaDefinition {

  private final PaymentGateway paymentGateway;
  private final CouponService couponService;
  private final Notifier notifier;
  private final CreditNoteRepository creditNoteRepository;
  private final CouponRepository couponRepository;
  private final RefundRepository refundRepository;

  public SagaDefinition(
      PaymentGateway paymentGateway,
      CouponService couponService,
      Notifier notifier,
      CreditNoteRepository creditNoteRepository,
      CouponRepository couponRepository,
      RefundRepository refundRepository) {
    this.paymentGateway = paymentGateway;
    this.couponService = couponService;
    this.notifier = notifier;
    this.creditNoteRepository = creditNoteRepository;
    this.couponRepository = couponRepository;
    this.refundRepository = refundRepository;
  }

  public List<SagaStep> stepsFor(SagaType type) {
    return switch (type) {
      case REFUND -> refundSteps();
      case COUPON_CREDIT -> couponCreditSteps();
    };
  }

  private List<SagaStep> refundSteps() {
    return List.of(
        SagaStep.compensable(
            "RESERVE_CREDIT_NOTE_NUMBER", this::reserveCreditNoteNumber, ctx -> {}),
        SagaStep.compensable("CREATE_CREDIT_NOTE", this::createCreditNote, this::voidCreditNote),
        SagaStep.pivot("REFUND", this::refund),
        SagaStep.retryOnly("NOTIFY", this::notifyRefund));
  }

  private List<SagaStep> couponCreditSteps() {
    return List.of(
        SagaStep.pivot("ISSUE_COUPON", this::issueCoupon),
        SagaStep.retryOnly("NOTIFY", this::notifyCoupon));
  }

  private void reserveCreditNoteNumber(SagaContext ctx) {
    creditNoteRepository
        .findByCreditNoteNumber(ctx.creditNoteNumber())
        .filter(cn -> !cn.orderId().equals(ctx.orderId()))
        .ifPresent(
            cn -> {
              throw new IllegalStateException(
                  "Credit note number "
                      + ctx.creditNoteNumber()
                      + " already used by another order");
            });
  }

  private void createCreditNote(SagaContext ctx) {
    if (creditNoteRepository.findByCreditNoteNumber(ctx.creditNoteNumber()).isPresent()) {
      return;
    }
    creditNoteRepository.save(
        new CreditNote(
            null, ctx.creditNoteNumber(), ctx.orderId(), ctx.amountPaise(), Instant.now()));
  }

  private void voidCreditNote(SagaContext ctx) {
    creditNoteRepository.deleteByCreditNoteNumber(ctx.creditNoteNumber());
  }

  private void refund(SagaContext ctx) {
    if (refundRepository.existsByIdempotencyKey(ctx.refundKey())) {
      return;
    }
    paymentGateway.refund(ctx.orderId(), ctx.amountPaise(), ctx.refundKey());
    refundRepository.save(
        new Refund(null, ctx.orderId(), ctx.amountPaise(), ctx.refundKey(), Instant.now()));
  }

  private void notifyRefund(SagaContext ctx) {
    notifier.notify(
        ctx.orderId(),
        "Your refund of " + ctx.amountPaise() + " paise has been processed.",
        ctx.notifyKey());
  }

  private void issueCoupon(SagaContext ctx) {
    if (couponRepository.findByCode(ctx.couponCode()).isPresent()) {
      return;
    }
    couponService.issueCoupon(ctx.orderId(), ctx.amountPaise(), ctx.couponCode());
    couponRepository.save(
        new Coupon(null, ctx.couponCode(), ctx.orderId(), ctx.amountPaise(), Instant.now(), null));
  }

  private void notifyCoupon(SagaContext ctx) {
    notifier.notify(
        ctx.orderId(),
        "A store-credit coupon of " + ctx.amountPaise() + " paise has been added to your account.",
        ctx.notifyKey());
  }
}
