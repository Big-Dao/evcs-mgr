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
}
