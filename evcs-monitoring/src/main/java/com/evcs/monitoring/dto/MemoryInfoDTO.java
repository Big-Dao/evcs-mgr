package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MemoryInfoDTO {
    long used;
    long max;
    double usagePercent;
}
