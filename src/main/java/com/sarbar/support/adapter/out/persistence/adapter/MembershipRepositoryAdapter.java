package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.MembershipEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaMembershipRepository;
import com.sarbar.support.domain.model.Membership;
import com.sarbar.support.domain.port.out.MembershipRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipRepositoryAdapter implements MembershipRepository {

  private final JpaMembershipRepository jpa;

  public MembershipRepositoryAdapter(JpaMembershipRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Membership> findActiveByCustomerId(Long customerId) {
    return jpa.findActiveByCustomerId(customerId).map(MembershipEntity::toDomain);
  }
}
