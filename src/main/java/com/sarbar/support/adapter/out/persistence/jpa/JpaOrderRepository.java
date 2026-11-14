package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.OrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, String> {

  List<OrderEntity> findByCustomerId(Long customerId);
}
