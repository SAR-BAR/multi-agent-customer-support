package com.sarbar.support.adapter.out.external;

import com.sarbar.support.domain.port.out.PaymentGateway;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** In-memory payment gateway for local runs and tests. Idempotent by key; can be told to fail. */
@Component
public class FakePaymentGateway implements PaymentGateway {

  private static final Logger log = LoggerFactory.getLogger(FakePaymentGateway.class);

  private final ConcurrentHashMap<String, String> appliedByKey = new ConcurrentHashMap<>();
  private final AtomicInteger debitCount = new AtomicInteger();
  private final Set<String> failKeys = ConcurrentHashMap.newKeySet();

  @Override
  public String refund(String orderId, int amountPaise, String idempotencyKey) {
    if (failKeys.contains(idempotencyKey)) {
      throw new IllegalStateException("Payment gateway refused refund for " + idempotencyKey);
    }
    String ref =
        appliedByKey.computeIfAbsent(
            idempotencyKey,
            k -> {
              debitCount.incrementAndGet();
              String tx = "TX-" + k;
              log.info("Refunded {} paise for order {} (tx {})", amountPaise, orderId, tx);
              return tx;
            });
    return ref;
  }

  public void failOn(String idempotencyKey) {
    failKeys.add(idempotencyKey);
  }

  public void clearFailures() {
    failKeys.clear();
  }

  public int debitCount() {
    return debitCount.get();
  }
}
