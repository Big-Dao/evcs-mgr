package com.evcs.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 趋势点 DTO（日期-数值）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "日期，格式YYYY-MM-DD")
    private String date;

    @Schema(description = "数值")
    private BigDecimal value;
}

