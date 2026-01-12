package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NetworkMetricsDTO {
    long bytesIn;
    long bytesOut;
    long packetsIn;
    long packetsOut;
}
