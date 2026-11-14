package com.sarbar.support.adapter.out.persistence.entity;

import com.sarbar.support.domain.model.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customers")
public class CustomerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String name;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected CustomerEntity() {}

  public static CustomerEntity fromDomain(Customer c) {
    CustomerEntity e = new CustomerEntity();
    e.id = c.id();
    e.email = c.email();
    e.name = c.name();
    e.createdAt = c.createdAt();
    return e;
  }

  public Customer toDomain() {
    return new Customer(id, email, name, createdAt);
  }

  public Long getId() {
    return id;
  }
}
