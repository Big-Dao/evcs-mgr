package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ThreadInfoDTO {
    int active;
    int peak;
    int daemon;
}
