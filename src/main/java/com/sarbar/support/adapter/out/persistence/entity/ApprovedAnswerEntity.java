package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.ApprovedAnswer;
import com.sarbar.support.domain.model.GeneralType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approved_answers")
public class ApprovedAnswerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "general_type", nullable = false, length = 30)
  private GeneralType generalType;

  @Column(nullable = false)
  private String topic;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String answer;

  @Column(name = "policy_version", nullable = false)
  private int policyVersion;

  protected ApprovedAnswerEntity() {}

  public static ApprovedAnswerEntity fromDomain(ApprovedAnswer a) {
    ApprovedAnswerEntity e = new ApprovedAnswerEntity();
    e.id = a.id();
    e.generalType = a.generalType();
    e.topic = a.topic();
    e.answer = a.answer();
    e.policyVersion = a.policyVersion();
    return e;
  }

  public ApprovedAnswer toDomain() {
    return new ApprovedAnswer(id, generalType, topic, answer, policyVersion);
  }
}
