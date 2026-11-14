package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.InvoiceLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice_lines")
public class InvoiceLineEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "invoice_id", nullable = false)
  private Long invoiceId;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price_paise", nullable = false)
  private int unitPricePaise;

  @Column(name = "line_total_paise", nullable = false)
  private int lineTotalPaise;

  protected InvoiceLineEntity() {}

  public static InvoiceLineEntity fromDomain(InvoiceLine il) {
    InvoiceLineEntity e = new InvoiceLineEntity();
    e.id = il.id();
    e.invoiceId = il.invoiceId();
    e.description = il.description();
    e.quantity = il.quantity();
    e.unitPricePaise = il.unitPricePaise();
    e.lineTotalPaise = il.lineTotalPaise();
    return e;
  }

  public InvoiceLine toDomain() {
    return new InvoiceLine(id, invoiceId, description, quantity, unitPricePaise, lineTotalPaise);
  }
}
