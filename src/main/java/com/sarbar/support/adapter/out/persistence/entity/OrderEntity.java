package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Order;
import com.sarbar.support.domain.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id private String id;

  @Column(name = "customer_id", nullable = false)
  private Long customerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  @Column(name = "total_paise", nullable = false)
  private int totalPaise;

  @Column(name = "delivery_fee_paise", nullable = false)
  private int deliveryFeePaise;

  @Column(name = "ordered_at", nullable = false)
  private Instant orderedAt;

  @Column(name = "expected_delivery_at")
  private Instant expectedDeliveryAt;

  @Version private long version;

  protected OrderEntity() {}

  public static OrderEntity fromDomain(Order o) {
    OrderEntity e = new OrderEntity();
    e.id = o.id();
    e.customerId = o.customerId();
    e.status = o.status();
    e.totalPaise = o.totalPaise();
    e.deliveryFeePaise = o.deliveryFeePaise();
    e.orderedAt = o.orderedAt();
    e.expectedDeliveryAt = o.expectedDeliveryAt();
    e.version = o.version();
    return e;
  }

  public Order toDomain() {
    return new Order(
        id,
        customerId,
        status,
        totalPaise,
        deliveryFeePaise,
        orderedAt,
        expectedDeliveryAt,
        version);
  }

  public String getId() {
    return id;
  }
}
