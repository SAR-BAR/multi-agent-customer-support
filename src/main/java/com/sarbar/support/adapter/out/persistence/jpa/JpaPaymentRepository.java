package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.PaymentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPaymentRepository extends JpaRepository<PaymentEntity, Long> {

  List<PaymentEntity> findByOrderId(String orderId);
}
