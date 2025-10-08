package com.evcs.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IBillingService {
    BigDecimal calculateAmount(LocalDateTime startTime, LocalDateTime endTime, Double energyKwh, Long stationId, Long chargerId, Long planId);
}
