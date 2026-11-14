package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConversationRepository extends JpaRepository<ConversationEntity, Long> {}
