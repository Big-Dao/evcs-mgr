package com.evcs.order.dto;

import com.evcs.order.entity.ChargingOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDTO extends ChargingOrder {
    private String stationName;
    private String chargerCode;
    private String orderNo; // Maps to sessionId or generated order no

    // Frontend compatibility fields
    private Double chargingAmount; // Maps to energy
    private BigDecimal totalAmount; // Maps to amount

    public String getOrderNo() {
        if (orderNo != null) {
            return orderNo;
        }
        return getSessionId();
    }

    public Double getChargingAmount() {
        if (chargingAmount != null) {
            return chargingAmount;
        }
        return getEnergy();
    }

    public BigDecimal getTotalAmount() {
        if (totalAmount != null) {
            return totalAmount;
        }
        return getAmount();
    }
}
