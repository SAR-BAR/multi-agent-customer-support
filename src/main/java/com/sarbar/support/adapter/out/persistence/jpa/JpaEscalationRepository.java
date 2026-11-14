package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.EscalationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEscalationRepository extends JpaRepository<EscalationEntity, Long> {

  Optional<EscalationEntity> findByCaseId(Long caseId);
}
