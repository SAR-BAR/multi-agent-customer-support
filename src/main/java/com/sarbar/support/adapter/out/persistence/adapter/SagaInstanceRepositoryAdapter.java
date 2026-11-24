package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.SagaInstanceEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaSagaInstanceRepository;
import com.sarbar.support.domain.model.SagaInstance;
import com.sarbar.support.domain.port.out.SagaInstanceRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SagaInstanceRepositoryAdapter implements SagaInstanceRepository {

  private final JpaSagaInstanceRepository jpa;

  public SagaInstanceRepositoryAdapter(JpaSagaInstanceRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public SagaInstance save(SagaInstance instance) {
    return jpa.save(SagaInstanceEntity.fromDomain(instance)).toDomain();
  }

  @Override
  public Optional<SagaInstance> findById(Long id) {
    return jpa.findById(id).map(SagaInstanceEntity::toDomain);
  }

  @Override
  public Optional<SagaInstance> findByCaseId(Long caseId) {
    return jpa.findByCaseId(caseId).map(SagaInstanceEntity::toDomain);
  }
}
