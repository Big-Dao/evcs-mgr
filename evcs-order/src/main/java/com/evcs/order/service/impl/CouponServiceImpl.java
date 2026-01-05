package com.evcs.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.order.entity.Coupon;
import com.evcs.order.mapper.CouponMapper;
import com.evcs.order.service.CouponService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements CouponService {

    @Override
    public void issueCoupon(Long userId, String name, Integer type, BigDecimal value, BigDecimal minAmount, Integer validDays) {
        Coupon coupon = new Coupon();
        coupon.setUserId(userId);
        coupon.setName(name);
        coupon.setType(type);
        coupon.setValue(value);
        coupon.setMinAmount(minAmount);
        coupon.setStartTime(LocalDateTime.now());
        coupon.setEndTime(LocalDateTime.now().plusDays(validDays));
        coupon.setStatus(0); // Unused
        save(coupon);
    }

    @Override
    public List<Coupon> listAvailableCoupons(Long userId, BigDecimal orderAmount) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getUserId, userId)
               .eq(Coupon::getStatus, 0) // Unused
               .le(Coupon::getMinAmount, orderAmount)
               .gt(Coupon::getEndTime, LocalDateTime.now());
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal useCoupon(Long couponId, Long orderId, BigDecimal orderAmount) {
        Coupon coupon = getById(couponId);
        if (coupon == null) {
            throw new RuntimeException("Coupon not found");
        }
        if (coupon.getStatus() != 0) {
            throw new RuntimeException("Coupon is not available");
        }
        if (coupon.getEndTime().isBefore(LocalDateTime.now())) {
            coupon.setStatus(2); // Expired
            updateById(coupon);
            throw new RuntimeException("Coupon expired");
        }
        if (orderAmount.compareTo(coupon.getMinAmount()) < 0) {
            throw new RuntimeException("Order amount does not meet minimum requirement");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getType() == 1) { // Amount Off
            discount = coupon.getValue();
        } else if (coupon.getType() == 2) { // Discount
            discount = orderAmount.multiply(BigDecimal.ONE.subtract(coupon.getValue()));
        }

        // Ensure discount doesn't exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        coupon.setStatus(1); // Used
        coupon.setOrderId(orderId);
        updateById(coupon);

        return discount;
    }
}
