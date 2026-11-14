package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Shipment;
import java.util.List;

public interface ShipmentRepository {

  List<Shipment> findByOrderId(String orderId);
}
