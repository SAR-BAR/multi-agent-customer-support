package com.sarbar.support.domain.model;

import java.time.Instant;

public record CreditNote(
    Long id, String creditNoteNumber, String orderId, int amountPaise, Instant issuedAt) {}
