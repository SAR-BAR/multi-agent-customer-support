package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Customer;
import java.util.Optional;

public interface CustomerRepository {

  Optional<Customer> findById(Long id);

  Optional<Customer> findByEmail(String email);
}
