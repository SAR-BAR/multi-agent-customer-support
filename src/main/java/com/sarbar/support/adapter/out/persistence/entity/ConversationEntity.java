package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Conversation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  protected ConversationEntity() {}

  public static ConversationEntity fromDomain(Conversation c) {
    ConversationEntity e = new ConversationEntity();
    e.id = c.id();
    e.customerId = c.customerId();
    e.startedAt = c.startedAt();
    return e;
  }

  public Conversation toDomain() {
    return new Conversation(id, customerId, startedAt);
  }

  public Long getId() {
    return id;
  }
}
