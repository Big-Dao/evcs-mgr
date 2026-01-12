package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerformanceMetricsDTO {
    String endpoint;
    String method;
    long avgResponseTime;
    long minResponseTime;
    long maxResponseTime;
    long p50ResponseTime;
    long p90ResponseTime;
    long p99ResponseTime;
    long requestCount;
    long successCount;
    long errorCount;
    double successRate;
    double qps;
}
