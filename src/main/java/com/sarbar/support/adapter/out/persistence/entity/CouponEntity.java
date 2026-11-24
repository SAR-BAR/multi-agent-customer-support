package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Coupon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "coupons")
public class CouponEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "order_id", nullable = false, length = 36)
  private String orderId;

  @Column(name = "discount_paise", nullable = false)
  private int discountPaise;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  protected CouponEntity() {}

  public static CouponEntity fromDomain(Coupon c) {
    CouponEntity e = new CouponEntity();
    e.id = c.id();
    e.code = c.code();
    e.orderId = c.orderId();
    e.discountPaise = c.discountPaise();
    e.issuedAt = c.issuedAt();
    e.expiresAt = c.expiresAt();
    return e;
  }

  public Coupon toDomain() {
    return new Coupon(id, code, orderId, discountPaise, issuedAt, expiresAt);
  }
}
