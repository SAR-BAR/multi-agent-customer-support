package com.sarbar.support.domain.record.post;

import com.sarbar.support.domain.model.PostAuthor;
import com.sarbar.support.domain.model.ProblemType;

public record HandoffContent(ProblemType problemType, PostAuthor targetSpecialist, String reason) {}
