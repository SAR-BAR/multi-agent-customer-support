package com.sarbar.support.adapter.out.external;

import com.sarbar.support.domain.port.out.Notifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** In-memory notifier for local runs and tests. */
@Component
public class FakeNotifier implements Notifier {

  private static final Logger log = LoggerFactory.getLogger(FakeNotifier.class);

  private final Set<String> sent = ConcurrentHashMap.newKeySet();
  private final Set<String> failKeys = ConcurrentHashMap.newKeySet();

  @Override
  public void notify(String orderId, String message, String idempotencyKey) {
    if (failKeys.contains(idempotencyKey)) {
      throw new IllegalStateException("Notifier failed for " + idempotencyKey);
    }
    if (sent.add(idempotencyKey)) {
      log.info("Notified customer for order {}: {}", orderId, message);
    }
  }

  public void failOn(String idempotencyKey) {
    failKeys.add(idempotencyKey);
  }

  public void clearFailures() {
    failKeys.clear();
  }
}
