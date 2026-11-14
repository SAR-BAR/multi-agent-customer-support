package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Invoice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "invoices")
public class InvoiceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false, length = 36)
  private String orderId;

  @Column(name = "total_paise", nullable = false)
  private int totalPaise;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  protected InvoiceEntity() {}

  public static InvoiceEntity fromDomain(Invoice i) {
    InvoiceEntity e = new InvoiceEntity();
    e.id = i.id();
    e.orderId = i.orderId();
    e.totalPaise = i.totalPaise();
    e.issuedAt = i.issuedAt();
    return e;
  }

  public Invoice toDomain() {
    return new Invoice(id, orderId, totalPaise, issuedAt);
  }

  public Long getId() {
    return id;
  }
}
