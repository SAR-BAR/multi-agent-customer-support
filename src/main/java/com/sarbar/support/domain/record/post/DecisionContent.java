package com.sarbar.support.domain.record.post;

import com.sarbar.support.domain.model.DecisionType;
import java.util.List;

public record DecisionContent(
    DecisionType type, List<Long> approvedProposalPostIds, String explanation) {}
