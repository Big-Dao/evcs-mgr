package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Map;

@Value
@Builder
public class AlertStatisticsDTO {
    long totalAlerts;
    long activeAlerts;
    long resolvedAlerts;
    long criticalAlerts;
    long highAlerts;
    long mediumAlerts;
    long lowAlerts;
    Map<String, Long> alertsByType;
    Map<String, Long> alertsBySeverity;

    public static AlertStatisticsDTO empty() {
        return AlertStatisticsDTO.builder()
                .totalAlerts(0)
                .activeAlerts(0)
                .resolvedAlerts(0)
                .criticalAlerts(0)
                .highAlerts(0)
                .mediumAlerts(0)
                .lowAlerts(0)
                .alertsByType(Collections.emptyMap())
                .alertsBySeverity(Collections.emptyMap())
                .build();
    }
}
