package com.sarbar.support.domain.model;

import java.time.Instant;

public record Escalation(
    Long id, Long caseId, String reason, Instant escalatedAt, Instant resolvedAt) {}
