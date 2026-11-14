package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.ApprovedAnswerEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaApprovedAnswerRepository;
import com.sarbar.support.domain.model.ApprovedAnswer;
import com.sarbar.support.domain.model.GeneralType;
import com.sarbar.support.domain.port.out.ApprovedAnswerRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApprovedAnswerRepositoryAdapter implements ApprovedAnswerRepository {

  private final JpaApprovedAnswerRepository jpa;

  public ApprovedAnswerRepositoryAdapter(JpaApprovedAnswerRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<ApprovedAnswer> findByGeneralType(GeneralType generalType) {
    return jpa.findFirstByGeneralType(generalType).map(ApprovedAnswerEntity::toDomain);
  }

  @Override
  public Optional<ApprovedAnswer> findByGeneralTypeAndTopic(GeneralType generalType, String topic) {
    return jpa.findByGeneralTypeAndTopic(generalType, topic).map(ApprovedAnswerEntity::toDomain);
  }
}
