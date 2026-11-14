package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Escalation;
import java.util.Optional;

public interface EscalationRepository {

  Optional<Escalation> findById(Long id);

  Optional<Escalation> findByCaseId(Long caseId);

  Escalation save(Escalation escalation);
}
