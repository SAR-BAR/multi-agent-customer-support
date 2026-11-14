package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.CustomerEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaCustomerRepository;
import com.sarbar.support.domain.model.Customer;
import com.sarbar.support.domain.port.out.CustomerRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepositoryAdapter implements CustomerRepository {

  private final JpaCustomerRepository jpa;

  public CustomerRepositoryAdapter(JpaCustomerRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Customer> findById(Long id) {
    return jpa.findById(id).map(CustomerEntity::toDomain);
  }

  @Override
  public Optional<Customer> findByEmail(String email) {
    return jpa.findByEmail(email).map(CustomerEntity::toDomain);
  }
}
