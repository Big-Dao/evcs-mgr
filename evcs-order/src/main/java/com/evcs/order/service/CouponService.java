package com.evcs.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.order.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService extends IService<Coupon> {

    /**
     * 发放优惠券
     */
    void issueCoupon(Long userId, String name, Integer type, BigDecimal value, BigDecimal minAmount, Integer validDays);

    /**
     * 获取用户可用优惠券
     */
    List<Coupon> listAvailableCoupons(Long userId, BigDecimal orderAmount);

    /**
     * 核销优惠券
     */
    BigDecimal useCoupon(Long couponId, Long orderId, BigDecimal orderAmount);
}
