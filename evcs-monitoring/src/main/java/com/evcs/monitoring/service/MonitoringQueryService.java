package com.evcs.monitoring.service;

import com.evcs.common.exception.BusinessException;
import com.evcs.common.result.ResultCode;
import com.evcs.monitoring.dto.AlertDTO;
import com.evcs.monitoring.dto.AlertQueryParams;
import com.evcs.monitoring.dto.AlertStatisticsDTO;
import com.evcs.monitoring.dto.BusinessMetricsDTO;
import com.evcs.monitoring.dto.CpuMetricsDTO;
import com.evcs.monitoring.dto.DiskMetricsDTO;
import com.evcs.monitoring.dto.JvmMetricsDTO;
import com.evcs.monitoring.dto.LogSearchParams;
import com.evcs.monitoring.dto.MemoryInfoDTO;
import com.evcs.monitoring.dto.MemoryMetricsDTO;
import com.evcs.monitoring.dto.MonitoringOverviewResponse;
import com.evcs.monitoring.dto.NetworkMetricsDTO;
import com.evcs.monitoring.dto.PagedResponse;
import com.evcs.monitoring.dto.PerformanceMetricsDTO;
import com.evcs.monitoring.dto.ResolveAlertRequest;
import com.evcs.monitoring.dto.ServiceHealthDTO;
import com.evcs.monitoring.dto.ServiceHealthDetailDTO;
import com.evcs.monitoring.dto.ServiceVersionDTO;
import com.evcs.monitoring.dto.SystemMetricsDTO;
import com.evcs.monitoring.dto.ThreadInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringQueryService {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate monitoringRestTemplate;

    public MonitoringOverviewResponse getOverview() {
        return MonitoringOverviewResponse.builder()
                .services(getAllServicesHealth())
                .metrics(getSystemMetrics())
                .business(getBusinessMetrics())
                .alerts(Collections.emptyList())
                .build();
    }

    public List<ServiceVersionDTO> getServiceVersions() {
        List<String> services = new ArrayList<>(discoveryClient.getServices());
        services.sort(Comparator.naturalOrder());

        List<ServiceVersionDTO> results = new ArrayList<>();
        for (String serviceName : services) {
            List<ServiceInstance> instances;
            try {
                instances = discoveryClient.getInstances(serviceName);
            } catch (Exception e) {
                log.warn("Discovery lookup failed (versions): serviceName={}", serviceName, e);
                instances = Collections.emptyList();
            }

            if (instances.isEmpty()) {
                results.add(ServiceVersionDTO.builder()
                        .serviceName(serviceName)
                        .reachable(false)
                        .error("NO_INSTANCE")
                        .build());
                continue;
            }

            for (ServiceInstance instance : instances) {
                results.add(probeInstanceVersion(serviceName, instance));
            }
        }

        return results;
    }

    public List<ServiceHealthDTO> getAllServicesHealth() {
        List<String> services = new ArrayList<>(discoveryClient.getServices());
        services.sort(Comparator.naturalOrder());

        List<ServiceHealthDTO> results = new ArrayList<>();
        for (String serviceName : services) {
            results.add(buildServiceHealth(serviceName));
        }
        return results;
    }

    public ServiceHealthDTO getServiceHealth(String serviceName) {
        return buildServiceHealth(serviceName);
    }

    public SystemMetricsDTO getSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = Math.max(0, totalMemory - freeMemory);
        long maxMemory = runtime.maxMemory();

        double heapUsagePercent = maxMemory > 0 ? (usedMemory * 100.0 / maxMemory) : 0.0;
        double memoryUsagePercent = totalMemory > 0 ? (usedMemory * 100.0 / totalMemory) : 0.0;

        return SystemMetricsDTO.builder()
                .cpu(CpuMetricsDTO.builder()
                        .usage(0)
                        .systemLoad(0)
                        .cores(runtime.availableProcessors())
                        .build())
                .memory(MemoryMetricsDTO.builder()
                        .total(totalMemory)
                        .used(usedMemory)
                        .free(freeMemory)
                        .usagePercent(memoryUsagePercent)
                        .build())
                .disk(DiskMetricsDTO.builder().total(0).used(0).free(0).usagePercent(0).build())
                .network(NetworkMetricsDTO.builder().bytesIn(0).bytesOut(0).packetsIn(0).packetsOut(0).build())
                .jvm(JvmMetricsDTO.builder()
                        .heapUsed(usedMemory)
                        .heapMax(maxMemory)
                        .heapUsagePercent(heapUsagePercent)
                        .nonHeapUsed(0)
                        .threadCount(Thread.activeCount())
                        .gcCount(0)
                        .gcTime(0)
                        .build())
                .build();
    }

    public List<PerformanceMetricsDTO> getPerformanceMetrics(String startTime, String endTime) {
        return Collections.emptyList();
    }

    public BusinessMetricsDTO getBusinessMetrics() {
        return BusinessMetricsDTO.builder()
                .activeOrders(0)
                .dailyOrders(0)
                .activeChargers(0)
                .onlineChargers(0)
                .dailyRevenue(0)
                .activeUsers(0)
                .dailyNewUsers(0)
                .build();
    }

    public PagedResponse<AlertDTO> getAlerts(AlertQueryParams params) {
        return PagedResponse.<AlertDTO>builder()
                .records(Collections.emptyList())
                .total(0)
                .build();
    }

    public AlertDTO getAlertDetail(Long id) {
        throw new BusinessException(ResultCode.NOT_FOUND, "告警不存在");
    }

    public void acknowledgeAlert(Long id) {
        // no-op
    }

    public void resolveAlert(Long id, ResolveAlertRequest request) {
        // no-op
    }

    public AlertStatisticsDTO getAlertStatistics(String startDate, String endDate) {
        return AlertStatisticsDTO.empty();
    }

    public List<String> getRealTimeLogs(String serviceName, String level, String keyword, Integer lines) {
        return Collections.emptyList();
    }

    public PagedResponse<Object> searchLogs(LogSearchParams params) {
        return PagedResponse.<Object>builder()
                .records(Collections.emptyList())
                .total(0)
                .build();
    }

    private ServiceHealthDTO buildServiceHealth(String serviceName) {
        List<ServiceInstance> instances;
        try {
            instances = discoveryClient.getInstances(serviceName);
        } catch (Exception e) {
            log.warn("Discovery lookup failed: serviceName={}", serviceName, e);
            instances = Collections.emptyList();
        }

        long start = System.nanoTime();
        int healthyInstances = 0;
        int unhealthyInstances = 0;
        List<ServiceHealthDetailDTO> details = new ArrayList<>();

        for (ServiceInstance instance : instances) {
            ServiceHealthDetailDTO detail = probeInstanceHealth(instance);
            details.add(detail);

            if ("UP".equalsIgnoreCase(detail.getStatus())) {
                healthyInstances++;
            } else {
                unhealthyInstances++;
            }
        }

        String status;
        if (instances.isEmpty()) {
            status = "UNKNOWN";
        } else if (unhealthyInstances == 0) {
            status = "UP";
        } else if (healthyInstances == 0) {
            status = "DOWN";
        } else {
            status = "UNKNOWN";
        }

        long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        return ServiceHealthDTO.builder()
                .serviceName(serviceName)
                .status(status)
                .instanceCount(instances.size())
                .healthyInstances(healthyInstances)
                .unhealthyInstances(unhealthyInstances)
                .responseTime(responseTimeMs)
                .lastCheckTime(OffsetDateTime.now().toString())
                .details(details)
                .build();
    }

    private ServiceVersionDTO probeInstanceVersion(String serviceName, ServiceInstance instance) {
        String instanceId = instance.getInstanceId();
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = instance.getHost() + ":" + instance.getPort();
        }

        String buildVersion = null;
        String buildTime = null;
        String gitCommit = null;
        String imageTag = null;
        String registry = null;

        boolean reachable = false;
        String error = null;

        try {
            String baseUrl = instance.getUri().toString();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String infoUrl = baseUrl + "/actuator/info";

            @SuppressWarnings("unchecked")
            Map<String, Object> body = monitoringRestTemplate.getForObject(infoUrl, Map.class);
            reachable = true;

            if (body != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> build = (Map<String, Object>) body.get("build");
                if (build != null) {
                    buildVersion = asString(build.get("version"));
                    buildTime = asString(build.get("time"));
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> git = (Map<String, Object>) body.get("git");
                if (git != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> commit = (Map<String, Object>) git.get("commit");
                    if (commit != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> id = (Map<String, Object>) commit.get("id");
                        if (id != null) {
                            gitCommit = asString(id.get("abbrev"));
                            if (gitCommit == null || gitCommit.isBlank()) {
                                gitCommit = asString(id.get("full"));
                            }
                        }
                        if (gitCommit == null || gitCommit.isBlank()) {
                            gitCommit = asString(commit.get("id"));
                        }
                    }
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> evcs = (Map<String, Object>) body.get("evcs");
                if (evcs != null) {
                    imageTag = asString(evcs.get("imageTag"));
                    registry = asString(evcs.get("registry"));
                }
            }
        } catch (Exception e) {
            reachable = false;
            error = e.getClass().getSimpleName();
            log.debug("Info probe failed: serviceName={}, instanceId={}, uri={}", serviceName, instanceId, instance.getUri(), e);
        }

        return ServiceVersionDTO.builder()
                .serviceName(serviceName)
                .instanceId(instanceId)
                .host(instance.getHost())
                .port(instance.getPort())
                .reachable(reachable)
                .error(error)
                .buildVersion(buildVersion)
                .buildTime(buildTime)
                .gitCommit(gitCommit)
                .imageTag(imageTag)
                .registry(registry)
                .build();
    }

    private String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private ServiceHealthDetailDTO probeInstanceHealth(ServiceInstance instance) {
        String instanceId = instance.getInstanceId();
        if (instanceId == null || instanceId.isBlank()) {
            instanceId = instance.getHost() + ":" + instance.getPort();
        }

        String status = "DOWN";
        try {
            String baseUrl = instance.getUri().toString();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String healthUrl = baseUrl + "/actuator/health";

            @SuppressWarnings("unchecked")
            Map<String, Object> body = monitoringRestTemplate.getForObject(healthUrl, Map.class);
            Object statusObj = body == null ? null : body.get("status");
            if (statusObj != null) {
                String raw = String.valueOf(statusObj);
                status = "UP".equalsIgnoreCase(raw) ? "UP" : "DOWN";
            } else {
                status = "UNKNOWN";
            }
        } catch (Exception e) {
            log.debug("Health probe failed: instanceId={}, uri={}", instanceId, instance.getUri(), e);
            status = "DOWN";
        }

        return ServiceHealthDetailDTO.builder()
                .instanceId(instanceId)
                .host(instance.getHost())
                .port(instance.getPort())
                .status(status)
                .uptime(0)
                .memory(MemoryInfoDTO.builder().used(0).max(0).usagePercent(0).build())
                .threads(ThreadInfoDTO.builder().active(0).peak(0).daemon(0).build())
                .build();
    }
}
