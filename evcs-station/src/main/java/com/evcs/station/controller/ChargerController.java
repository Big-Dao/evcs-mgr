package com.evcs.station.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import com.evcs.station.entity.Charger;
import com.evcs.station.service.IChargerService;
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
import java.util.Map;

/**
 * 充电桩管理控制器
 */
@Slf4j
@Tag(name = "充电桩管理", description = "充电桩CRUD操作和状态管理")
@RestController
@RequestMapping("/charger")
@RequiredArgsConstructor
@Validated
public class ChargerController {

    private final IChargerService chargerService;

    /**
     * 分页查询充电桩列表
     */
    @Operation(summary = "分页查询充电桩列表", description = "支持按名称、编码、状态、类型查询，返回分页结果")
    @GetMapping({"/page", "/list"})
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:list')")
    @DataScope
    public Result<IPage<Charger>> getChargerPage(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "查询条件") Charger queryParam) {
        
        Page<Charger> page = new Page<>(current, size);
        IPage<Charger> result = chargerService.queryChargerPage(page, queryParam);
        
        return Result.success(result);
    }

    /**
     * 根据ID查询充电桩详情
     */
    @Operation(summary = "查询充电桩详情", description = "根据充电桩ID查询详细信息")
    @GetMapping("/{chargerId:\\d+}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:query')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Charger> getChargerById(
            @Parameter(description = "充电桩ID") @PathVariable @NotNull Long chargerId) {
        
        Charger charger = chargerService.getById(chargerId);
        if (charger == null) {
            return Result.fail("充电桩不存在");
        }
        
        return Result.success(charger);
    }

    /**
     * 根据充电站ID查询充电桩列表
     */
    @Operation(summary = "查询充电站下的充电桩", description = "根据充电站ID查询所有充电桩")
    @GetMapping("/station/{stationId}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:query')")
    @DataScope
    public Result<List<Charger>> getChargersByStationId(
            @Parameter(description = "充电站ID") @PathVariable @NotNull Long stationId) {
        
        List<Charger> chargers = chargerService.getChargersByStationId(stationId);
        return Result.success(chargers);
    }

    /**
     * 新增充电桩
     */
    @Operation(summary = "新增充电桩", description = "创建新的充电桩")
    @PostMapping
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:add')")
    public Result<Void> addCharger(@Parameter(description = "充电桩信息") @RequestBody @Valid Charger charger) {
        try {
            boolean success = chargerService.saveCharger(charger);
            if (success) {
                return Result.success("新增充电桩成功");
            } else {
                return Result.fail("新增充电桩失败");
            }
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 更新充电桩信息
     */
    @Operation(summary = "更新充电桩", description = "更新充电桩信息")
    @PutMapping
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:edit')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> updateCharger(@Parameter(description = "充电桩信息") @RequestBody @Valid Charger charger) {
        if (charger.getId() == null) {
            return Result.fail("充电桩ID不能为空");
        }
        
        try {
            boolean success = chargerService.updateCharger(charger);
            if (success) {
                return Result.success("更新充电桩成功");
            } else {
                return Result.fail("更新充电桩失败");
            }
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 删除充电桩
     */
    @Operation(summary = "删除充电桩", description = "删除指定充电桩（逻辑删除）")
    @DeleteMapping("/{chargerId}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:remove')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> deleteCharger(
            @Parameter(description = "充电桩ID") @PathVariable @NotNull Long chargerId) {
        
        try {
            boolean success = chargerService.deleteCharger(chargerId);
            if (success) {
                return Result.success("删除充电桩成功");
            } else {
                return Result.fail("删除充电桩失败");
            }
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 更新充电桩状态
     */
    @Operation(summary = "更新充电桩状态", description = "更新充电桩运行状态")
    @PutMapping("/{chargerId}/status")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:edit')")
    public Result<Void> updateChargerStatus(
            @Parameter(description = "充电桩ID") @PathVariable @NotNull Long chargerId,
            @Parameter(description = "状态：0离线，1空闲，2充电中，3故障，4维护，5预约中") @RequestParam Integer status) {
        
        boolean success = chargerService.updateStatus(chargerId, status);
        if (success) {
            return Result.success("更新状态成功");
        } else {
            return Result.fail("更新状态失败");
        }
    }

    /**
     * 更新充电桩实时数据
     */
    @Operation(summary = "更新实时数据", description = "更新充电桩实时运行数据")
    @PutMapping("/{chargerId}/realtime")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:edit')")
    public Result<Void> updateRealTimeData(
            @Parameter(description = "充电桩ID") @PathVariable @NotNull Long chargerId,
            @Parameter(description = "实时功率(kW)") @RequestParam(required = false) Double power,
            @Parameter(description = "实时电压(V)") @RequestParam(required = false) Double voltage,
            @Parameter(description = "实时电流(A)") @RequestParam(required = false) Double current,
            @Parameter(description = "温度(℃)") @RequestParam(required = false) Double temperature) {
        
        boolean success = chargerService.updateRealTimeData(chargerId, power, voltage, current, temperature);
        if (success) {
            return Result.success("更新实时数据成功");
        } else {
            return Result.fail("更新实时数据失败");
        }
    }

    /**
     * 开始充电会话
     */
    @Operation(summary = "开始充电", description = "开始充电会话")
    @PostMapping("/{chargerId}/start")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:charge')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> startCharging(
            @Parameter(description = "充电桩ID") @PathVariable @NotNull Long chargerId,
            @Parameter(description = "会话ID") @RequestParam String sessionId,
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        
        try {
            boolean success = chargerService.startChargingSession(chargerId, sessionId, userId);
            if (success) {
                return Result.success("开始充电成功");
            } else {
                return Result.fail("开始充电失败");
            }
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 结束充电会话
     */
    @Operation(summary = "结束充电", description = "结束充电会话")
    @PostMapping("/{chargerId}/stop")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:charge')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> stopCharging(
            @Parameter(description = "充电桩ID") @PathVariable @NotNull Long chargerId,
            @Parameter(description = "充电量(kWh)") @RequestParam Double energy,
            @Parameter(description = "充电时长(分钟)") @RequestParam Long duration) {
        
        boolean success = chargerService.endChargingSession(chargerId, energy, duration);
        if (success) {
            return Result.success("结束充电成功");
        } else {
            return Result.fail("结束充电失败");
        }
    }

    /**
     * 重置充电桩
     */
    @Operation(summary = "重置充电桩", description = "重置充电桩状态，清除当前会话")
    @PostMapping("/{chargerId}/reset")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:edit')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> resetCharger(
            @Parameter(description = "充电桩ID") @PathVariable @NotNull Long chargerId) {
        
        boolean success = chargerService.resetCharger(chargerId);
        if (success) {
            return Result.success("重置充电桩成功");
        } else {
            return Result.fail("重置充电桩失败");
        }
    }

    /**
     * 启用/停用充电桩
     */
    @Operation(summary = "启用停用充电桩", description = "启用或停用指定充电桩")
    @PutMapping("/{chargerId}/enable")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:edit')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> changeChargerStatus(
            @Parameter(description = "充电桩ID") @PathVariable @NotNull Long chargerId,
            @Parameter(description = "状态：1启用，0停用") @RequestParam Integer enabled) {
        
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            return Result.fail("状态值无效");
        }
        
        boolean success = chargerService.changeStatus(chargerId, enabled);
        if (success) {
            String action = enabled == 1 ? "启用" : "停用";
            return Result.success(action + "充电桩成功");
        } else {
            return Result.fail("操作失败");
        }
    }

    /**
     * 查询离线充电桩
     */
    @Operation(summary = "查询离线充电桩", description = "查询指定时间内未上报心跳的充电桩")
    @GetMapping("/offline")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:query')")
    @DataScope
    public Result<List<Charger>> getOfflineChargers(
            @Parameter(description = "离线时间阈值(分钟)", example = "5") @RequestParam(defaultValue = "5") Integer minutes) {
        
        List<Charger> chargers = chargerService.getOfflineChargers(minutes);
        return Result.success(chargers);
    }

    /**
     * 查询故障充电桩
     */
    @Operation(summary = "查询故障充电桩", description = "查询当前故障状态的充电桩")
    @GetMapping("/fault")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:query')")
    @DataScope
    public Result<List<Charger>> getFaultChargers() {
        List<Charger> chargers = chargerService.getFaultChargers();
        return Result.success(chargers);
    }

    /**
     * 统计充电桩状态
     */
    @Operation(summary = "统计充电桩状态", description = "统计各状态充电桩数量")
    @GetMapping("/statistics")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:query')")
    @DataScope
    public Result<Map<Integer, Long>> getStatusStatistics() {
        Map<Integer, Long> statistics = chargerService.getStatusStatistics(null);
        return Result.success(statistics);
    }

    /**
     * 根据协议查询充电桩
     */
    @Operation(summary = "按协议查询充电桩", description = "根据充电协议类型查询充电桩")
    @GetMapping("/protocol/{protocol}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:query')")
    @DataScope
    public Result<List<Charger>> getChargersByProtocol(
            @Parameter(description = "协议类型", example = "ocpp") @PathVariable String protocol) {
        
        List<Charger> chargers = chargerService.getChargersByProtocol(protocol);
        return Result.success(chargers);
    }

    /**
     * 检查充电桩编码是否存在
     */
    @Operation(summary = "检查充电桩编码", description = "检查充电桩编码是否已存在")
    @GetMapping("/check-code")
    public Result<Boolean> checkChargerCode(
            @Parameter(description = "充电桩编码") @RequestParam String chargerCode,
            @Parameter(description = "排除的充电桩ID") @RequestParam(required = false) Long excludeId) {
        
        boolean exists = chargerService.checkChargerCodeExists(chargerCode, excludeId);
        return Result.success(exists);
    }

    /**
     * 批量更新充电桩状态
     */
    @Operation(summary = "批量更新状态", description = "批量更新多个充电桩的状态")
    @PutMapping("/batch-status")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'charger:edit')")
    @DataScope(value = DataScope.DataScopeType.USER)
    public Result<Void> batchUpdateStatus(
            @Parameter(description = "充电桩ID列表") @RequestBody List<Long> chargerIds,
            @Parameter(description = "目标状态") @RequestParam Integer status) {
        
        if (chargerIds == null || chargerIds.isEmpty()) {
            return Result.fail("充电桩ID列表不能为空");
        }
        
        boolean success = chargerService.batchUpdateStatus(chargerIds, status);
        if (success) {
            return Result.success("批量更新状态成功");
        } else {
            return Result.fail("批量更新状态失败");
        }
    }
}
