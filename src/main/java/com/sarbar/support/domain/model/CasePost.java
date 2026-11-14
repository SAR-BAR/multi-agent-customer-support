package com.sarbar.support.domain.model;

import java.time.Instant;
import java.util.List;

public record CasePost(
    Long id,
    Long caseId,
    PostAuthor author,
    PostType type,
    String contentJson,
    List<String> evidenceIds,
    Long replyTo,
    Instant createdAt) {}
