package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.OrderItemEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaOrderItemRepository;
import com.sarbar.support.domain.model.OrderItem;
import com.sarbar.support.domain.port.out.OrderItemRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class OrderItemRepositoryAdapter implements OrderItemRepository {

  private final JpaOrderItemRepository jpa;

  public OrderItemRepositoryAdapter(JpaOrderItemRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public List<OrderItem> findByOrderId(String orderId) {
    return jpa.findByOrderId(orderId).stream().map(OrderItemEntity::toDomain).toList();
  }
}
