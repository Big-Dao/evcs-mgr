package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceHealthDetailDTO {
    String instanceId;
    String host;
    int port;
    String status;
    long uptime;
    MemoryInfoDTO memory;
    ThreadInfoDTO threads;
}
