package com.evcs.payment.service.metrics;

import com.evcs.payment.dto.MetricsResponse;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付监控服务接口
 *
 * 提供支付系统的实时监控和指标统计
 */
public interface PaymentMonitoringService {

    /**
     * 获取总体监控指标
     *
     * @return 监控指标响应
     */
    MetricsResponse getOverallMetrics();

    /**
     * 获取渠道监控指标
     *
     * @param channel 支付渠道
     * @return 渠道指标
     */
    MetricsResponse.ChannelMetrics getChannelMetrics(String channel);

    /**
     * 获取时间段监控指标
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间段指标
     */
    MetricsResponse.TimeRangeMetrics getTimeRangeMetrics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取系统健康状态
     *
     * @return 健康状态
     */
    MetricsResponse.HealthStatus getHealthStatus();

    /**
     * 获取实时统计
     *
     * @return 实时统计数据
     */
    Map<String, Object> getRealTimeStats();

    /**
     * 获取性能指标
     *
     * @return 性能指标数据
     */
    Map<String, Object> getPerformanceMetrics();

    /**
     * 记录自定义指标
     *
     * @param metricName 指标名称
     * @param value 指标值
     * @param tags 标签
     */
    void recordCustomMetric(String metricName, double value, Map<String, String> tags);

    /**
     * 增加计数器指标
     *
     * @param metricName 指标名称
     * @param tags 标签
     */
    void incrementCounter(String metricName, Map<String, String> tags);

    /**
     * 记录时间指标
     *
     * @param metricName 指标名称
     * @param durationMs 持续时间（毫秒）
     * @param tags 标签
     */
    void recordTimer(String metricName, long durationMs, Map<String, String> tags);
}