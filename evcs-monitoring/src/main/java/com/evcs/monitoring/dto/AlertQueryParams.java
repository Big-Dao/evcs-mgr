package com.evcs.monitoring.dto;

public record AlertQueryParams(
        Integer page,
        Integer size,
        String alertType,
        String severity,
        String status,
        String source,
        String startTime,
        String endTime
) {
}
