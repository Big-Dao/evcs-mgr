package com.evcs.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单时段分布 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodDistributionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "时段标签，如 0-3时")
    private String slot;

    @Schema(description = "订单数量")
    private Long count;
}

