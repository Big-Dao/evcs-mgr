package com.evcs.order.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.order.dto.OrderDTO;
import com.evcs.order.dto.OrderResponse;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.service.IChargingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理", description = "充电订单查询接口")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final IChargingOrderService orderService;

    @GetMapping({"/page", "/list"})
    @Operation(summary = "分页查询订单")
    @DataScope
    public Result<IPage<OrderDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Long chargerId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String sessionId) {
        Page<OrderDTO> p = new Page<>(page, size);
        ChargingOrder query = new ChargingOrder();
        query.setStationId(stationId);
        query.setChargerId(chargerId);
        query.setUserId(userId);
        query.setStatus(status);
        query.setSessionId(sessionId);
        return Result.success(orderService.getOrderPage(p, query));
    }

    @GetMapping("/by-session")
    @Operation(summary = "按会话查询订单")
    @DataScope
    public Result<OrderResponse> bySession(@Parameter(description = "会话ID") @RequestParam @NotNull String sessionId) {
        return Result.success(OrderResponse.from(orderService.getBySessionId(sessionId)));
    }

    @PostMapping("/start")
    @Operation(summary = "协议事件：开始充电->创建订单(幂等)")
    @DataScope
    public Result<Boolean> start(@RequestParam Long stationId,
                                 @RequestParam Long chargerId,
                                 @RequestParam String sessionId,
                                 @RequestParam(required = false) Long userId,
                                 @RequestParam(required = false) Long billingPlanId) {
        return Result.success(orderService.createOrderOnStart(stationId, chargerId, sessionId, userId, billingPlanId));
    }

    @PostMapping("/stop")
    @Operation(summary = "协议事件：停止充电->完成订单(幂等)")
    @DataScope
    public Result<Boolean> stop(@RequestParam String sessionId,
                                @RequestParam Double energy,
                                @RequestParam Long duration) {
        return Result.success(orderService.completeOrderOnStop(sessionId, energy, duration));
    }

    @PostMapping("/{orderId:\\d+}/to-pay")
    @Operation(summary = "订单进入待支付状态")
    @DataScope
    public Result<Boolean> toPay(@PathVariable Long orderId) {
        return Result.success(orderService.markToPay(orderId));
    }

    @PostMapping("/{orderId:\\d+}/paid")
    @Operation(summary = "订单支付完成")
    @DataScope
    public Result<Boolean> paid(@PathVariable Long orderId) {
        return Result.success(orderService.markPaid(orderId));
    }

    @PostMapping("/{orderId:\\d+}/cancel")
    @Operation(summary = "取消订单")
    @DataScope
    public Result<Boolean> cancel(@PathVariable Long orderId) {
        return Result.success(orderService.cancelOrder(orderId));
    }

    @PostMapping("/{orderId:\\d+}/refunding")
    @Operation(summary = "订单进入退款中")
    @DataScope
    public Result<Boolean> refunding(@PathVariable Long orderId) {
        return Result.success(orderService.markRefunding(orderId));
    }

    @PostMapping("/{orderId:\\d+}/refunded")
    @Operation(summary = "订单退款完成")
    @DataScope
    public Result<Boolean> refunded(@PathVariable Long orderId) {
        return Result.success(orderService.markRefunded(orderId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询订单")
    @DataScope
    public Result<OrderResponse> getById(@PathVariable Long id) {
        return Result.success(OrderResponse.from(orderService.getById(id)));
    }

    @PostMapping("/payment/callback")
    @Operation(summary = "支付回调通知")
    @DataScope
    public Result<Boolean> paymentCallback(@RequestParam String tradeId, @RequestParam boolean success) {
        return Result.success(orderService.paymentCallback(tradeId, success));
    }

    @GetMapping("/statistics/city")
    @Operation(summary = "获取城市级别订单统计", description = "用于地图可视化分析")
    @DataScope
    public Result<java.util.List<com.evcs.order.dto.CityOrderStatistics>> getCityStatistics(
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        
        if (startTime != null && !startTime.isEmpty()) {
            start = java.time.LocalDateTime.parse(startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            end = java.time.LocalDateTime.parse(endTime);
        }
        
        return Result.success(orderService.getCityOrderStatistics(start, end));
    }

}
