package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ServiceHealthDTO {
    String serviceName;
    String status;
    int instanceCount;
    int healthyInstances;
    int unhealthyInstances;
    long responseTime;
    String lastCheckTime;
    List<ServiceHealthDetailDTO> details;
}
