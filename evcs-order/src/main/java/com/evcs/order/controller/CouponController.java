package com.evcs.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.result.Result;
import com.evcs.order.dto.CouponResponse;
import com.evcs.order.entity.Coupon;
import com.evcs.order.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "营销优惠", description = "优惠券管理与发放")
@RestController
@RequestMapping("/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "发放优惠券", description = "给指定用户发放优惠券")
    @PostMapping("/issue")
    public Result<Void> issueCoupon(@RequestParam Long userId,
                                    @RequestParam String name,
                                    @RequestParam Integer type,
                                    @RequestParam BigDecimal value,
                                    @RequestParam BigDecimal minAmount,
                                    @RequestParam Integer validDays) {
        couponService.issueCoupon(userId, name, type, value, minAmount, validDays);
        return Result.success();
    }

    @Operation(summary = "查询我的优惠券", description = "查询当前用户的优惠券")
    @GetMapping("/my")
    public Result<List<CouponResponse>> myCoupons(@RequestParam Long userId, @RequestParam(required = false) BigDecimal orderAmount) {
        if (orderAmount != null) {
            return Result.success(couponService.listAvailableCoupons(userId, orderAmount).stream()
                    .map(CouponResponse::from).toList());
        }
        // Simple list all for now if no amount
        return Result.success(couponService.lambdaQuery().eq(Coupon::getUserId, userId).list().stream()
                .map(CouponResponse::from).toList());
    }
}
