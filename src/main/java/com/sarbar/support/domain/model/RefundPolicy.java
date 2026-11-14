package com.sarbar.support.domain.model;

import java.time.Instant;

public record RefundPolicy(Long id, int version, String content, Instant effectiveFrom) {}
