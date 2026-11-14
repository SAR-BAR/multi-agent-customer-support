package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.ConversationEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaConversationRepository;
import com.sarbar.support.domain.model.Conversation;
import com.sarbar.support.domain.port.out.ConversationRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationRepositoryAdapter implements ConversationRepository {

  private final JpaConversationRepository jpa;

  public ConversationRepositoryAdapter(JpaConversationRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Conversation> findById(Long id) {
    return jpa.findById(id).map(ConversationEntity::toDomain);
  }

  @Override
  public Conversation save(Conversation conversation) {
    ConversationEntity saved = jpa.save(ConversationEntity.fromDomain(conversation));
    return saved.toDomain();
  }
}
