package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.ChatMemoryMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaChatMemoryRepository extends JpaRepository<ChatMemoryMessageEntity, Long> {

  List<ChatMemoryMessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

  void deleteByConversationId(Long conversationId);
}
