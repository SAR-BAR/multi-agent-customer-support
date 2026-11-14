package com.sarbar.support.domain.model;

import java.time.Instant;

public record Invoice(Long id, String orderId, int totalPaise, Instant issuedAt) {}
