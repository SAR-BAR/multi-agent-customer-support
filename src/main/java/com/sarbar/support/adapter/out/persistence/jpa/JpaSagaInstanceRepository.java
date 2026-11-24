package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.SagaInstanceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSagaInstanceRepository extends JpaRepository<SagaInstanceEntity, Long> {

  Optional<SagaInstanceEntity> findByCaseId(Long caseId);
}
