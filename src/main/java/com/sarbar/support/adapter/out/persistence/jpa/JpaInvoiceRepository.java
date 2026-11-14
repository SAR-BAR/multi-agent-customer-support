package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.InvoiceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

  Optional<InvoiceEntity> findByOrderId(String orderId);
}
