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

    /**
     * Protocol服务注入已移除，避免Object类型导致的bean冲突
     * Week 9 TODO: 重新设计protocol依赖方式
     */

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
        // Protocol服务已临时禁用 (Week 1 Day 1)
        // 将在Week 9重新启用
        return Result.fail("协议调试功能暂时禁用，将在Week 9重新启用");
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
        // Protocol服务已临时禁用 (Week 1 Day 1)
        return Result.fail("协议调试功能暂时禁用，将在Week 9重新启用");
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
