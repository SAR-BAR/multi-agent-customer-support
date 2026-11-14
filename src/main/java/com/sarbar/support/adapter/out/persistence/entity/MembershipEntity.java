package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Membership;
import com.sarbar.support.domain.model.MembershipStatus;
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
@Table(name = "memberships")
public class MembershipEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MembershipStatus status;

  @Column(name = "free_delivery", nullable = false)
  private boolean freeDelivery;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  protected MembershipEntity() {}

  public static MembershipEntity fromDomain(Membership m) {
    MembershipEntity e = new MembershipEntity();
    e.id = m.id();
    e.customerId = m.customerId();
    e.status = m.status();
    e.freeDelivery = m.freeDelivery();
    e.startedAt = m.startedAt();
    e.expiresAt = m.expiresAt();
    return e;
  }

  public Membership toDomain() {
    return new Membership(id, customerId, status, freeDelivery, startedAt, expiresAt);
  }
}
