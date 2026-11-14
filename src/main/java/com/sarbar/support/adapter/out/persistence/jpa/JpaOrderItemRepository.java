package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.OrderItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

  List<OrderItemEntity> findByOrderId(String orderId);
}
