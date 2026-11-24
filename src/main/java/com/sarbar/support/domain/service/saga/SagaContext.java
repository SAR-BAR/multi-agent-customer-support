package com.sarbar.support.domain.service.saga;

/**
 * Immutable inputs for one saga run. All idempotency keys derive from the case id so that
 * concurrent duplicate requests for the same case produce identical keys and collide on the unique
 * constraints, moving money at most once.
 */
public record SagaContext(long caseId, String orderId, int amountPaise) {

  public String creditNoteNumber() {
    return "CN-" + caseId;
  }

  public String couponCode() {
    return "CPN-" + caseId;
  }

  public String refundKey() {
    return "REFUND-case-" + caseId;
  }

  public String notifyKey() {
    return "NOTIFY-case-" + caseId;
  }
}
