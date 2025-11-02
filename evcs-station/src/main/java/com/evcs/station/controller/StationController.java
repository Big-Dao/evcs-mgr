package com.evcs.station.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 充电站管理控制器
 */
@Slf4j
@Tag(name = "充电站管理", description = "充电站CRUD操作和查询")
@RestController
@RequestMapping("/station")
@RequiredArgsConstructor
@Validated
public class StationController {

    private final IStationService stationService;

    /**
     * 分页查询充电站列表
     */
    @Operation(summary = "分页查询充电站列表", description = "支持按名称、编码、状态、地区查询，返回分页结果")
    @GetMapping({"", "/", "/page", "/list"})
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:list')")
    @DataScope
    public Result<IPage<Station>> getStationPage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "查询条件") Station queryParam) {
        
        Page<Station> page = new Page<>(current, size);
        IPage<Station> result = stationService.queryStationPage(page, queryParam);
        
        return Result.success(result);
    }

    /**
     * 根据ID查询充电站详情
     */
    @Operation(summary = "查询充电站详情", description = "根据充电站ID查询详细信息，包含充电桩统计")
    @GetMapping("/{stationId}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Station> getStationById(
            @Parameter(description = "充电站ID") @PathVariable @NotNull Long stationId) {
        
        Station station = stationService.getStationDetail(stationId);
        if (station == null) {
            return Result.fail("充电站不存在");
        }
        
        return Result.success(station);
    }

    /**
     * 新增充电站
     */
    @Operation(summary = "新增充电站", description = "创建新的充电站")
    @PostMapping
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:add')")
    public Result<Void> addStation(@Parameter(description = "充电站信息") @RequestBody @Valid Station station) {
        try {
            boolean success = stationService.saveStation(station);
            if (success) {
                return Result.success("新增充电站成功");
            } else {
                return Result.fail("新增充电站失败");
            }
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 更新充电站信息
     */
    @Operation(summary = "更新充电站", description = "更新充电站信息")
    @PutMapping("/{stationId}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:edit')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> updateStation(
            @Parameter(description = "充电站ID") @PathVariable @NotNull Long stationId,
            @Parameter(description = "充电站信息") @RequestBody @Valid Station station) {
        if (station.getStationId() == null) {
            station.setStationId(stationId);
        } else if (!stationId.equals(station.getStationId())) {
            return Result.fail("路径ID与请求体中的充电站ID不一致");
        }
        
        try {
            boolean success = stationService.updateStation(station);
            if (success) {
                return Result.success("更新充电站成功");
            } else {
                return Result.fail("更新充电站失败");
            }
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 删除充电站
     */
    @Operation(summary = "删除充电站", description = "删除指定充电站（逻辑删除）")
    @DeleteMapping("/{stationId}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:remove')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> deleteStation(
            @Parameter(description = "充电站ID") @PathVariable @NotNull Long stationId) {
        
        try {
            boolean success = stationService.deleteStation(stationId);
            if (success) {
                return Result.success("删除充电站成功");
            } else {
                return Result.fail("删除充电站失败");
            }
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询附近充电站
     */
    @Operation(summary = "查询附近充电站", description = "根据经纬度查询附近的充电站")
    @GetMapping("/nearby")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope
    public Result<List<Station>> getNearbyStations(
            @Parameter(description = "纬度") @RequestParam Double latitude,
            @Parameter(description = "经度") @RequestParam Double longitude,
            @Parameter(description = "查询半径(公里)", example = "10") @RequestParam(defaultValue = "10") Double radius,
            @Parameter(description = "返回数量限制", example = "20") @RequestParam(defaultValue = "20") Integer limit) {
        
        List<Station> stations = stationService.getNearbyStations(latitude, longitude, radius, limit);
        return Result.success(stations);
    }

    /**
     * 启用/停用充电站
     */
    @Operation(summary = "启用停用充电站", description = "启用或停用指定充电站")
    @PutMapping("/{stationId}/status")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:edit')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> changeStationStatus(
            @Parameter(description = "充电站ID") @PathVariable @NotNull Long stationId,
            @Parameter(description = "状态：1启用，0停用") @RequestParam Integer status) {
        
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("状态值无效");
        }
        
        boolean success = stationService.changeStatus(stationId, status);
        if (success) {
            String action = status == 1 ? "启用" : "停用";
            return Result.success(action + "充电站成功");
        } else {
            return Result.fail("操作失败");
        }
    }

    /**
     * 检查充电站编码是否存在
     */
    @Operation(summary = "检查充电站编码", description = "检查充电站编码是否已存在")
    @GetMapping("/check-code")
    public Result<Boolean> checkStationCode(
            @Parameter(description = "充电站编码") @RequestParam String stationCode,
            @Parameter(description = "排除的充电站ID") @RequestParam(required = false) Long excludeId) {
        
        boolean exists = stationService.checkStationCodeExists(stationCode, excludeId);
        return Result.success(exists);
    }

    /**
     * 统计充电站数量
     */
    @Operation(summary = "统计充电站数量", description = "统计当前租户下的充电站数量")
    @GetMapping("/count")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    @DataScope
    public Result<Long> countStations() {
        Long count = stationService.count();
        return Result.success(count);
    }

    /**
     * 导入充电站数据
     */
    @Operation(summary = "导入充电站", description = "批量导入充电站数据")
    @PostMapping("/import")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:import')")
    public Result<Void> importStations(@Parameter(description = "充电站列表") @RequestBody @Valid List<Station> stations) {
        if (stations == null || stations.isEmpty()) {
            return Result.fail("导入数据不能为空");
        }
        
        try {
            boolean success = stationService.importStations(stations);
            if (success) {
                return Result.success("导入充电站成功");
            } else {
                return Result.fail("导入充电站失败");
            }
        } catch (Exception e) {
            log.error("导入充电站异常", e);
            return Result.fail("导入充电站异常：" + e.getMessage());
        }
    }

    /**
     * 导出充电站数据
     */
    @Operation(summary = "导出充电站", description = "导出充电站数据")
    @GetMapping("/export")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:export')")
    @DataScope
    public Result<List<Station>> exportStations(@Parameter(description = "查询条件") Station queryParam) {
        List<Station> stations = stationService.exportStations(queryParam);
        return Result.success(stations);
    }
}
