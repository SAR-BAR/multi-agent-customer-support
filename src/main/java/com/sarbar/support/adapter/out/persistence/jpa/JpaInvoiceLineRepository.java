package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.InvoiceLineEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvoiceLineRepository extends JpaRepository<InvoiceLineEntity, Long> {

  List<InvoiceLineEntity> findByInvoiceId(Long invoiceId);
}
