package com.evcs.monitoring.controller;

import com.evcs.common.result.Result;
import com.evcs.monitoring.dto.*;
import com.evcs.monitoring.service.MonitoringQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统监控", description = "系统监控聚合接口")
@RestController
@RequestMapping({"/monitoring", "/v1/monitoring"})
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringQueryService monitoringQueryService;

    @GetMapping("/overview")
    public Result<MonitoringOverviewResponse> getSystemOverview() {
        return Result.success(monitoringQueryService.getOverview());
    }

    @GetMapping("/versions")
    public Result<List<ServiceVersionDTO>> getServiceVersions() {
        return Result.success(monitoringQueryService.getServiceVersions());
    }

    @GetMapping("/services/health")
    public Result<List<ServiceHealthDTO>> getAllServicesHealth() {
        return Result.success(monitoringQueryService.getAllServicesHealth());
    }

    @GetMapping("/services/{serviceName}/health")
    public Result<ServiceHealthDTO> getServiceHealth(@PathVariable String serviceName) {
        return Result.success(monitoringQueryService.getServiceHealth(serviceName));
    }

    @GetMapping("/metrics/system")
    public Result<SystemMetricsDTO> getSystemMetrics() {
        return Result.success(monitoringQueryService.getSystemMetrics());
    }

    @GetMapping("/metrics/performance")
    public Result<List<PerformanceMetricsDTO>> getPerformanceMetrics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(monitoringQueryService.getPerformanceMetrics(startTime, endTime));
    }

    @GetMapping("/metrics/business")
    public Result<BusinessMetricsDTO> getBusinessMetrics() {
        return Result.success(monitoringQueryService.getBusinessMetrics());
    }

    @GetMapping("/alerts")
    public Result<PagedResponse<AlertDTO>> getAlertList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(monitoringQueryService.getAlerts(new AlertQueryParams(
                page,
                size,
                alertType,
                severity,
                status,
                source,
                startTime,
                endTime
        )));
    }

    @GetMapping("/alerts/{id}")
    public Result<AlertDTO> getAlertDetail(@PathVariable Long id) {
        return Result.success(monitoringQueryService.getAlertDetail(id));
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public Result<Void> acknowledgeAlert(@PathVariable Long id) {
        monitoringQueryService.acknowledgeAlert(id);
        return Result.success();
    }

    @PostMapping("/alerts/{id}/resolve")
    public Result<Void> resolveAlert(@PathVariable Long id, @RequestBody ResolveAlertRequest request) {
        monitoringQueryService.resolveAlert(id, request);
        return Result.success();
    }

    @GetMapping("/alerts/statistics")
    public Result<AlertStatisticsDTO> getAlertStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(monitoringQueryService.getAlertStatistics(startDate, endDate));
    }

    @GetMapping("/logs/realtime")
    public Result<List<String>> getRealTimeLogs(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer lines) {
        return Result.success(monitoringQueryService.getRealTimeLogs(serviceName, level, keyword, lines));
    }

    @GetMapping("/logs/search")
    public Result<PagedResponse<Object>> searchLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(monitoringQueryService.searchLogs(new LogSearchParams(
                page,
                size,
                serviceName,
                level,
                keyword,
                startTime,
                endTime
        )));
    }
}
