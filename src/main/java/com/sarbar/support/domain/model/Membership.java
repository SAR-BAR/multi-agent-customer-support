package com.sarbar.support.domain.model;

import java.time.Instant;

public record Membership(
    Long id,
    Long customerId,
    MembershipStatus status,
    boolean freeDelivery,
    Instant startedAt,
    Instant expiresAt) {}
