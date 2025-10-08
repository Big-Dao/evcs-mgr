package com.evcs.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayParams {
    private String tradeId;
    private String payUrl;
    private BigDecimal amount;
    private String currency = "CNY";
    private LocalDateTime expireTime;
}
