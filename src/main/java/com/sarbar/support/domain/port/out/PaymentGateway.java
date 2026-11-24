package com.sarbar.support.domain.port.out;

/** External system that moves refund money. The irreversible pivot of the REFUND saga. */
public interface PaymentGateway {

  /** Refunds money for an order. Must be idempotent by {@code idempotencyKey}: */
  String refund(String orderId, int amountPaise, String idempotencyKey);
}
