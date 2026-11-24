package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.SagaInstance;
import java.util.Optional;

public interface SagaInstanceRepository {

  SagaInstance save(SagaInstance instance);

  Optional<SagaInstance> findById(Long id);

  Optional<SagaInstance> findByCaseId(Long caseId);
}
