package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CpuMetricsDTO {
    double usage;
    double systemLoad;
    int cores;
}
