package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SystemMetricsDTO {
    CpuMetricsDTO cpu;
    MemoryMetricsDTO memory;
    DiskMetricsDTO disk;
    NetworkMetricsDTO network;
    JvmMetricsDTO jvm;
}
