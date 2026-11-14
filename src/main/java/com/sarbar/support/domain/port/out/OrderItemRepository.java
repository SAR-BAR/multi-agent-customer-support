package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.OrderItem;
import java.util.List;

public interface OrderItemRepository {

  List<OrderItem> findByOrderId(String orderId);
}
