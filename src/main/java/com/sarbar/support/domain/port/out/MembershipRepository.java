package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Membership;
import java.util.Optional;

public interface MembershipRepository {

  Optional<Membership> findActiveByCustomerId(Long customerId);
}
