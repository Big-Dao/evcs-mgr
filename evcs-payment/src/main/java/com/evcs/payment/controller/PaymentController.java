package com.evcs.payment.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.service.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器
 */
@Tag(name = "支付管理", description = "支付订单创建、查询、回调、退款")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/create")
    @Operation(summary = "创建支付订单")
    @DataScope
    public Result<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return Result.success(response);
    }

    @GetMapping("/query/{tradeNo}")
    @Operation(summary = "查询支付状态")
    @DataScope
    public Result<PaymentResponse> queryPayment(@PathVariable String tradeNo) {
        PaymentResponse response = paymentService.queryPayment(tradeNo);
        return response != null ? Result.success(response) : Result.fail("支付订单不存在");
    }

    @PostMapping("/callback/simple/alipay")
    @Operation(summary = "支付宝支付回调（简化版）")
    public Result<Boolean> alipayCallback(@RequestParam String tradeNo,
                                         @RequestParam(defaultValue = "true") boolean success) {
        boolean handled = paymentService.handlePaymentCallback(tradeNo, success);
        return Result.success(handled);
    }

    @PostMapping("/callback/simple/wechat")
    @Operation(summary = "微信支付回调（简化版）")
    public Result<Boolean> wechatCallback(@RequestParam String tradeNo,
                                         @RequestParam(defaultValue = "true") boolean success) {
        boolean handled = paymentService.handlePaymentCallback(tradeNo, success);
        return Result.success(handled);
    }

    @PostMapping("/refund")
    @Operation(summary = "退款")
    @DataScope
    public Result<RefundResponse> refund(@RequestBody RefundRequest request) {
        RefundResponse response = paymentService.refund(request);
        return Result.success(response);
    }
}
