package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DiskMetricsDTO {
    long total;
    long used;
    long free;
    double usagePercent;
}
