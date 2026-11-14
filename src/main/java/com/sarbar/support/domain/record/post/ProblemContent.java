package com.sarbar.support.domain.record.post;

import com.sarbar.support.domain.model.ProblemType;

public record ProblemContent(ProblemType type, String orderId, String description) {}
