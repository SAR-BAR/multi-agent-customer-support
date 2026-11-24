package com.sarbar.support.adapter.out.external;

import com.sarbar.support.domain.port.out.CouponService;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** In-memory coupon service for local runs and tests. Idempotent by code; can be told to fail. */
@Component
public class FakeCouponService implements CouponService {

  private static final Logger log = LoggerFactory.getLogger(FakeCouponService.class);

  private final Set<String> issued = ConcurrentHashMap.newKeySet();
  private final Set<String> failCodes = ConcurrentHashMap.newKeySet();

  @Override
  public String issueCoupon(String orderId, int amountPaise, String couponCode) {
    if (failCodes.contains(couponCode)) {
      throw new IllegalStateException("Coupon service refused coupon " + couponCode);
    }
    if (issued.add(couponCode)) {
      log.info("Issued coupon {} worth {} paise for order {}", couponCode, amountPaise, orderId);
    }
    return couponCode;
  }

  public void failOn(String couponCode) {
    failCodes.add(couponCode);
  }

  public void clearFailures() {
    failCodes.clear();
  }
}
