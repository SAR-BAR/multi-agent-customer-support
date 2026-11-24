package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.RefundEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaRefundRepository;
import com.sarbar.support.domain.model.Refund;
import com.sarbar.support.domain.port.out.RefundRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RefundRepositoryAdapter implements RefundRepository {

  private final JpaRefundRepository jpa;

  public RefundRepositoryAdapter(JpaRefundRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Refund save(Refund refund) {
    return jpa.save(RefundEntity.fromDomain(refund)).toDomain();
  }

  @Override
  public List<Refund> findByOrderId(String orderId) {
    return jpa.findByOrderId(orderId).stream().map(RefundEntity::toDomain).toList();
  }

  @Override
  public boolean existsByIdempotencyKey(String idempotencyKey) {
    return jpa.existsByIdempotencyKey(idempotencyKey);
  }
}
