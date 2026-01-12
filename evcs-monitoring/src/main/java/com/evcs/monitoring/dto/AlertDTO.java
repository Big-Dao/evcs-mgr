package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AlertDTO {
    Long id;
    String alertType;
    String severity;
    String title;
    String message;
    String source;
    String status;
    String createTime;
    String updateTime;
    String resolvedTime;
}
