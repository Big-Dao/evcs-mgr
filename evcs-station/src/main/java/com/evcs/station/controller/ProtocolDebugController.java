package com.evcs.station.controller;

import com.evcs.common.result.Result;
import com.evcs.station.entity.Charger;
import com.evcs.station.mapper.ChargerMapper;
import com.evcs.station.service.IChargerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 协议调试控制器
 * 使用Optional依赖处理evcs-protocol模块，避免编译时依赖问题
 */
@Slf4j
@Tag(
    name = "协议调试",
    description = "用于本地联调，触发协议层心跳/状态/开始/停止"
)
@RestController("stationProtocolDebugController")
@RequestMapping("/debug/protocol")
public class ProtocolDebugController {

    @Autowired(required = false)
    private Object ocppService; // 实际类型: IOCPPProtocolService from evcs-protocol

    @Autowired(required = false)
    private Object cloudService; // 实际类型: ICloudChargeProtocolService from evcs-protocol

    @Autowired
    private ChargerMapper chargerMapper;

    @Autowired
    private IChargerService chargerService;

    private boolean useOcpp(Charger charger) {
        String protocols = charger.getSupportedProtocols();
        return protocols != null && protocols.toLowerCase().contains("ocpp");
    }

    @PostMapping("/{chargerId}/heartbeat")
    @Operation(summary = "触发心跳", description = "根据充电桩协议触发一次心跳")
    public Result<Void> heartbeat(
        @Parameter(
            description = "充电桩ID"
        ) @PathVariable @NotNull Long chargerId
    ) {
        if (ocppService == null && cloudService == null) {
            return Result.fail("协议服务未配置");
        }

        Charger charger = chargerMapper.selectById(chargerId);
        if (charger == null) {
            return Result.fail("充电桩不存在");
        }

        try {
            boolean ok;
            if (useOcpp(charger) && ocppService != null) {
                ok = (Boolean) ocppService
                    .getClass()
                    .getMethod("sendHeartbeat", Long.class)
                    .invoke(ocppService, chargerId);
            } else if (cloudService != null) {
                ok = (Boolean) cloudService
                    .getClass()
                    .getMethod("reportHeartbeat", Long.class)
                    .invoke(cloudService, chargerId);
            } else {
                return Result.fail("对应协议服务不可用");
            }
            return ok
                ? Result.success("心跳触发成功")
                : Result.fail("心跳触发失败");
        } catch (Exception e) {
            log.error("触发心跳失败", e);
            return Result.fail("触发心跳失败: " + e.getMessage());
        }
    }

    @PostMapping("/{chargerId}/status")
    @Operation(summary = "上报状态", description = "根据充电桩协议上报状态")
    public Result<Void> status(
        @Parameter(
            description = "充电桩ID"
        ) @PathVariable @NotNull Long chargerId,
        @Parameter(
            description = "状态：0离线，1空闲，2充电中，3故障等"
        ) @RequestParam Integer status
    ) {
        if (ocppService == null && cloudService == null) {
            return Result.fail("协议服务未配置");
        }

        Charger charger = chargerMapper.selectById(chargerId);
        if (charger == null) {
            return Result.fail("充电桩不存在");
        }

        try {
            boolean ok;
            if (useOcpp(charger) && ocppService != null) {
                ok = (Boolean) ocppService
                    .getClass()
                    .getMethod("updateStatus", Long.class, Integer.class)
                    .invoke(ocppService, chargerId, status);
            } else if (cloudService != null) {
                ok = (Boolean) cloudService
                    .getClass()
                    .getMethod("reportStatus", Long.class, Integer.class)
                    .invoke(cloudService, chargerId, status);
            } else {
                return Result.fail("对应协议服务不可用");
            }
            return ok
                ? Result.success("状态上报成功")
                : Result.fail("状态上报失败");
        } catch (Exception e) {
            log.error("上报状态失败", e);
            return Result.fail("上报状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/{chargerId}/start")
    @Operation(
        summary = "开始充电(服务链路)",
        description = "调用业务服务开始充电，内部联动协议"
    )
    public Result<Void> start(
        @Parameter(
            description = "充电桩ID"
        ) @PathVariable @NotNull Long chargerId,
        @Parameter(description = "会话ID") @RequestParam String sessionId,
        @Parameter(description = "用户ID") @RequestParam Long userId
    ) {
        boolean ok = chargerService.startChargingSession(
            chargerId,
            sessionId,
            userId
        );
        return ok
            ? Result.success("开始充电成功")
            : Result.fail("开始充电失败");
    }

    @PostMapping("/{chargerId}/stop")
    @Operation(
        summary = "停止充电(服务链路)",
        description = "调用业务服务停止充电，内部联动协议"
    )
    public Result<Void> stop(
        @Parameter(
            description = "充电桩ID"
        ) @PathVariable @NotNull Long chargerId,
        @Parameter(description = "充电量kWh") @RequestParam Double energy,
        @Parameter(description = "时长分钟") @RequestParam Long duration
    ) {
        boolean ok = chargerService.endChargingSession(
            chargerId,
            energy,
            duration
        );
        return ok
            ? Result.success("停止充电成功")
            : Result.fail("停止充电失败");
    }
}
