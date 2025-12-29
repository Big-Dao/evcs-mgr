package com.evcs.protocol.controller;

import com.evcs.common.result.Result;
import com.evcs.protocol.api.IProtocolService;
import com.evcs.protocol.api.ProtocolManager;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 协议指令控制控制器
 * 提供远程启停等指令下发接口
 */
@Slf4j
@Tag(name = "协议指令控制", description = "提供远程启停等指令下发接口")
@RestController
@RequestMapping("/protocol/command")
@RequiredArgsConstructor
public class ProtocolCommandController {

    private final ProtocolManager protocolManager;

    @Operation(summary = "远程启动充电", description = "下发远程启动指令到充电桩")
    @PostMapping("/start")
    public Result<ProtocolResponse> startCharging(@RequestBody ProtocolRequest request) {
        log.info("Received remote start command: deviceCode={}, sessionId={}", request.getDeviceCode(), request.getSessionId());
        
        if (request.getProtocolType() == null) {
             request.setProtocolType(protocolManager.getProtocolType(request.getDeviceCode()));
        }
        
        try {
             IProtocolService service = protocolManager.getProtocolService(request.getProtocolType());
             ProtocolResponse response = service.startCharging(request);
             if (response.isSuccess()) {
                 return Result.success(response);
             } else {
                 return Result.fail(response.getMessage());
             }
        } catch (Exception e) {
            log.error("Failed to execute start command", e);
            return Result.fail("Failed to execute start command: " + e.getMessage());
        }
    }

    @Operation(summary = "远程停止充电", description = "下发远程停止指令到充电桩")
    @PostMapping("/stop")
    public Result<ProtocolResponse> stopCharging(@RequestBody ProtocolRequest request) {
        log.info("Received remote stop command: deviceCode={}, sessionId={}", request.getDeviceCode(), request.getSessionId());
        
        if (request.getProtocolType() == null) {
             request.setProtocolType(protocolManager.getProtocolType(request.getDeviceCode()));
        }
        
        try {
             IProtocolService service = protocolManager.getProtocolService(request.getProtocolType());
             ProtocolResponse response = service.stopCharging(request);
             if (response.isSuccess()) {
                 return Result.success(response);
             } else {
                 return Result.fail(response.getMessage());
             }
        } catch (Exception e) {
            log.error("Failed to execute stop command", e);
            return Result.fail("Failed to execute stop command: " + e.getMessage());
        }
    }

    @Operation(summary = "预约充电", description = "下发预约指令到充电桩")
    @PostMapping("/reserve")
    public Result<ProtocolResponse> reserveNow(@RequestBody ProtocolRequest request) {
        log.info("Received reserve command: deviceCode={}", request.getDeviceCode());
        
        if (request.getProtocolType() == null) {
             request.setProtocolType(protocolManager.getProtocolType(request.getDeviceCode()));
        }
        
        try {
             IProtocolService service = protocolManager.getProtocolService(request.getProtocolType());
             ProtocolResponse response = service.reserveNow(request);
             if (response.isSuccess()) {
                 return Result.success(response);
             } else {
                 return Result.fail(response.getMessage());
             }
        } catch (Exception e) {
            log.error("Failed to execute reserve command", e);
            return Result.fail("Failed to execute reserve command: " + e.getMessage());
        }
    }

    @Operation(summary = "取消预约", description = "下发取消预约指令到充电桩")
    @PostMapping("/cancel-reservation")
    public Result<ProtocolResponse> cancelReservation(@RequestBody ProtocolRequest request) {
        log.info("Received cancel reservation command: deviceCode={}", request.getDeviceCode());

        if (request.getProtocolType() == null) {
             request.setProtocolType(protocolManager.getProtocolType(request.getDeviceCode()));
        }

        try {
             IProtocolService service = protocolManager.getProtocolService(request.getProtocolType());
             ProtocolResponse response = service.cancelReservation(request);
             if (response.isSuccess()) {
                 return Result.success(response);
             } else {
                 return Result.fail(response.getMessage());
             }
        } catch (Exception e) {
            log.error("Failed to execute cancel reservation command", e);
            return Result.fail("Failed to execute cancel reservation command: " + e.getMessage());
        }
    }

    @Operation(summary = "固件升级", description = "下发固件升级指令到充电桩")
    @PostMapping("/update-firmware")
    public Result<ProtocolResponse> updateFirmware(@RequestBody ProtocolRequest request) {
        log.info("Received update firmware command: deviceCode={}", request.getDeviceCode());

        if (request.getProtocolType() == null) {
             request.setProtocolType(protocolManager.getProtocolType(request.getDeviceCode()));
        }

        try {
             IProtocolService service = protocolManager.getProtocolService(request.getProtocolType());
             ProtocolResponse response = service.updateFirmware(request);
             if (response.isSuccess()) {
                 return Result.success(response);
             } else {
                 return Result.fail(response.getMessage());
             }
        } catch (Exception e) {
            log.error("Failed to execute update firmware command", e);
            return Result.fail("Failed to execute update firmware command: " + e.getMessage());
        }
    }

    @Operation(summary = "设置充电策略", description = "下发充电策略到充电桩")
    @PostMapping("/set-charging-profile")
    public Result<ProtocolResponse> setChargingProfile(@RequestBody ProtocolRequest request) {
        log.info("Received set charging profile command: deviceCode={}", request.getDeviceCode());

        if (request.getProtocolType() == null) {
             request.setProtocolType(protocolManager.getProtocolType(request.getDeviceCode()));
        }

        try {
             IProtocolService service = protocolManager.getProtocolService(request.getProtocolType());
             ProtocolResponse response = service.setChargingProfile(request);
             if (response.isSuccess()) {
                 return Result.success(response);
             } else {
                 return Result.fail(response.getMessage());
             }
        } catch (Exception e) {
            log.error("Failed to execute set charging profile command", e);
            return Result.fail("Failed to execute set charging profile command: " + e.getMessage());
        }
    }
}
