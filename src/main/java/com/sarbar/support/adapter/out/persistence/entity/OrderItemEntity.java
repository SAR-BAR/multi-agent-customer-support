package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false, length = 36)
  private String orderId;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price_paise", nullable = false)
  private int unitPricePaise;

  @Column(name = "line_total_paise", nullable = false)
  private int lineTotalPaise;

  protected OrderItemEntity() {}

  public static OrderItemEntity fromDomain(OrderItem o) {
    OrderItemEntity e = new OrderItemEntity();
    e.id = o.id();
    e.orderId = o.orderId();
    e.productName = o.productName();
    e.quantity = o.quantity();
    e.unitPricePaise = o.unitPricePaise();
    e.lineTotalPaise = o.lineTotalPaise();
    return e;
  }

  public OrderItem toDomain() {
    return new OrderItem(id, orderId, productName, quantity, unitPricePaise, lineTotalPaise);
  }
}
