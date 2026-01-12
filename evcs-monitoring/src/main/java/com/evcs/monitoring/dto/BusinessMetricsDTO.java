package com.evcs.monitoring.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BusinessMetricsDTO {
    int activeOrders;
    int dailyOrders;
    int activeChargers;
    int onlineChargers;
    double dailyRevenue;
    int activeUsers;
    int dailyNewUsers;
}
