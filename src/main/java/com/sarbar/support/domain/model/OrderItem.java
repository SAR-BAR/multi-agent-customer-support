package com.sarbar.support.domain.model;

public record OrderItem(
    Long id,
    String orderId,
    String productName,
    int quantity,
    int unitPricePaise,
    int lineTotalPaise) {}
