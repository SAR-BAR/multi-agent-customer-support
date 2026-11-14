package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

  Optional<Order> findById(String id);

  List<Order> findByCustomerId(Long customerId);
}
