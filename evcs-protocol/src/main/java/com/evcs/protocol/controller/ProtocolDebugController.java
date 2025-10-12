package com.evcs.protocol.controller;

import com.evcs.common.core.Result;
import com.evcs.protocol.event.*;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 协议调试控制器
 * 提供协议事件模拟和查看功能，方便开发和测试
 */
@Slf4j
@RestController
@RequestMapping("/debug/protocol")
@RequiredArgsConstructor
public class ProtocolDebugController {
    
    private final ProtocolEventPublisher eventPublisher;
    
    // 用于存储最近的事件历史（生产环境应使用数据库或Redis）
    private final List<ProtocolEvent> eventHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * 模拟发送心跳事件
     */
    @PostMapping("/simulate/heartbeat")
    public Result<?> simulateHeartbeat(@RequestBody HeartbeatRequest request) {
        log.info("Simulating heartbeat: chargerId={}, protocol={}", 
                request.getChargerId(), request.getProtocolType());
        
        try {
            LocalDateTime now = LocalDateTime.now();
            eventPublisher.publishHeartbeat(
                    request.getChargerId(),
                    request.getTenantId(),
                    request.getProtocolType(),
                    now
            );
            
            return Result.success("Heartbeat event sent successfully");
        } catch (Exception e) {
            log.error("Failed to simulate heartbeat", e);
            return Result.error("Failed to simulate heartbeat: " + e.getMessage());
        }
    }
    
    /**
     * 模拟发送状态变更事件
     */
    @PostMapping("/simulate/status")
    public Result<?> simulateStatusChange(@RequestBody StatusChangeRequest request) {
        log.info("Simulating status change: chargerId={}, oldStatus={}, newStatus={}", 
                request.getChargerId(), request.getOldStatus(), request.getNewStatus());
        
        try {
            eventPublisher.publishStatusChange(
                    request.getChargerId(),
                    request.getTenantId(),
                    request.getProtocolType(),
                    request.getOldStatus(),
                    request.getNewStatus(),
                    request.getStatusDesc()
            );
            
            return Result.success("Status change event sent successfully");
        } catch (Exception e) {
            log.error("Failed to simulate status change", e);
            return Result.error("Failed to simulate status change: " + e.getMessage());
        }
    }
    
    /**
     * 模拟发送开始充电事件
     */
    @PostMapping("/simulate/start")
    public Result<?> simulateChargingStart(@RequestBody ChargingStartRequest request) {
        log.info("Simulating charging start: chargerId={}, userId={}", 
                request.getChargerId(), request.getUserId());
        
        try {
            String sessionId = UUID.randomUUID().toString();
            String orderNo = "ORD" + System.currentTimeMillis();
            
            eventPublisher.publishChargingStart(
                    request.getChargerId(),
                    request.getTenantId(),
                    request.getProtocolType(),
                    sessionId,
                    request.getUserId(),
                    orderNo,
                    request.getInitialEnergy(),
                    true,
                    "Charging started"
            );
            
            return Result.success("Charging start event sent successfully", 
                    new SimulateResponse(sessionId, orderNo));
        } catch (Exception e) {
            log.error("Failed to simulate charging start", e);
            return Result.error("Failed to simulate charging start: " + e.getMessage());
        }
    }
    
    /**
     * 模拟发送停止充电事件
     */
    @PostMapping("/simulate/stop")
    public Result<?> simulateChargingStop(@RequestBody ChargingStopRequest request) {
        log.info("Simulating charging stop: chargerId={}, sessionId={}", 
                request.getChargerId(), request.getSessionId());
        
        try {
            eventPublisher.publishChargingStop(
                    request.getChargerId(),
                    request.getTenantId(),
                    request.getProtocolType(),
                    request.getSessionId(),
                    request.getOrderNo(),
                    request.getEnergy(),
                    request.getDuration(),
                    request.getReason(),
                    true,
                    "Charging stopped"
            );
            
            return Result.success("Charging stop event sent successfully");
        } catch (Exception e) {
            log.error("Failed to simulate charging stop", e);
            return Result.error("Failed to simulate charging stop: " + e.getMessage());
        }
    }
    
    /**
     * 查看事件历史
     * 注意：这只是演示，生产环境应该从数据库或消息队列中查询
     */
    @GetMapping("/events/history")
    public Result<?> getEventHistory(
            @RequestParam(required = false) Long chargerId,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Querying event history: chargerId={}, eventType={}, limit={}", 
                chargerId, eventType, limit);
        
        List<ProtocolEvent> filteredEvents = new ArrayList<>(eventHistory);
        
        // 简单过滤（生产环境应使用数据库查询）
        if (chargerId != null) {
            filteredEvents.removeIf(event -> !event.getChargerId().equals(chargerId));
        }
        if (eventType != null) {
            filteredEvents.removeIf(event -> !event.getEventType().toString().equals(eventType));
        }
        
        // 限制数量
        if (filteredEvents.size() > limit) {
            filteredEvents = filteredEvents.subList(
                    filteredEvents.size() - limit, 
                    filteredEvents.size()
            );
        }
        
        return Result.success(filteredEvents);
    }
    
    /**
     * 获取协议统计信息
     */
    @GetMapping("/stats")
    public Result<?> getProtocolStats() {
        // TODO: 实现真实的统计逻辑
        ProtocolStats stats = new ProtocolStats();
        stats.setTotalEvents(eventHistory.size());
        stats.setOcppEvents(eventHistory.stream()
                .filter(e -> "OCPP".equals(e.getProtocolType()))
                .count());
        stats.setCloudChargeEvents(eventHistory.stream()
                .filter(e -> "CloudCharge".equals(e.getProtocolType()))
                .count());
        
        return Result.success(stats);
    }
    
    // ===== Request DTOs =====
    
    @Data
    public static class HeartbeatRequest {
        private Long chargerId;
        private Long tenantId = 1L;
        private String protocolType = "OCPP";
    }
    
    @Data
    public static class StatusChangeRequest {
        private Long chargerId;
        private Long tenantId = 1L;
        private String protocolType = "OCPP";
        private Integer oldStatus;
        private Integer newStatus;
        private String statusDesc;
    }
    
    @Data
    public static class ChargingStartRequest {
        private Long chargerId;
        private Long tenantId = 1L;
        private Long userId;
        private String protocolType = "OCPP";
        private Double initialEnergy = 0.0;
    }
    
    @Data
    public static class ChargingStopRequest {
        private Long chargerId;
        private Long tenantId = 1L;
        private String protocolType = "OCPP";
        private String sessionId;
        private String orderNo;
        private Double energy;
        private Long duration;
        private String reason = "Normal stop";
    }
    
    @Data
    public static class SimulateResponse {
        private String sessionId;
        private String orderNo;
        
        public SimulateResponse(String sessionId, String orderNo) {
            this.sessionId = sessionId;
            this.orderNo = orderNo;
        }
    }
    
    @Data
    public static class ProtocolStats {
        private long totalEvents;
        private long ocppEvents;
        private long cloudChargeEvents;
    }
}
