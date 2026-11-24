package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.SagaStepEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSagaStepRepository extends JpaRepository<SagaStepEntity, Long> {

  List<SagaStepEntity> findBySagaId(Long sagaId);

  Optional<SagaStepEntity> findBySagaIdAndStepName(Long sagaId, String stepName);
}
