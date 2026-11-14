package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.RefundPolicy;
import java.util.Optional;

public interface RefundPolicyRepository {

  Optional<RefundPolicy> findCurrentVersion();
}
