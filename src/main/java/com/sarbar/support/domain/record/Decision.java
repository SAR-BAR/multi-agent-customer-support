package com.sarbar.support.domain.record;

import com.sarbar.support.domain.model.DecisionType;
import java.util.List;

public record Decision(DecisionType type, List<Long> approvedProposalPostIds, String explanation) {}
