package com.evcs.protocol.controller;

import com.evcs.common.result.Result;
import com.evcs.protocol.event.*;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 协议调试控制器
 * 提供协议事件模拟和查看功能，方便开发和测试
 */
@RestController("protocolEventDebugController")
@RequestMapping("/debug/protocol")
public class ProtocolDebugController {

    private static final Logger log = LoggerFactory.getLogger(
        ProtocolDebugController.class
    );

    private final ProtocolEventPublisher eventPublisher;

    @Autowired
    public ProtocolDebugController(ProtocolEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 模拟发送心跳事件
     */
    @PostMapping("/simulate/heartbeat")
    public Result<?> simulateHeartbeat(@RequestBody HeartbeatRequest request) {
        log.info(
            "Simulating heartbeat: chargerId={}, protocol={}",
            request.getChargerId(),
            request.getProtocolType()
        );

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
            return Result.failure(
                "Failed to simulate heartbeat: " + e.getMessage()
            );
        }
    }

    /**
     * 模拟发送状态变更事件
     */
    @PostMapping("/simulate/status")
    public Result<?> simulateStatusChange(
        @RequestBody StatusChangeRequest request
    ) {
        log.info(
            "Simulating status change: chargerId={}, oldStatus={}, newStatus={}",
            request.getChargerId(),
            request.getOldStatus(),
            request.getNewStatus()
        );

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
            return Result.failure(
                "Failed to simulate status change: " + e.getMessage()
            );
        }
    }

    /**
     * 模拟发送开始充电事件
     */
    @PostMapping("/simulate/start")
    public Result<?> simulateChargingStart(
        @RequestBody ChargingStartRequest request
    ) {
        log.info(
            "Simulating charging start: chargerId={}, userId={}",
            request.getChargerId(),
            request.getUserId()
        );

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

            return Result.success(
                "Charging start event sent successfully",
                new SimulateResponse(sessionId, orderNo)
            );
        } catch (Exception e) {
            log.error("Failed to simulate charging start", e);
            return Result.failure(
                "Failed to simulate charging start: " + e.getMessage()
            );
        }
    }

    /**
     * 模拟发送停止充电事件
     */
    @PostMapping("/simulate/stop")
    public Result<?> simulateChargingStop(
        @RequestBody ChargingStopRequest request
    ) {
        log.info(
            "Simulating charging stop: chargerId={}, sessionId={}",
            request.getChargerId(),
            request.getSessionId()
        );

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
            return Result.failure(
                "Failed to simulate charging stop: " + e.getMessage()
            );
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
        @RequestParam(defaultValue = "20") int limit
    ) {
        log.info(
            "Querying event history: chargerId={}, eventType={}, limit={}",
            chargerId,
            eventType,
            limit
        );

        List<ProtocolEvent> allEvents = eventPublisher.getEventHistory();

        List<ProtocolEvent> filtered = allEvents
            .stream()
            .filter(e -> {
                if (chargerId != null) {
                    Long ch = getFieldAsLong(e, "chargerId");
                    if (ch == null || !ch.equals(chargerId)) {
                        return false;
                    }
                }
                if (eventType != null) {
                    String et = getFieldAsString(e, "eventType");
                    if (et == null || !et.equals(eventType)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());

        if (filtered.size() > limit) {
            filtered = filtered.subList(
                Math.max(0, filtered.size() - limit),
                filtered.size()
            );
        }

        return Result.success(filtered);
    }

    /**
     * 获取协议统计信息
     */
    @GetMapping("/stats")
    public Result<?> getProtocolStats() {
        List<ProtocolEvent> events = eventPublisher.getEventHistory();
        ProtocolStats stats = new ProtocolStats();
        stats.setTotalEvents((long) events.size());
        stats.setOcppEvents(
            events
                .stream()
                .filter(e -> "OCPP".equals(getFieldAsString(e, "protocolType")))
                .count()
        );
        stats.setCloudChargeEvents(
            events
                .stream()
                .filter(e ->
                    "CloudCharge".equals(getFieldAsString(e, "protocolType"))
                )
                .count()
        );

        return Result.success(stats);
    }

    // --- Reflection helpers to read fields from ProtocolEvent without relying on generated getters ---
    private Object getFieldValue(ProtocolEvent event, String fieldName) {
        try {
            Field f = ProtocolEvent.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(event);
        } catch (Exception ex) {
            log.warn(
                "Unable to read field '{}' from ProtocolEvent via reflection: {}",
                fieldName,
                ex.getMessage()
            );
            return null;
        }
    }

    private String getFieldAsString(ProtocolEvent event, String fieldName) {
        Object v = getFieldValue(event, fieldName);
        return v == null ? null : v.toString();
    }

    private Long getFieldAsLong(ProtocolEvent event, String fieldName) {
        Object v = getFieldValue(event, fieldName);
        if (v instanceof Long) {
            return (Long) v;
        } else if (v instanceof Integer) {
            return ((Integer) v).longValue();
        } else if (v != null) {
            try {
                return Long.valueOf(v.toString());
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        return null;
    }

    // ===== Request DTOs =====

    public static class HeartbeatRequest {

        private Long chargerId;
        private Long tenantId = 1L;
        private String protocolType = "OCPP";

        public HeartbeatRequest() {}

        public Long getChargerId() {
            return chargerId;
        }

        public void setChargerId(Long chargerId) {
            this.chargerId = chargerId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public String getProtocolType() {
            return protocolType;
        }

        public void setProtocolType(String protocolType) {
            this.protocolType = protocolType;
        }
    }

    public static class StatusChangeRequest {

        private Long chargerId;
        private Long tenantId = 1L;
        private String protocolType = "OCPP";
        private Integer oldStatus;
        private Integer newStatus;
        private String statusDesc;

        public StatusChangeRequest() {}

        public Long getChargerId() {
            return chargerId;
        }

        public void setChargerId(Long chargerId) {
            this.chargerId = chargerId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public String getProtocolType() {
            return protocolType;
        }

        public void setProtocolType(String protocolType) {
            this.protocolType = protocolType;
        }

        public Integer getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(Integer oldStatus) {
            this.oldStatus = oldStatus;
        }

        public Integer getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(Integer newStatus) {
            this.newStatus = newStatus;
        }

        public String getStatusDesc() {
            return statusDesc;
        }

        public void setStatusDesc(String statusDesc) {
            this.statusDesc = statusDesc;
        }
    }

    public static class ChargingStartRequest {

        private Long chargerId;
        private Long tenantId = 1L;
        private Long userId;
        private String protocolType = "OCPP";
        private Double initialEnergy = 0.0;

        public ChargingStartRequest() {}

        public Long getChargerId() {
            return chargerId;
        }

        public void setChargerId(Long chargerId) {
            this.chargerId = chargerId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getProtocolType() {
            return protocolType;
        }

        public void setProtocolType(String protocolType) {
            this.protocolType = protocolType;
        }

        public Double getInitialEnergy() {
            return initialEnergy;
        }

        public void setInitialEnergy(Double initialEnergy) {
            this.initialEnergy = initialEnergy;
        }
    }

    public static class ChargingStopRequest {

        private Long chargerId;
        private Long tenantId = 1L;
        private String protocolType = "OCPP";
        private String sessionId;
        private String orderNo;
        private Double energy;
        private Long duration;
        private String reason = "Normal stop";

        public ChargingStopRequest() {}

        public Long getChargerId() {
            return chargerId;
        }

        public void setChargerId(Long chargerId) {
            this.chargerId = chargerId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public String getProtocolType() {
            return protocolType;
        }

        public void setProtocolType(String protocolType) {
            this.protocolType = protocolType;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public Double getEnergy() {
            return energy;
        }

        public void setEnergy(Double energy) {
            this.energy = energy;
        }

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class SimulateResponse {

        private String sessionId;
        private String orderNo;

        public SimulateResponse(String sessionId, String orderNo) {
            this.sessionId = sessionId;
            this.orderNo = orderNo;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getOrderNo() {
            return orderNo;
        }
    }

    public static class ProtocolStats {

        private long totalEvents;
        private long ocppEvents;
        private long cloudChargeEvents;

        public long getTotalEvents() {
            return totalEvents;
        }

        public void setTotalEvents(long totalEvents) {
            this.totalEvents = totalEvents;
        }

        public long getOcppEvents() {
            return ocppEvents;
        }

        public void setOcppEvents(long ocppEvents) {
            this.ocppEvents = ocppEvents;
        }

        public long getCloudChargeEvents() {
            return cloudChargeEvents;
        }

        public void setCloudChargeEvents(long cloudChargeEvents) {
            this.cloudChargeEvents = cloudChargeEvents;
        }
    }
}
