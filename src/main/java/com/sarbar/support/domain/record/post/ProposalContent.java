package com.sarbar.support.domain.record.post;

import com.sarbar.support.domain.model.ProblemType;
import java.util.List;

public record ProposalContent(
    ProblemType problemType,
    String action,
    int amountPaise,
    List<String> evidenceIds,
    String reasoning) {

  public boolean involvesMoney() {
    return amountPaise > 0;
  }
}
