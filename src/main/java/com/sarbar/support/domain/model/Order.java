package com.sarbar.support.domain.model;

import java.time.Instant;

public record Order(
    String id,
    Long customerId,
    OrderStatus status,
    int totalPaise,
    int deliveryFeePaise,
    Instant orderedAt,
    Instant expectedDeliveryAt,
    long version) {}
