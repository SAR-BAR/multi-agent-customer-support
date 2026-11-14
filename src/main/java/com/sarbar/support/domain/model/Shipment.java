package com.sarbar.support.domain.model;

import java.time.Instant;

public record Shipment(
    Long id,
    String orderId,
    ShipmentStatus status,
    String carrier,
    String trackingNumber,
    Instant shippedAt,
    Instant deliveredAt) {}
