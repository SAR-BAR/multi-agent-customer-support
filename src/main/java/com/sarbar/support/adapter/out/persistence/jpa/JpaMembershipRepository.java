package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.MembershipEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaMembershipRepository extends JpaRepository<MembershipEntity, Long> {

  @Query(
      "SELECT m FROM MembershipEntity m WHERE m.customerId = :customerId AND m.status = 'ACTIVE'")
  Optional<MembershipEntity> findActiveByCustomerId(Long customerId);
}
