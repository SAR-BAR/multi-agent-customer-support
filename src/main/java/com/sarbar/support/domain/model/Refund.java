package com.sarbar.support.domain.model;

import java.time.Instant;

public record Refund(
    Long id, String orderId, int amountPaise, String idempotencyKey, Instant refundedAt) {}
