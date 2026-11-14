package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.ShipmentEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaShipmentRepository;
import com.sarbar.support.domain.model.Shipment;
import com.sarbar.support.domain.port.out.ShipmentRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ShipmentRepositoryAdapter implements ShipmentRepository {

  private final JpaShipmentRepository jpa;

  public ShipmentRepositoryAdapter(JpaShipmentRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<Shipment> findByOrderId(String orderId) {
    return jpa.findByOrderId(orderId).stream().map(ShipmentEntity::toDomain).toList();
  }
}
