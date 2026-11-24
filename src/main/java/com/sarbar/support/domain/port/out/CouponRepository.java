package com.sarbar.support.domain.port.out;

import com.sarbar.support.domain.model.Coupon;
import java.util.Optional;

public interface CouponRepository {

  Coupon save(Coupon coupon);

  Optional<Coupon> findByCode(String code);
}
