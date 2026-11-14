package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.PaymentEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaPaymentRepository;
import com.sarbar.support.domain.model.Payment;
import com.sarbar.support.domain.port.out.PaymentRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

  private final JpaPaymentRepository jpa;

  public PaymentRepositoryAdapter(JpaPaymentRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<Payment> findByOrderId(String orderId) {
    return jpa.findByOrderId(orderId).stream().map(PaymentEntity::toDomain).toList();
  }
}
