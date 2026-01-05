package com.evcs.payment.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReconciliationQuery {
    private String channel;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer page;
    private Integer size;
}
