package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.CreditNote;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "credit_notes")
public class CreditNoteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "credit_note_number", nullable = false, unique = true, length = 50)
  private String creditNoteNumber;

  @Column(name = "order_id", nullable = false, length = 36)
  private String orderId;

  @Column(name = "amount_paise", nullable = false)
  private int amountPaise;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  protected CreditNoteEntity() {}

  public static CreditNoteEntity fromDomain(CreditNote cn) {
    CreditNoteEntity e = new CreditNoteEntity();
    e.id = cn.id();
    e.creditNoteNumber = cn.creditNoteNumber();
    e.orderId = cn.orderId();
    e.amountPaise = cn.amountPaise();
    e.issuedAt = cn.issuedAt();
    return e;
  }

  public CreditNote toDomain() {
    return new CreditNote(id, creditNoteNumber, orderId, amountPaise, issuedAt);
  }
}
