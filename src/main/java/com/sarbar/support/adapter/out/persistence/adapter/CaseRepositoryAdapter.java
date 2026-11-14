package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.CaseEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaCaseRepository;
import com.sarbar.support.domain.model.Case;
import com.sarbar.support.domain.port.out.CaseRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CaseRepositoryAdapter implements CaseRepository {

  private final JpaCaseRepository jpa;

  public CaseRepositoryAdapter(JpaCaseRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Case> findById(Long id) {
    return jpa.findById(id).map(CaseEntity::toDomain);
  }

  @Override
  public Optional<Case> findByConversationIdAndOrderId(Long conversationId, String orderId) {
    return jpa.findByConversationIdAndOrderId(conversationId, orderId).map(CaseEntity::toDomain);
  }

  @Override
  public List<Case> findByConversationId(Long conversationId) {
    return jpa.findByConversationId(conversationId).stream().map(CaseEntity::toDomain).toList();
  }

  @Override
  public Case save(Case supportCase) {
    CaseEntity saved = jpa.save(CaseEntity.fromDomain(supportCase));
    return saved.toDomain();
  }
}
