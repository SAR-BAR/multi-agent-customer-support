package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Refund;
import java.util.List;

public interface RefundRepository {

  Refund save(Refund refund);

  List<Refund> findByOrderId(String orderId);

  boolean existsByIdempotencyKey(String idempotencyKey);
}
