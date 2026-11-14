package com.sarbar.support.domain.record;

import com.sarbar.support.domain.model.ProblemType;

public record ProblemRef(ProblemType type, String orderId, String description) {}
