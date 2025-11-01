package com.evcs.payment.service.metrics.impl;

import com.evcs.payment.dto.MetricsResponse;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.metrics.PaymentMonitoringService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 支付监控服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMonitoringServiceImpl implements PaymentMonitoringService, HealthIndicator {

    private final PaymentMetrics paymentMetrics;
    private final MeterRegistry meterRegistry;

    @Value("${evcs.payment.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    // 缓存实时统计数据
    // private final Map<String, Object> realTimeStatsCache = new ConcurrentHashMap<>();
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());

    @Override
    public MetricsResponse getOverallMetrics() {
        log.debug("获取总体监控指标");

        return MetricsResponse.builder()
            .responseTime(LocalDateTime.now())
            .overall(buildOverallMetrics())
            .channels(buildChannelMetrics())
            .timeRanges(buildTimeRangeMetrics())
            .health(getHealthStatus())
            .build();
    }

    @Override
    public MetricsResponse.ChannelMetrics getChannelMetrics(String channel) {
        log.debug("获取渠道监控指标: channel={}", channel);

        // 从PaymentMetrics获取渠道数据
        double channelSuccessRate = paymentMetrics.getChannelSuccessRate(channel);

        return MetricsResponse.ChannelMetrics.builder()
            .channelName(channel)
            .totalRequests(getTotalRequestsForChannel(channel))
            .successCount(getSuccessCountForChannel(channel))
            .failureCount(getFailureCountForChannel(channel))
            .successRate(channelSuccessRate)
            .totalAmount(getTotalAmountForChannel(channel))
            .averageResponseTime(getAverageResponseTimeForChannel(channel))
            .lastUpdateTime(LocalDateTime.now())
            .build();
    }

    @Override
    public MetricsResponse.TimeRangeMetrics getTimeRangeMetrics(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("获取时间段监控指标: startTime={}, endTime={}", startTime, endTime);

        // 计算时间范围内的指标
        long hours = ChronoUnit.HOURS.between(startTime, endTime);

        return MetricsResponse.TimeRangeMetrics.builder()
            .timeRange(hours + "小时")
            .startTime(startTime)
            .endTime(endTime)
            .paymentCount(estimatePaymentCountInRange(hours))
            .paymentAmount(estimatePaymentAmountInRange(hours))
            .successRate(estimateSuccessRateInRange(hours))
            .averageResponseTime(estimateAverageResponseTimeInRange(hours))
            .build();
    }

    @Override
    public MetricsResponse.HealthStatus getHealthStatus() {
        log.debug("获取系统健康状态");

        try {
            // 检查各个组件的健康状态
            MetricsResponse.Status paymentHealth = checkPaymentSystemHealth();
            MetricsResponse.Status callbackHealth = checkCallbackSystemHealth();
            MetricsResponse.Status databaseHealth = checkDatabaseHealth();
            MetricsResponse.Status apiHealth = checkExternalApiHealth();

            // 确定总体健康状态
            MetricsResponse.Status overall = determineOverallHealth(paymentHealth, callbackHealth, databaseHealth, apiHealth);

            return MetricsResponse.HealthStatus.builder()
                .overall(overall)
                .paymentSystem(paymentHealth)
                .callbackSystem(callbackHealth)
                .database(databaseHealth)
                .externalApis(apiHealth)
                .lastCheckTime(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("获取健康状态失败", e);
            return MetricsResponse.HealthStatus.builder()
                .overall(MetricsResponse.Status.UNHEALTHY)
                .errorMessage(e.getMessage())
                .lastCheckTime(LocalDateTime.now())
                .build();
        }
    }

    @Override
    public Map<String, Object> getRealTimeStats() {
        // 更新缓存时间
        lastUpdateTime.set(System.currentTimeMillis());

        Map<String, Object> stats = new HashMap<>();
        stats.put("lastUpdateTime", LocalDateTime.now());
        stats.put("totalPaymentAmount", paymentMetrics.getTotalPaymentAmount());
        stats.put("paymentSuccessRate", paymentMetrics.getPaymentSuccessRate());
        stats.put("callbackSuccessRate", paymentMetrics.getCallbackSuccessRate());
        stats.put("averageResponseTime", paymentMetrics.getPaymentProcessTimer().mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        stats.put("activeConnections", getActiveConnections());
        stats.put("systemLoad", getSystemLoad());

        return stats;
    }

    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // JVM指标
        Runtime runtime = Runtime.getRuntime();
        metrics.put("jvm.memory.used", runtime.totalMemory() - runtime.freeMemory());
        metrics.put("jvm.memory.max", runtime.maxMemory());
        metrics.put("jvm.memory.usage", (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory());

        // 线程指标
        metrics.put("thread.count", Thread.activeCount());

        // GC指标
        metrics.put("gc.collections", getGarbageCollectionCount());

        // 支付系统指标
        metrics.put("payment.requests.per.minute", getRequestsPerMinute());
        metrics.put("payment.response.time.p95", getP95ResponseTime());
        metrics.put("payment.error.rate", getErrorRate());

        return metrics;
    }

    @Override
    public void recordCustomMetric(String metricName, double value, Map<String, String> tags) {
        if (!monitoringEnabled) {
            return;
        }

        try {
            // 使用Micrometer记录自定义指标
            // 转换Map为Tags
            String[] tagArray = tags.entrySet().stream()
                .flatMap(entry -> java.util.stream.Stream.of(entry.getKey(), entry.getValue()))
                .toArray(String[]::new);

            meterRegistry.gauge(metricName, Tags.of(tagArray), value);
            log.debug("记录自定义指标: {}={}, tags={}", metricName, value, tags);
        } catch (Exception e) {
            log.error("记录自定义指标失败: metricName={}", metricName, e);
        }
    }

    @Override
    public void incrementCounter(String metricName, Map<String, String> tags) {
        if (!monitoringEnabled) {
            return;
        }

        try {
            // 转换Map为Tags
            String[] tagArray = tags.entrySet().stream()
                .flatMap(entry -> java.util.stream.Stream.of(entry.getKey(), entry.getValue()))
                .toArray(String[]::new);

            meterRegistry.counter(metricName, Tags.of(tagArray)).increment();
            log.debug("增加计数器指标: {}, tags={}", metricName, tags);
        } catch (Exception e) {
            log.error("增加计数器指标失败: metricName={}", metricName, e);
        }
    }

    @Override
    public void recordTimer(String metricName, long durationMs, Map<String, String> tags) {
        if (!monitoringEnabled) {
            return;
        }

        try {
            // 转换Map为Tags
            String[] tagArray = tags.entrySet().stream()
                .flatMap(entry -> java.util.stream.Stream.of(entry.getKey(), entry.getValue()))
                .toArray(String[]::new);

            meterRegistry.timer(metricName, Tags.of(tagArray)).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            log.debug("记录时间指标: {}={}ms, tags={}", metricName, durationMs, tags);
        } catch (Exception e) {
            log.error("记录时间指标失败: metricName={}", metricName, e);
        }
    }

    @Override
    public Health health() {
        try {
            MetricsResponse.HealthStatus status = getHealthStatus();
            if (status.getOverall() == MetricsResponse.Status.HEALTHY) {
                return Health.up()
                    .withDetail("components", Map.of(
                        "paymentSystem", status.getPaymentSystem().getDescription(),
                        "callbackSystem", status.getCallbackSystem().getDescription(),
                        "database", status.getDatabase().getDescription(),
                        "externalApis", status.getExternalApis().getDescription()
                    ))
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", status.getOverall().getDescription())
                    .withDetail("error", status.getErrorMessage())
                    .build();
            }
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }

    // 私有辅助方法
    private MetricsResponse.OverallMetrics buildOverallMetrics() {
        return MetricsResponse.OverallMetrics.builder()
            .totalPaymentRequests(getTotalPaymentRequests())
            .paymentSuccessRate(paymentMetrics.getPaymentSuccessRate())
            .totalPaymentAmount(BigDecimal.valueOf(paymentMetrics.getTotalPaymentAmount().get()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
            .averageResponseTime(paymentMetrics.getPaymentProcessTimer().mean(java.util.concurrent.TimeUnit.MILLISECONDS))
            .todayPaymentCount(getTodayPaymentCount())
            .todayPaymentAmount(getTodayPaymentAmount())
            .callbackSuccessRate(paymentMetrics.getCallbackSuccessRate())
            .refundSuccessRate(getRefundSuccessRate())
            .reconciliationSuccessRate(getReconciliationSuccessRate())
            .build();
    }

    private Map<String, MetricsResponse.ChannelMetrics> buildChannelMetrics() {
        Map<String, MetricsResponse.ChannelMetrics> channels = new HashMap<>();

        // 支付宝渠道
        channels.put("alipay", getChannelMetrics("alipay"));

        // 微信支付渠道
        channels.put("wechat", getChannelMetrics("wechat"));

        return channels;
    }

    private Map<String, MetricsResponse.TimeRangeMetrics> buildTimeRangeMetrics() {
        Map<String, MetricsResponse.TimeRangeMetrics> timeRanges = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // 最近1小时
        timeRanges.put("1h", getTimeRangeMetrics(now.minusHours(1), now));

        // 最近24小时
        timeRanges.put("24h", getTimeRangeMetrics(now.minusDays(1), now));

        // 最近7天
        timeRanges.put("7d", getTimeRangeMetrics(now.minusDays(7), now));

        return timeRanges;
    }

    // 健康检查方法
    private MetricsResponse.Status checkPaymentSystemHealth() {
        // 检查支付成功率
        double successRate = paymentMetrics.getPaymentSuccessRate();
        if (successRate < 90.0) {
            return MetricsResponse.Status.WARNING;
        }
        if (successRate < 80.0) {
            return MetricsResponse.Status.UNHEALTHY;
        }
        return MetricsResponse.Status.HEALTHY;
    }

    private MetricsResponse.Status checkCallbackSystemHealth() {
        // 检查回调成功率
        double callbackRate = paymentMetrics.getCallbackSuccessRate();
        if (callbackRate < 95.0) {
            return MetricsResponse.Status.WARNING;
        }
        if (callbackRate < 85.0) {
            return MetricsResponse.Status.UNHEALTHY;
        }
        return MetricsResponse.Status.HEALTHY;
    }

    private MetricsResponse.Status checkDatabaseHealth() {
        try {
            // 简单的数据库健康检查
            // TODO: 实现真实的数据库连接检查
            return MetricsResponse.Status.HEALTHY;
        } catch (Exception e) {
            return MetricsResponse.Status.UNHEALTHY;
        }
    }

    private MetricsResponse.Status checkExternalApiHealth() {
        try {
            // 检查外部API连通性
            // TODO: 实现真实的API健康检查
            return MetricsResponse.Status.HEALTHY;
        } catch (Exception e) {
            return MetricsResponse.Status.WARNING;
        }
    }

    private MetricsResponse.Status determineOverallHealth(MetricsResponse.Status... statuses) {
        boolean hasWarning = false;

        for (MetricsResponse.Status status : statuses) {
            if (status == MetricsResponse.Status.UNHEALTHY) {
                return MetricsResponse.Status.UNHEALTHY;
            }
            if (status == MetricsResponse.Status.WARNING) {
                hasWarning = true;
            }
        }

        return hasWarning ? MetricsResponse.Status.WARNING : MetricsResponse.Status.HEALTHY;
    }

    // 模拟数据获取方法（实际项目中应从数据库或缓存获取）
    private Long getTotalPaymentRequests() { return 10000L; }
    private Long getTotalRequestsForChannel(String channel) { return 5000L; }
    private Long getSuccessCountForChannel(String channel) { return 4850L; }
    private Long getFailureCountForChannel(String channel) { return 150L; }
    private BigDecimal getTotalAmountForChannel(String channel) { return BigDecimal.valueOf(500000.00); }
    private Double getAverageResponseTimeForChannel(String channel) { return 150.0; }
    private Long estimatePaymentCountInRange(long hours) { return hours * 50L; }
    private BigDecimal estimatePaymentAmountInRange(long hours) { return BigDecimal.valueOf(hours * 1000.00); }
    private Double estimateSuccessRateInRange(long hours) { return 97.5; }
    private Double estimateAverageResponseTimeInRange(long hours) { return 120.0; }
    private Long getTodayPaymentCount() { return 1200L; }
    private BigDecimal getTodayPaymentAmount() { return BigDecimal.valueOf(120000.00); }
    private Double getRefundSuccessRate() { return 98.0; }
    private Double getReconciliationSuccessRate() { return 99.0; }
    private Integer getActiveConnections() { return 25; }
    private Double getSystemLoad() { return 0.65; }
    private Long getGarbageCollectionCount() { return 150L; }
    private Double getRequestsPerMinute() { return 45.0; }
    private Double getP95ResponseTime() { return 250.0; }
    private Double getErrorRate() { return 0.5; }
}