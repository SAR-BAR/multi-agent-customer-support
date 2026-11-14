package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Payment;
import com.sarbar.support.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false, length = 36)
  private String orderId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PaymentStatus status;

  @Column(name = "amount_paise", nullable = false)
  private int amountPaise;

  @Column(nullable = false, length = 50)
  private String method;

  @Column(name = "transaction_ref", length = 100)
  private String transactionRef;

  @Column(name = "paid_at")
  private Instant paidAt;

  protected PaymentEntity() {}

  public static PaymentEntity fromDomain(Payment p) {
    PaymentEntity e = new PaymentEntity();
    e.id = p.id();
    e.orderId = p.orderId();
    e.status = p.status();
    e.amountPaise = p.amountPaise();
    e.method = p.method();
    e.transactionRef = p.transactionRef();
    e.paidAt = p.paidAt();
    return e;
  }

  public Payment toDomain() {
    return new Payment(id, orderId, status, amountPaise, method, transactionRef, paidAt);
  }
}
