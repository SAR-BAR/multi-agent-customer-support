package com.sarbar.support.domain.model;

import java.time.Instant;

public record Case(
    Long id,
    Long conversationId,
    String orderId,
    CaseStatus status,
    Instant openedAt,
    Instant closedAt) {}
