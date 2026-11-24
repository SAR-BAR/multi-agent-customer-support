package com.sarbar.support.adapter.out.persistence.adapter;

import com.sarbar.support.adapter.out.persistence.entity.CouponEntity;
import com.sarbar.support.adapter.out.persistence.jpa.JpaCouponRepository;
import com.sarbar.support.domain.model.Coupon;
import com.sarbar.support.domain.port.out.CouponRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CouponRepositoryAdapter implements CouponRepository {

  private final JpaCouponRepository jpa;

  public CouponRepositoryAdapter(JpaCouponRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Coupon save(Coupon coupon) {
    return jpa.save(CouponEntity.fromDomain(coupon)).toDomain();
  }

  @Override
  public Optional<Coupon> findByCode(String code) {
    return jpa.findByCode(code).map(CouponEntity::toDomain);
  }
}
