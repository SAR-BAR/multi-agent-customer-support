package com.sarbar.support.domain.record;

import com.sarbar.support.domain.model.GeneralType;
import java.util.List;

public record TriageFinding(
    List<ProblemRef> problems,
    List<String> orderIds,
    GeneralType generalType,
    double frustration,
    boolean legalOrSocialThreat,
    boolean asksForHuman,
    List<String> missingFields,
    boolean suspiciousContent) {}
