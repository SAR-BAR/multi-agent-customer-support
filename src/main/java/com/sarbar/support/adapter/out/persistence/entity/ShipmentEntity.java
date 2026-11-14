package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Shipment;
import com.sarbar.support.domain.model.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class ShipmentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false, length = 36)
  private String orderId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ShipmentStatus status;

  @Column(length = 100)
  private String carrier;

  @Column(name = "tracking_number", length = 100)
  private String trackingNumber;

  @Column(name = "shipped_at")
  private Instant shippedAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  protected ShipmentEntity() {}

  public static ShipmentEntity fromDomain(Shipment s) {
    ShipmentEntity e = new ShipmentEntity();
    e.id = s.id();
    e.orderId = s.orderId();
    e.status = s.status();
    e.carrier = s.carrier();
    e.trackingNumber = s.trackingNumber();
    e.shippedAt = s.shippedAt();
    e.deliveredAt = s.deliveredAt();
    return e;
  }

  public Shipment toDomain() {
    return new Shipment(id, orderId, status, carrier, trackingNumber, shippedAt, deliveredAt);
  }
}
