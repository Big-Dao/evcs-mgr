package com.evcs.payment.metrics;

import com.evcs.common.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 支付服务业务监控指标
 * 
 * 监控指标包括：
 * - 支付请求成功/失败计数
 * - 支付回调成功/失败计数
 * - 支付金额统计
 * - 支付响应时间
 * - 不同支付渠道的成功率
 */
@Slf4j
@Component
public class PaymentMetrics extends BusinessMetrics {

    // 支付请求计数器
    private Counter paymentRequestCounter;
    private Counter paymentSuccessCounter;
    private Counter paymentFailureCounter;
    
    // 支付回调计数器
    private Counter callbackReceivedCounter;
    private Counter callbackSuccessCounter;
    private Counter callbackFailureCounter;
    
    // 退款计数器
    private Counter refundRequestCounter;
    private Counter refundSuccessCounter;
    private Counter refundFailureCounter;
    
    // 对账计数器
    private Counter reconciliationSuccessCounter;
    private Counter reconciliationFailureCounter;
    
    // 支付响应时间
    private Timer paymentProcessTimer;
    
    // 支付金额统计（单位：分）
    private final AtomicLong totalPaymentAmount = new AtomicLong(0);
    private final AtomicLong totalRefundAmount = new AtomicLong(0);
    
    // 各渠道支付计数器
    private final ConcurrentHashMap<String, Counter> channelSuccessCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> channelFailureCounters = new ConcurrentHashMap<>();

    public PaymentMetrics(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }

    @Override
    protected void registerMetrics() {
        // 支付请求指标
        paymentRequestCounter = createCounter(
            "evcs.payment.request.total",
            "Total number of payment requests"
        );
        
        paymentSuccessCounter = createCounter(
            "evcs.payment.success.total",
            "Total number of successful payments"
        );
        
        paymentFailureCounter = createCounter(
            "evcs.payment.failure.total",
            "Total number of failed payments"
        );
        
        // 支付回调指标
        callbackReceivedCounter = createCounter(
            "evcs.payment.callback.received.total",
            "Total number of payment callbacks received"
        );
        
        callbackSuccessCounter = createCounter(
            "evcs.payment.callback.success.total",
            "Total number of successful payment callbacks"
        );
        
        callbackFailureCounter = createCounter(
            "evcs.payment.callback.failure.total",
            "Total number of failed payment callbacks"
        );
        
        // 退款指标
        refundRequestCounter = createCounter(
            "evcs.payment.refund.request.total",
            "Total number of refund requests"
        );
        
        refundSuccessCounter = createCounter(
            "evcs.payment.refund.success.total",
            "Total number of successful refunds"
        );
        
        refundFailureCounter = createCounter(
            "evcs.payment.refund.failure.total",
            "Total number of failed refunds"
        );
        
        // 对账指标
        reconciliationSuccessCounter = createCounter(
            "evcs.payment.reconciliation.success.total",
            "Total number of successful reconciliations"
        );
        
        reconciliationFailureCounter = createCounter(
            "evcs.payment.reconciliation.failure.total",
            "Total number of failed reconciliations"
        );
        
        // 支付处理时间
        paymentProcessTimer = createTimer(
            "evcs.payment.process.duration",
            "Payment processing duration in seconds"
        );
        
        // 支付金额统计
        createGauge(
            "evcs.payment.amount.total",
            "Total payment amount in cents",
            totalPaymentAmount
        );
        
        createGauge(
            "evcs.payment.refund.amount.total",
            "Total refund amount in cents",
            totalRefundAmount
        );
        
        log.info("Payment business metrics registered successfully");
    }

    /**
     * 记录支付请求
     */
    public void recordPaymentRequest() {
        incrementCounter(paymentRequestCounter);
    }

    /**
     * 记录支付成功
     * 
     * @param channel 支付渠道 (alipay, wechat)
     * @param amount 支付金额（分）
     */
    public void recordPaymentSuccess(String channel, Long amount) {
        incrementCounter(paymentSuccessCounter);
        incrementChannelSuccess(channel);
        if (amount != null) {
            totalPaymentAmount.addAndGet(amount);
        }
    }

    /**
     * 记录支付失败
     * 
     * @param channel 支付渠道 (alipay, wechat)
     */
    public void recordPaymentFailure(String channel) {
        incrementCounter(paymentFailureCounter);
        incrementChannelFailure(channel);
    }

    /**
     * 记录支付回调接收
     */
    public void recordCallbackReceived() {
        incrementCounter(callbackReceivedCounter);
    }

    /**
     * 记录支付回调成功
     */
    public void recordCallbackSuccess() {
        incrementCounter(callbackSuccessCounter);
    }

    /**
     * 记录支付回调失败
     */
    public void recordCallbackFailure() {
        incrementCounter(callbackFailureCounter);
    }

    /**
     * 记录退款请求
     */
    public void recordRefundRequest() {
        incrementCounter(refundRequestCounter);
    }

    /**
     * 记录退款成功
     * 
     * @param amount 退款金额（分）
     */
    public void recordRefundSuccess(Long amount) {
        incrementCounter(refundSuccessCounter);
        if (amount != null) {
            totalRefundAmount.addAndGet(amount);
        }
    }

    /**
     * 记录退款失败
     */
    public void recordRefundFailure() {
        incrementCounter(refundFailureCounter);
    }

    /**
     * 记录对账成功
     */
    public void recordReconciliationSuccess() {
        incrementCounter(reconciliationSuccessCounter);
    }

    /**
     * 记录对账失败
     */
    public void recordReconciliationFailure() {
        incrementCounter(reconciliationFailureCounter);
    }

    /**
     * 获取支付处理Timer用于记录执行时间
     * 
     * @return Timer
     */
    public Timer getPaymentProcessTimer() {
        return paymentProcessTimer;
    }

    /**
     * 增加渠道成功计数
     * 
     * @param channel 支付渠道
     */
    private void incrementChannelSuccess(String channel) {
        Counter counter = channelSuccessCounters.computeIfAbsent(channel, 
            ch -> createCounter(
                "evcs.payment.channel.success.total",
                "Successful payments by channel",
                "channel", ch
            )
        );
        incrementCounter(counter);
    }

    /**
     * 增加渠道失败计数
     * 
     * @param channel 支付渠道
     */
    private void incrementChannelFailure(String channel) {
        Counter counter = channelFailureCounters.computeIfAbsent(channel,
            ch -> createCounter(
                "evcs.payment.channel.failure.total",
                "Failed payments by channel",
                "channel", ch
            )
        );
        incrementCounter(counter);
    }

    /**
     * 计算支付成功率
     * 
     * @return 成功率百分比 (0-100)
     */
    public double getPaymentSuccessRate() {
        double total = paymentSuccessCounter.count() + paymentFailureCounter.count();
        if (total == 0) {
            return 100.0;
        }
        return (paymentSuccessCounter.count() / total) * 100.0;
    }

    /**
     * 计算指定渠道的支付成功率
     * 
     * @param channel 支付渠道
     * @return 成功率百分比 (0-100)
     */
    public double getChannelSuccessRate(String channel) {
        Counter successCounter = channelSuccessCounters.get(channel);
        Counter failureCounter = channelFailureCounters.get(channel);
        
        if (successCounter == null || failureCounter == null) {
            return 100.0;
        }
        
        double total = successCounter.count() + failureCounter.count();
        if (total == 0) {
            return 100.0;
        }
        return (successCounter.count() / total) * 100.0;
    }

    /**
     * 计算回调处理成功率
     * 
     * @return 成功率百分比 (0-100)
     */
    public double getCallbackSuccessRate() {
        double total = callbackSuccessCounter.count() + callbackFailureCounter.count();
        if (total == 0) {
            return 100.0;
        }
        return (callbackSuccessCounter.count() / total) * 100.0;
    }
}
