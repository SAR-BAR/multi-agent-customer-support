package com.sarbar.support.domain.model;

import java.time.Instant;

public record Conversation(Long id, Long customerId, Instant startedAt) {}
