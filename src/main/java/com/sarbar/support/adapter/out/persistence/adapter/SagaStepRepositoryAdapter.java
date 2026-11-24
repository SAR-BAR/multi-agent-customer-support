package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.SagaStepEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaSagaStepRepository;
import com.sarbar.support.domain.model.SagaStepRecord;
import com.sarbar.support.domain.port.out.SagaStepRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SagaStepRepositoryAdapter implements SagaStepRepository {

  private final JpaSagaStepRepository jpa;

  public SagaStepRepositoryAdapter(JpaSagaStepRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public SagaStepRecord save(SagaStepRecord step) {
    return jpa.save(SagaStepEntity.fromDomain(step)).toDomain();
  }

  @Override
  public List<SagaStepRecord> findBySagaId(Long sagaId) {
    return jpa.findBySagaId(sagaId).stream().map(SagaStepEntity::toDomain).toList();
  }

  @Override
  public Optional<SagaStepRecord> findBySagaIdAndStepName(Long sagaId, String stepName) {
    return jpa.findBySagaIdAndStepName(sagaId, stepName).map(SagaStepEntity::toDomain);
  }
}
