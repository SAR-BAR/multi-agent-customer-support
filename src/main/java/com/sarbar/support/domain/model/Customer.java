package com.sarbar.support.domain.model;

import java.time.Instant;

public record Customer(Long id, String email, String name, Instant createdAt) {}
