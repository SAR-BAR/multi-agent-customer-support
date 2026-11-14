package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.OrderEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaOrderRepository;
import com.sarbar.support.domain.model.Order;
import com.sarbar.support.domain.port.out.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {

  private final JpaOrderRepository jpa;

  public OrderRepositoryAdapter(JpaOrderRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Order> findById(String id) {
    return jpa.findById(id).map(OrderEntity::toDomain);
  }

  @Override
  public List<Order> findByCustomerId(Long customerId) {
    return jpa.findByCustomerId(customerId).stream().map(OrderEntity::toDomain).toList();
  }
}
