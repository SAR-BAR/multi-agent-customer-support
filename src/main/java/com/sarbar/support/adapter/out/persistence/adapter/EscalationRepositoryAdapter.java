package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.EscalationEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaEscalationRepository;
import com.sarbar.support.domain.model.Escalation;
import com.sarbar.support.domain.port.out.EscalationRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class EscalationRepositoryAdapter implements EscalationRepository {

  private final JpaEscalationRepository jpa;

  public EscalationRepositoryAdapter(JpaEscalationRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Escalation> findById(Long id) {
    return jpa.findById(id).map(EscalationEntity::toDomain);
  }

  @Override
  public Optional<Escalation> findByCaseId(Long caseId) {
    return jpa.findByCaseId(caseId).map(EscalationEntity::toDomain);
  }

  @Override
  public Escalation save(Escalation escalation) {
    EscalationEntity saved = jpa.save(EscalationEntity.fromDomain(escalation));
    return saved.toDomain();
  }
}
