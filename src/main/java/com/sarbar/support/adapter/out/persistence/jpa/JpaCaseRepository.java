package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.CaseEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCaseRepository extends JpaRepository<CaseEntity, Long> {

  Optional<CaseEntity> findByConversationIdAndOrderId(Long conversationId, String orderId);

  List<CaseEntity> findByConversationId(Long conversationId);
}
