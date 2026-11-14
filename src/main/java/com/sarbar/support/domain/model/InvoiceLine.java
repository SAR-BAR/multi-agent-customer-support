package com.sarbar.support.domain.model;

public record InvoiceLine(
    Long id,
    Long invoiceId,
    String description,
    int quantity,
    int unitPricePaise,
    int lineTotalPaise) {}
