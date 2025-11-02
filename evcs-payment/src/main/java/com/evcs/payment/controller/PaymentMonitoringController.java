package com.evcs.payment.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.payment.dto.MetricsResponse;
import com.evcs.payment.service.metrics.PaymentMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付监控控制器
 *
 * 提供支付系统的监控指标和健康状态查询
 */
@Tag(name = "支付监控", description = "支付系统监控指标和健康状态")
@Slf4j
@RestController
@RequestMapping("/api/payment/monitoring")
@RequiredArgsConstructor
public class PaymentMonitoringController {

    private final PaymentMonitoringService monitoringService;

    @GetMapping("/overall")
    @Operation(summary = "获取总体监控指标")
    @DataScope
    public Result<MetricsResponse> getOverallMetrics() {
        log.info("获取总体监控指标");
        MetricsResponse metrics = monitoringService.getOverallMetrics();
        return Result.success(metrics);
    }

    @GetMapping("/channel/{channel}")
    @Operation(summary = "获取渠道监控指标")
    @DataScope
    public Result<MetricsResponse.ChannelMetrics> getChannelMetrics(
            @Parameter(description = "支付渠道") @PathVariable String channel) {
        log.info("获取渠道监控指标: channel={}", channel);
        MetricsResponse.ChannelMetrics metrics = monitoringService.getChannelMetrics(channel);
        return Result.success(metrics);
    }

    @GetMapping("/time-range")
    @Operation(summary = "获取时间段监控指标")
    @DataScope
    public Result<MetricsResponse.TimeRangeMetrics> getTimeRangeMetrics(
            @Parameter(description = "开始时间") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime) {
        log.info("获取时间段监控指标: startTime={}, endTime={}", startTime, endTime);
        MetricsResponse.TimeRangeMetrics metrics = monitoringService.getTimeRangeMetrics(startTime, endTime);
        return Result.success(metrics);
    }

    @GetMapping("/health")
    @Operation(summary = "获取系统健康状态")
    public Result<MetricsResponse.HealthStatus> getHealthStatus() {
        log.info("获取系统健康状态");
        MetricsResponse.HealthStatus health = monitoringService.getHealthStatus();
        return Result.success(health);
    }

    @GetMapping("/realtime")
    @Operation(summary = "获取实时统计数据")
    public Result<Map<String, Object>> getRealTimeStats() {
        log.info("获取实时统计数据");
        Map<String, Object> stats = monitoringService.getRealTimeStats();
        return Result.success(stats);
    }

    @GetMapping("/performance")
    @Operation(summary = "获取性能指标")
    public Result<Map<String, Object>> getPerformanceMetrics() {
        log.info("获取性能指标");
        Map<String, Object> metrics = monitoringService.getPerformanceMetrics();
        return Result.success(metrics);
    }

    @PostMapping("/custom-metric")
    @Operation(summary = "记录自定义指标")
    public Result<String> recordCustomMetric(
            @Parameter(description = "指标名称") @RequestParam String metricName,
            @Parameter(description = "指标值") @RequestParam double value,
            @Parameter(description = "标签") @RequestParam(required = false) Map<String, String> tags) {
        log.info("记录自定义指标: metricName={}, value={}, tags={}", metricName, value, tags);

        if (tags == null) {
            tags = new HashMap<>();
        }

        monitoringService.recordCustomMetric(metricName, value, tags);
        return Result.success("指标记录成功", "指标记录成功");
    }

    @PostMapping("/increment-counter")
    @Operation(summary = "增加计数器指标")
    public Result<String> incrementCounter(
            @Parameter(description = "指标名称") @RequestParam String metricName,
            @Parameter(description = "标签") @RequestParam(required = false) Map<String, String> tags) {
        log.info("增加计数器指标: metricName={}, tags={}", metricName, tags);

        if (tags == null) {
            tags = new HashMap<>();
        }

        monitoringService.incrementCounter(metricName, tags);
        return Result.success("计数器指标增加成功", "计数器指标增加成功");
    }

    @PostMapping("/record-timer")
    @Operation(summary = "记录时间指标")
    public Result<String> recordTimer(
            @Parameter(description = "指标名称") @RequestParam String metricName,
            @Parameter(description = "持续时间（毫秒）") @RequestParam long durationMs,
            @Parameter(description = "标签") @RequestParam(required = false) Map<String, String> tags) {
        log.info("记录时间指标: metricName={}, duration={}ms, tags={}", metricName, durationMs, tags);

        if (tags == null) {
            tags = new HashMap<>();
        }

        monitoringService.recordTimer(metricName, durationMs, tags);
        return Result.success("时间指标记录成功", "时间指标记录成功");
    }
}