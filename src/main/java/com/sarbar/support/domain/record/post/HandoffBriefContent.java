package com.sarbar.support.domain.record.post;

import java.util.List;

public record HandoffBriefContent(
    String whatWeChecked, String whatWeFound, List<String> flags, String suggestedAction) {}
