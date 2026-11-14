package com.sarbar.support.adapter.out.persistence.jpa;

import com.sarbar.support.adapter.out.persistence.entity.CustomerEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCustomerRepository extends JpaRepository<CustomerEntity, Long> {

  Optional<CustomerEntity> findByEmail(String email);
}
