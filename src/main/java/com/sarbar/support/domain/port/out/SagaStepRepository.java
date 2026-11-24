package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.SagaStepRecord;
import java.util.List;
import java.util.Optional;

public interface SagaStepRepository {

  SagaStepRecord save(SagaStepRecord step);

  List<SagaStepRecord> findBySagaId(Long sagaId);

  Optional<SagaStepRecord> findBySagaIdAndStepName(Long sagaId, String stepName);
}
