package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Invoice;
import com.sarbar.support.domain.model.InvoiceLine;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

  Optional<Invoice> findByOrderId(String orderId);

  List<InvoiceLine> findLinesByInvoiceId(Long invoiceId);
}
