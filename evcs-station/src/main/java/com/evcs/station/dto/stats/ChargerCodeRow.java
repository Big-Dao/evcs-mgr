package com.evcs.station.dto.stats;

/**
 * 站点/充电桩统计投影（内部 API 返回）。
 */
public record ChargerCodeRow(
        Long id, String chargerCode
) {
}
