package com.sarbar.support.domain.port.out;

/** External system that tells the customer the outcome. */
public interface Notifier {

  void notify(String orderId, String message, String idempotencyKey);
}
