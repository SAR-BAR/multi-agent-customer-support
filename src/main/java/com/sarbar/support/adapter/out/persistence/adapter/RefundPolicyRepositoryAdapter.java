package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.RefundPolicyEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaRefundPolicyRepository;
import com.sarbar.support.domain.model.RefundPolicy;
import com.sarbar.support.domain.port.out.RefundPolicyRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RefundPolicyRepositoryAdapter implements RefundPolicyRepository {

  private final JpaRefundPolicyRepository jpa;

  public RefundPolicyRepositoryAdapter(JpaRefundPolicyRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<RefundPolicy> findCurrentVersion() {
    return jpa.findTopByOrderByVersionDesc().map(RefundPolicyEntity::toDomain);
  }
}
