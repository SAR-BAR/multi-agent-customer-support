package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.RefundPolicyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaRefundPolicyRepository extends JpaRepository<RefundPolicyEntity, Long> {

  @Query("SELECT rp FROM RefundPolicyEntity rp ORDER BY rp.version DESC LIMIT 1")
  Optional<RefundPolicyEntity> findTopByOrderByVersionDesc();
}
