package com.sarbar.support.domain.port.out;

/** External system that issues store-credit coupons. The pivot of the COUPON_CREDIT saga. */
public interface CouponService {

  /** Issues a coupon worth {@code amountPaise}. Must be idempotent by {@code couponCode} */
  String issueCoupon(String orderId, int amountPaise, String couponCode);
}
