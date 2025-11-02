package com.evcs.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付监控指标响应
 */
@Data
@Builder
public class MetricsResponse {

    /**
     * 响应时间
     */
    private LocalDateTime responseTime;

    /**
     * 总体统计
     */
    private OverallMetrics overall;

    /**
     * 渠道统计
     */
    private Map<String, ChannelMetrics> channels;

    /**
     * 时间段统计
     */
    private Map<String, TimeRangeMetrics> timeRanges;

    /**
     * 健康状态
     */
    private HealthStatus health;

    /**
     * 总体指标
     */
    @Data
    @Builder
    public static class OverallMetrics {
        /**
         * 总支付请求数
         */
        private Long totalPaymentRequests;

        /**
         * 支付成功率
         */
        private Double paymentSuccessRate;

        /**
         * 总支付金额
         */
        private BigDecimal totalPaymentAmount;

        /**
         * 平均支付响应时间（毫秒）
         */
        private Double averageResponseTime;

        /**
         * 今日支付数量
         */
        private Long todayPaymentCount;

        /**
         * 今日支付金额
         */
        private BigDecimal todayPaymentAmount;

        /**
         * 回调成功率
         */
        private Double callbackSuccessRate;

        /**
         * 退款成功率
         */
        private Double refundSuccessRate;

        /**
         * 对账成功率
         */
        private Double reconciliationSuccessRate;
    }

    /**
     * 渠道指标
     */
    @Data
    @Builder
    public static class ChannelMetrics {
        /**
         * 渠道名称
         */
        private String channelName;

        /**
         * 总请求数
         */
        private Long totalRequests;

        /**
         * 成功数
         */
        private Long successCount;

        /**
         * 失败数
         */
        private Long failureCount;

        /**
         * 成功率
         */
        private Double successRate;

        /**
         * 总金额
         */
        private BigDecimal totalAmount;

        /**
         * 平均响应时间
         */
        private Double averageResponseTime;

        /**
         * 最后更新时间
         */
        private LocalDateTime lastUpdateTime;
    }

    /**
     * 时间段指标
     */
    @Data
    @Builder
    public static class TimeRangeMetrics {
        /**
         * 时间段名称
         */
        private String timeRange;

        /**
         * 开始时间
         */
        private LocalDateTime startTime;

        /**
         * 结束时间
         */
        private LocalDateTime endTime;

        /**
         * 支付数量
         */
        private Long paymentCount;

        /**
         * 支付金额
         */
        private BigDecimal paymentAmount;

        /**
         * 成功率
         */
        private Double successRate;

        /**
         * 平均响应时间
         */
        private Double averageResponseTime;
    }

    /**
     * 健康状态
     */
    @Data
    @Builder
    public static class HealthStatus {
        /**
         * 总体健康状态
         */
        private Status overall;

        /**
         * 支付系统健康状态
         */
        private Status paymentSystem;

        /**
         * 回调系统健康状态
         */
        private Status callbackSystem;

        /**
         * 数据库连接健康状态
         */
        private Status database;

        /**
         * 外部API健康状态
         */
        private Status externalApis;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 最后检查时间
         */
        private LocalDateTime lastCheckTime;
    }

    /**
     * 健康状态枚举
     */
    public enum Status {
        HEALTHY("健康"),
        WARNING("警告"),
        UNHEALTHY("不健康");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}