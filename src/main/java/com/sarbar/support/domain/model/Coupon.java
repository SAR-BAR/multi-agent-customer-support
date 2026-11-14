package com.sarbar.support.domain.model;

import java.time.Instant;

public record Coupon(
    Long id, String code, String orderId, int discountPaise, Instant issuedAt, Instant expiresAt) {}
