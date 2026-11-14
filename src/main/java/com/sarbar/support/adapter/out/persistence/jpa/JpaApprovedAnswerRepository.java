package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.ApprovedAnswerEntity;
import com.sarbar.support.domain.model.GeneralType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaApprovedAnswerRepository extends JpaRepository<ApprovedAnswerEntity, Long> {

  Optional<ApprovedAnswerEntity> findFirstByGeneralType(GeneralType generalType);

  Optional<ApprovedAnswerEntity> findByGeneralTypeAndTopic(GeneralType generalType, String topic);
}
