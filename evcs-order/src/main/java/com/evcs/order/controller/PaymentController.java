package com.evcs.order.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.order.service.IChargingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "支付(占位)", description = "创建支付与回调模拟")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final IChargingOrderService orderService;

    @PostMapping("/create")
    @Operation(summary = "为订单创建支付(占位)")
    @DataScope
    public Result<com.evcs.order.dto.PayParams> create(@RequestParam Long orderId) {
        var params = orderService.createPayment(orderId);
        return params != null ? Result.success(params) : Result.fail("创建支付失败");
    }

    @PostMapping("/callback")
    @Operation(summary = "支付回调(占位)")
    @DataScope
    public Result<Boolean> callback(@RequestParam String tradeId, @RequestParam(defaultValue = "true") boolean success) {
        return Result.success(orderService.paymentCallback(tradeId, success));
    }
}
