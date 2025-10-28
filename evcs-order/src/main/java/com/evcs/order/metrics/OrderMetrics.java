package com.evcs.order.metrics;

import com.evcs.common.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单服务业务监控指标
 * 
 * 监控指标包括：
 * - 订单创建成功/失败计数
 * - 订单创建响应时间
 * - 订单启动/停止成功/失败计数
 * - 当前活跃订单数
 * - 计费准确率
 */
@Slf4j
@Component
public class OrderMetrics extends BusinessMetrics {

    // 订单创建计数器
    private Counter orderCreatedCounter;
    private Counter orderCreatedFailureCounter;
    
    // 订单启动/停止计数器
    private Counter orderStartedCounter;
    private Counter orderStartedFailureCounter;
    private Counter orderStoppedCounter;
    private Counter orderStoppedFailureCounter;
    
    // 计费相关计数器
    private Counter billingSuccessCounter;
    private Counter billingFailureCounter;
    
    // 订单创建响应时间
    private Timer orderCreationTimer;
    
    // 当前活跃订单数
    private final AtomicInteger activeOrdersCount = new AtomicInteger(0);

    public OrderMetrics(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }

    @Override
    protected void registerMetrics() {
        // 订单创建指标
        orderCreatedCounter = createCounter(
            "evcs.order.created.total",
            "Total number of orders created successfully"
        );
        
        orderCreatedFailureCounter = createCounter(
            "evcs.order.created.failure.total",
            "Total number of failed order creations"
        );
        
        // 订单启动指标
        orderStartedCounter = createCounter(
            "evcs.order.started.total",
            "Total number of orders started successfully"
        );
        
        orderStartedFailureCounter = createCounter(
            "evcs.order.started.failure.total",
            "Total number of failed order starts"
        );
        
        // 订单停止指标
        orderStoppedCounter = createCounter(
            "evcs.order.stopped.total",
            "Total number of orders stopped successfully"
        );
        
        orderStoppedFailureCounter = createCounter(
            "evcs.order.stopped.failure.total",
            "Total number of failed order stops"
        );
        
        // 计费指标
        billingSuccessCounter = createCounter(
            "evcs.order.billing.success.total",
            "Total number of successful billing operations"
        );
        
        billingFailureCounter = createCounter(
            "evcs.order.billing.failure.total",
            "Total number of failed billing operations"
        );
        
        // 订单创建响应时间
        orderCreationTimer = createTimer(
            "evcs.order.creation.duration",
            "Order creation duration in seconds"
        );
        
        // 当前活跃订单数
        createGauge(
            "evcs.order.active.current",
            "Current number of active orders",
            activeOrdersCount
        );
        
        log.info("Order business metrics registered successfully");
    }

    /**
     * 记录订单创建成功
     */
    public void recordOrderCreated() {
        incrementCounter(orderCreatedCounter);
        activeOrdersCount.incrementAndGet();
    }

    /**
     * 记录订单创建失败
     */
    public void recordOrderCreatedFailure() {
        incrementCounter(orderCreatedFailureCounter);
    }

    /**
     * 记录订单启动成功
     */
    public void recordOrderStarted() {
        incrementCounter(orderStartedCounter);
    }

    /**
     * 记录订单启动失败
     */
    public void recordOrderStartedFailure() {
        incrementCounter(orderStartedFailureCounter);
    }

    /**
     * 记录订单停止成功
     */
    public void recordOrderStopped() {
        incrementCounter(orderStoppedCounter);
        activeOrdersCount.decrementAndGet();
    }

    /**
     * 记录订单停止失败
     */
    public void recordOrderStoppedFailure() {
        incrementCounter(orderStoppedFailureCounter);
    }

    /**
     * 记录计费成功
     */
    public void recordBillingSuccess() {
        incrementCounter(billingSuccessCounter);
    }

    /**
     * 记录计费失败
     */
    public void recordBillingFailure() {
        incrementCounter(billingFailureCounter);
    }

    /**
     * 获取订单创建Timer用于记录执行时间
     * 
     * @return Timer
     */
    public Timer getOrderCreationTimer() {
        return orderCreationTimer;
    }

    /**
     * 计算订单创建成功率
     * 
     * @return 成功率百分比 (0-100)
     */
    public double getOrderCreationSuccessRate() {
        double total = orderCreatedCounter.count() + orderCreatedFailureCounter.count();
        if (total == 0) {
            return 100.0;
        }
        return (orderCreatedCounter.count() / total) * 100.0;
    }

    /**
     * 计算计费准确率
     * 
     * @return 准确率百分比 (0-100)
     */
    public double getBillingAccuracyRate() {
        double total = billingSuccessCounter.count() + billingFailureCounter.count();
        if (total == 0) {
            return 100.0;
        }
        return (billingSuccessCounter.count() / total) * 100.0;
    }
}
