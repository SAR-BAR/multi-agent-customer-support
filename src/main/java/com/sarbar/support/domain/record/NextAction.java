package com.sarbar.support.domain.record;

import java.util.List;

public record NextAction(Actor actor, List<Long> targetPostIds, String reason) {

  public enum Actor {
    RESOLUTION,
    BILLING,
    RISK,
    DECISION_ENGINE,
    COMMUNICATOR,
    ESCALATE,
    DONE
  }
}
