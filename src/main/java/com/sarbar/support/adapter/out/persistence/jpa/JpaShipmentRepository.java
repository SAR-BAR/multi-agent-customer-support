package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.ShipmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaShipmentRepository extends JpaRepository<ShipmentEntity, Long> {

  List<ShipmentEntity> findByOrderId(String orderId);
}
