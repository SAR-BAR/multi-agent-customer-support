package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Refund;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refunds")
public class RefundEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false, length = 36)
  private String orderId;

  @Column(name = "amount_paise", nullable = false)
  private int amountPaise;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
  private String idempotencyKey;

  @Column(name = "refunded_at", nullable = false)
  private Instant refundedAt;

  protected RefundEntity() {}

  public static RefundEntity fromDomain(Refund r) {
    RefundEntity e = new RefundEntity();
    e.id = r.id();
    e.orderId = r.orderId();
    e.amountPaise = r.amountPaise();
    e.idempotencyKey = r.idempotencyKey();
    e.refundedAt = r.refundedAt();
    return e;
  }

  public Refund toDomain() {
    return new Refund(id, orderId, amountPaise, idempotencyKey, refundedAt);
  }
}
