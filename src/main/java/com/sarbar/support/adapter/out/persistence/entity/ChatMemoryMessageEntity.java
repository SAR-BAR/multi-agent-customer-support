package com.sarbar.support.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_memory")
public class ChatMemoryMessageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "conversation_id", nullable = false)
  private Long conversationId;

  @Column(nullable = false, length = 20)
  private String role;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ChatMemoryMessageEntity() {}

  public ChatMemoryMessageEntity(Long conversationId, String role, String content) {
    this.conversationId = conversationId;
    this.role = role;
    this.content = content;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Long getConversationId() {
    return conversationId;
  }

  public String getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
