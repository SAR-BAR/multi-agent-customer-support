package com.sarbar.support.domain.service.saga;

import com.sarbar.support.domain.model.SagaStatus;
import com.sarbar.support.domain.model.SagaType;

public record SagaResult(Long sagaId, SagaType sagaType, SagaStatus status, String detail) {

  public boolean succeeded() {
    return status == SagaStatus.DONE;
  }
}
