package com.evcs.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.tenant.TenantContext;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.mapper.ChargingOrderMapper;
import com.evcs.order.service.IChargingOrderService;
import org.springframework.stereotype.Service;
import java.math.RoundingMode;
import com.evcs.common.annotation.DataScope;
import com.evcs.order.service.IBillingService;
import com.evcs.order.metrics.OrderMetrics;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingOrderServiceImpl extends ServiceImpl<ChargingOrderMapper, ChargingOrder> implements IChargingOrderService {
    private final IBillingService billingService;
    private final MeterRegistry meterRegistry;
    private final OrderMetrics orderMetrics;

    @Override
    @DataScope
    public boolean createOrderOnStart(Long stationId, Long chargerId, String sessionId, Long userId, Long billingPlanId) {
        // 记录订单创建时间
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // 幂等：同一租户+sessionId 已存在则直接返回成功
            // MyBatis Plus自动添加tenant_id过滤
            ChargingOrder exist = this.getOne(new QueryWrapper<ChargingOrder>()
                    .eq("session_id", sessionId)
                    .orderByDesc("id").last("limit 1"));
            if (exist != null) {
                orderMetrics.recordOrderStarted();
                return true;
            }
            
            ChargingOrder order = new ChargingOrder();
            order.setTenantId(TenantContext.getCurrentTenantId());
            order.setStationId(stationId);
            order.setChargerId(chargerId);
            meterRegistry.counter("evcs.order.created.attempt").increment();

            order.setSessionId(sessionId);
            order.setUserId(userId);
            order.setStartTime(LocalDateTime.now());
            order.setStatus(0); // INIT/CHARGING
            order.setBillingPlanId(billingPlanId);
            order.setAmount(BigDecimal.ZERO);
            order.setVersion(0);
            
            boolean result = this.save(order);
            
            if (result) {
                orderMetrics.recordOrderCreated();
                orderMetrics.recordOrderStarted();
                log.info("Order created successfully: sessionId={}, orderId={}", sessionId, order.getId());
            } else {
                orderMetrics.recordOrderCreatedFailure();
                log.error("Failed to create order: sessionId={}", sessionId);
            }
            
            return result;
        } catch (Exception e) {
            orderMetrics.recordOrderCreatedFailure();
            log.error("Error creating order: sessionId={}", sessionId, e);
            throw e;
        } finally {
            sample.stop(orderMetrics.getOrderCreationTimer());
        }
    }

    @Override
    @DataScope
    public boolean completeOrderOnStop(String sessionId, Double energy, Long duration) {
        try {
            // MyBatis Plus自动添加tenant_id过滤
            ChargingOrder order = this.getOne(new QueryWrapper<ChargingOrder>()
                    .eq("session_id", sessionId)
                    .orderByDesc("id").last("limit 1"));
            if (order == null) {
                orderMetrics.recordOrderStoppedFailure();
                log.error("Order not found for sessionId: {}", sessionId);
                return false;
            }
            // 幂等：已完成直接返回
            if (order.getStatus() != null && order.getStatus() == 1) {
                orderMetrics.recordOrderStopped();
                return true;
            }
            
            order.setEndTime(LocalDateTime.now());
            meterRegistry.counter("evcs.order.completed").increment();

            order.setEnergy(energy);
            order.setDuration(duration);
            order.setStatus(1); // COMPLETED
            
            // 计费：按配置/计划进行
            try {
                BigDecimal amount = billingService.calculateAmount(
                    order.getStartTime(), 
                    order.getEndTime(), 
                    energy, 
                    order.getStationId(), 
                    order.getChargerId(), 
                    order.getBillingPlanId()
                );
                order.setAmount(amount);
                orderMetrics.recordBillingSuccess();
            } catch (Exception e) {
                orderMetrics.recordBillingFailure();
                log.error("Billing calculation failed for sessionId: {}", sessionId, e);
                // 继续完成订单，但计费失败
                order.setAmount(BigDecimal.ZERO);
            }
            
            boolean result = this.updateById(order);
            
            if (result) {
                orderMetrics.recordOrderStopped();
                log.info("Order completed successfully: sessionId={}, orderId={}, amount={}", 
                    sessionId, order.getId(), order.getAmount());
            } else {
                orderMetrics.recordOrderStoppedFailure();
                log.error("Failed to complete order: sessionId={}", sessionId);
            }
            
            return result;
        } catch (Exception e) {
            orderMetrics.recordOrderStoppedFailure();
            log.error("Error completing order: sessionId={}", sessionId, e);
            throw e;
        }
    }

    @Override
    @DataScope
    public ChargingOrder getBySessionId(String sessionId) {
        // MyBatis Plus自动添加tenant_id过滤
        return this.getOne(new QueryWrapper<ChargingOrder>()
                .eq("session_id", sessionId)
                .orderByDesc("id").last("limit 1"));
    }

    @Override
    @DataScope
    public boolean markToPay(Long orderId) {
        // MyBatis Plus自动添加tenant_id过滤
        ChargingOrder order = this.getOne(new QueryWrapper<ChargingOrder>()
                .eq("id", orderId)
                .last("limit 1"));
        if (order == null) return false;
        // 允许从 COMPLETED(1) 进入 TO_PAY(10)，幂等允许重复设置
        if (order.getStatus() != null && (order.getStatus() == 10 || order.getStatus() == 1)) {
            order.setStatus(10);
            return this.updateById(order);
        }
        return false;
    }

    @Override
    @DataScope
    public boolean markPaid(Long orderId) {
        // MyBatis Plus自动添加tenant_id过滤
        ChargingOrder order = this.getOne(new QueryWrapper<ChargingOrder>()
                .eq("id", orderId)
                .last("limit 1"));
        if (order == null) return false;
        // 允许从 TO_PAY(10) 进入 PAID(11)，幂等允许重复设置
        if (order.getStatus() != null && (order.getStatus() == 11 || order.getStatus() == 10)) {
            order.setStatus(11);
            return this.updateById(order);
        }
        return false;
    }

    @Override
    @DataScope
    public com.evcs.order.dto.PayParams createPayment(Long orderId) {
        // MyBatis Plus自动添加tenant_id过滤
        ChargingOrder order = this.getOne(new QueryWrapper<ChargingOrder>()
                .eq("id", orderId)
                .last("limit 1"));
        if (order == null) return null;
        // 进入待支付
        meterRegistry.counter("evcs.order.payment.create").increment();

        if (order.getStatus() == null || order.getStatus() < 10) {
            order.setStatus(10);
        }
        if (order.getPaymentTradeId() == null || order.getPaymentTradeId().isEmpty()) {
            order.setPaymentTradeId(java.util.UUID.randomUUID().toString());
        }
        this.updateById(order);
        com.evcs.order.dto.PayParams params = new com.evcs.order.dto.PayParams();
        params.setTradeId(order.getPaymentTradeId());
        params.setPayUrl("https://pay.mock/qr/" + order.getPaymentTradeId());
        params.setAmount(order.getAmount());
        params.setExpireTime(java.time.LocalDateTime.now().plusMinutes(15));
        return params;
    }

    @Override
    @DataScope
    public boolean paymentCallback(String tradeId, boolean success) {
        meterRegistry.counter("evcs.order.paid").increment();

        if (tradeId == null || tradeId.isEmpty()) return false;
        // MyBatis Plus自动添加tenant_id过滤
        ChargingOrder order = this.getOne(new QueryWrapper<ChargingOrder>()
                .eq("payment_trade_id", tradeId)
                .last("limit 1"));
        if (order == null) return false;
        if (!success) return false;
        // 已支付幂等
        if (order.getStatus() != null && order.getStatus() == 11) return true;
        order.setStatus(11);
        order.setPaidTime(LocalDateTime.now());
        return this.updateById(order);
    }


    @Override
    @DataScope
    public boolean cancelOrder(Long orderId) {
        // MyBatis Plus自动添加tenant_id过滤
        ChargingOrder order = this.getOne(new QueryWrapper<ChargingOrder>()
                .eq("id", orderId)
                .last("limit 1"));
        if (order == null) return false;
        // 未支付的订单允许取消（非已支付/已退款）
        Integer st = order.getStatus();
        if (st == null || st == 0 || st == 1 || st == 10) {
            order.setStatus(2);
            return this.updateById(order);
        }
        return false;
    }

    @Override
    @DataScope
    public boolean markRefunding(Long orderId) {
        // MyBatis Plus自动添加tenant_id过滤
        ChargingOrder order = this.getOne(new QueryWrapper<ChargingOrder>()
                .eq("id", orderId)
                .last("limit 1"));
        if (order == null) return false;
        if (order.getStatus() != null && (order.getStatus() == 11 || order.getStatus() == 12)) {
            order.setStatus(12);
            return this.updateById(order);
        }
        return false;
    }

    @Override
    @DataScope
    public boolean markRefunded(Long orderId) {
        // MyBatis Plus自动添加tenant_id过滤
        ChargingOrder order = this.getOne(new QueryWrapper<ChargingOrder>()
                .eq("id", orderId)
                .last("limit 1"));
        if (order == null) return false;
        if (order.getStatus() != null && (order.getStatus() == 12 || order.getStatus() == 13)) {
            order.setStatus(13);
            return this.updateById(order);
        }
        return false;
    }

    @Override
    @DataScope
    public IPage<ChargingOrder> pageOrders(Page<ChargingOrder> page, Long stationId, Long chargerId, Long userId, Integer status) {
        QueryWrapper<ChargingOrder> wrapper = new QueryWrapper<>();
        // MyBatis Plus自动添加tenant_id过滤
        if (stationId != null) wrapper.eq("station_id", stationId);
        if (chargerId != null) wrapper.eq("charger_id", chargerId);
        if (userId != null) wrapper.eq("user_id", userId);
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("id");
        return this.page(page, wrapper);
    }
}
