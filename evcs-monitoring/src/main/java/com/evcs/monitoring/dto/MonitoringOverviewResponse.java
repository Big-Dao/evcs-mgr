package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MonitoringOverviewResponse {
    List<ServiceHealthDTO> services;
    SystemMetricsDTO metrics;
    BusinessMetricsDTO business;
    List<AlertDTO> alerts;
}
