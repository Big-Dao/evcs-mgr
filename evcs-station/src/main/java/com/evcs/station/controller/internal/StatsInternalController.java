package com.evcs.station.controller.internal;

import com.evcs.common.result.Result;
import com.evcs.station.dto.stats.ChargerCodeRow;
import com.evcs.station.dto.stats.ChargerStatusStats;
import com.evcs.station.dto.stats.StationBriefRow;
import com.evcs.station.dto.stats.StationNameRow;
import com.evcs.station.service.StationStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站点/充电桩统计内部查询端点（服务间调用）。
 */
@RestController
@RequestMapping("/internal/api/v1/stats")
@RequiredArgsConstructor
public class StatsInternalController {

    private final StationStatsService stationStatsService;

    @GetMapping("/stations/by-id/{stationId}")
    public Result<StationBriefRow> stationBrief(@org.springframework.web.bind.annotation.PathVariable("stationId") Long stationId) {
        return Result.success(stationStatsService.getStationBrief(stationId));
    }

    @GetMapping("/stations/names")
    public Result<List<StationNameRow>> stationNames(@RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(stationStatsService.getStationNames(tenantIds));
    }

    @GetMapping("/chargers/codes")
    public Result<List<ChargerCodeRow>> chargerCodes(@RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(stationStatsService.getChargerCodes(tenantIds));
    }

    @GetMapping("/chargers/status-stats")
    public Result<ChargerStatusStats> chargerStatusStats(@RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(stationStatsService.getChargerStatusStats(tenantIds));
    }
}
