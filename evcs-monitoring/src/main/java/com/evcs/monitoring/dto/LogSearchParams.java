package com.evcs.monitoring.dto;

public record LogSearchParams(
        Integer page,
        Integer size,
        String serviceName,
        String level,
        String keyword,
        String startTime,
        String endTime
) {
}
