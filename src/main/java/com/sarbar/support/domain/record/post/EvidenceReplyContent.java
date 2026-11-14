package com.sarbar.support.domain.record.post;

import java.util.List;

public record EvidenceReplyContent(
    Long challengePostId, List<String> evidenceIds, String explanation) {}
