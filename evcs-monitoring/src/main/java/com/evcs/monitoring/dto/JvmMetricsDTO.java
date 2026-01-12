package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JvmMetricsDTO {
    long heapUsed;
    long heapMax;
    double heapUsagePercent;
    long nonHeapUsed;
    int threadCount;
    long gcCount;
    long gcTime;
}
