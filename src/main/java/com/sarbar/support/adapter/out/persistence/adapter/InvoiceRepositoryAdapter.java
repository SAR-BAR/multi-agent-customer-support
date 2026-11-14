package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.InvoiceEntity;
import com.sarbar.support.adapter.out.persistence.entity.InvoiceLineEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaInvoiceLineRepository;
import com.sarbar.support.adapter.out.persistence.jpa.JpaInvoiceRepository;
import com.sarbar.support.domain.model.Invoice;
import com.sarbar.support.domain.model.InvoiceLine;
import com.sarbar.support.domain.port.out.InvoiceRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepositoryAdapter implements InvoiceRepository {

  private final JpaInvoiceRepository invoiceJpa;
  private final JpaInvoiceLineRepository lineJpa;

  public InvoiceRepositoryAdapter(
      JpaInvoiceRepository invoiceJpa, JpaInvoiceLineRepository lineJpa) {
    this.invoiceJpa = invoiceJpa;
    this.lineJpa = lineJpa;
  }

  @Override
  public Optional<Invoice> findByOrderId(String orderId) {
    return invoiceJpa.findByOrderId(orderId).map(InvoiceEntity::toDomain);
  }

  @Override
  public List<InvoiceLine> findLinesByInvoiceId(Long invoiceId) {
    return lineJpa.findByInvoiceId(invoiceId).stream().map(InvoiceLineEntity::toDomain).toList();
  }
}
