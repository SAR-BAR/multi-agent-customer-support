package com.sarbar.support.adapter.out.agent.llm;

import java.util.List;

public record SpecialistResponse(List<Action> actions) {

  public record Action(
      String type,
      String problemType,
      String action,
      int amountPaise,
      List<String> evidenceIds,
      String reasoning,
      Long proposalPostId,
      String targetSpecialist,
      Long challengePostId) {}
}
