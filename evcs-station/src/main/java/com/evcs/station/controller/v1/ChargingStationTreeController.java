package com.evcs.station.controller.v1;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.station.dto.ChargingStationTreeDTO;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v1 充电站聚合查询接口（管理端）
 */
@Tag(name = "充电站管理(v1)", description = "充电站聚合查询（树形结构）")
@RestController
@RequestMapping("/v1/charging-stations")
@RequiredArgsConstructor
@Validated
public class ChargingStationTreeController {

    private final IStationService stationService;

    @Operation(summary = "充电站-桩-枪树形列表", description = "返回充电站下的充电桩与枪口树形结构，用于管理端展示")
    @GetMapping("/tree")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope
    public Result<List<ChargingStationTreeDTO>> tree(Station queryParam) {
        return Result.success(stationService.listChargingStationTree(queryParam));
    }
}
