package com.evcs.station.dto.stats;

/**
 * 站点简要信息（内部 API 返回，供 order 写单时冗余展示字段）。
 */
public record StationBriefRow(
        Long stationId,
        String stationName,
        String province,
        String city
) {
}
