package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.RefundPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refund_policies")
public class RefundPolicyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private int version;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "effective_from", nullable = false)
  private Instant effectiveFrom;

  protected RefundPolicyEntity() {}

  public static RefundPolicyEntity fromDomain(RefundPolicy rp) {
    RefundPolicyEntity e = new RefundPolicyEntity();
    e.id = rp.id();
    e.version = rp.version();
    e.content = rp.content();
    e.effectiveFrom = rp.effectiveFrom();
    return e;
  }

  public RefundPolicy toDomain() {
    return new RefundPolicy(id, version, content, effectiveFrom);
  }
}
