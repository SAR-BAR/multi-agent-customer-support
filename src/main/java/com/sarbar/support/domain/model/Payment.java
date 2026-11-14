package com.sarbar.support.domain.model;

import java.time.Instant;

public record Payment(
    Long id,
    String orderId,
    PaymentStatus status,
    int amountPaise,
    String method,
    String transactionRef,
    Instant paidAt) {}
