package com.evcs.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 充电桩状态统计 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargerStatusStatsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "在线充电桩数量（非离线）")
    private Long online;

    @Schema(description = "离线充电桩数量（status=0）")
    private Long offline;

    @Schema(description = "充电中数量（status=2）")
    private Long charging;

    @Schema(description = "空闲数量（status=1）")
    private Long idle;
}

