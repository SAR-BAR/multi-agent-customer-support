package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Payment;
import java.util.List;

public interface PaymentRepository {

  List<Payment> findByOrderId(String orderId);
}
