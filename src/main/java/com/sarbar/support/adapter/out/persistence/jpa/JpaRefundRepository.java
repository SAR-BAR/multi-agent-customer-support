package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.RefundEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRefundRepository extends JpaRepository<RefundEntity, Long> {

  List<RefundEntity> findByOrderId(String orderId);

  boolean existsByIdempotencyKey(String idempotencyKey);
}
