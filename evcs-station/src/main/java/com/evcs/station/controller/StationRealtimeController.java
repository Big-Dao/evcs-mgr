package com.evcs.station.controller;

import com.evcs.common.result.Result;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 充电站实时状态控制器
 * 提供WebSocket和SSE支持，用于实时监控充电站状态变化
 */
@Slf4j
@Tag(name = "充电站实时状态", description = "充电站状态实时监控和通知")
@RestController
@RequestMapping("/station/realtime")
@RequiredArgsConstructor
public class StationRealtimeController {

    private final IStationService stationService;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 订阅充电站状态变化事件
     */
    @Operation(summary = "订阅充电站状态变化", description = "通过SSE实时接收充电站状态变化通知")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    public SseEmitter subscribeStationStatus() {
        SseEmitter emitter = new SseEmitter(30000L); // 30秒超时

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("SSE连接完成，当前连接数: {}", emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("SSE连接超时，当前连接数: {}", emitters.size());
        });

        emitters.add(emitter);
        log.info("新的SSE连接建立，当前连接数: {}", emitters.size());

        // 发送初始连接确认
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"message\":\"连接成功\",\"timestamp\":" + System.currentTimeMillis() + "}")
                .id(String.valueOf(System.currentTimeMillis())));
        } catch (IOException e) {
            log.error("发送SSE连接确认失败", e);
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * 广播充电站状态变化
     */
    @Operation(summary = "广播状态变化", description = "向所有订阅的客户端广播充电站状态变化")
    @PostMapping("/broadcast/{stationId}")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:edit')")
    public Result<Void> broadcastStationStatusChange(
            @Parameter(description = "充电站ID") @PathVariable Long stationId,
            @Parameter(description = "状态信息") @RequestBody StatusChangeRequest request) {

        try {
            Station station = stationService.getById(stationId);
            if (station == null) {
                return Result.fail("充电站不存在");
            }

            // 构建状态变化消息
            StatusChangeEvent event = new StatusChangeEvent();
            event.setStationId(stationId);
            event.setStationCode(station.getStationCode());
            event.setStationName(station.getStationName());
            event.setOldStatus(request.getOldStatus());
            event.setNewStatus(request.getNewStatus());
            event.setChangeTime(System.currentTimeMillis());
            event.setChangedBy(request.getChangedBy());
            event.setMessage(request.getMessage());

            // 向所有订阅者广播
            broadcastStatusChange(event);

            log.info("广播充电站状态变化: stationId={}, {} -> {}",
                stationId, request.getOldStatus(), request.getNewStatus());

            return Result.success("状态变化广播成功");
        } catch (Exception e) {
            log.error("广播充电站状态变化失败", e);
            return Result.fail("广播失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时统计数据
     */
    @Operation(summary = "获取实时统计", description = "获取当前充电站的实时统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("@simplePermissionEvaluator.hasPermission(authentication, null, 'station:query')")
    public Result<StationStatistics> getRealtimeStatistics() {
        try {
            StationStatistics statistics = stationService.getRealtimeStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取实时统计数据失败", e);
            return Result.fail("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 向所有SSE连接广播状态变化
     */
    private void broadcastStatusChange(StatusChangeEvent event) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("statusChange")
                    .data(event)
                    .id(String.valueOf(System.currentTimeMillis())));
            } catch (IOException e) {
                log.warn("SSE发送失败，移除连接", e);
                deadEmitters.add(emitter);
            }
        }

        // 移除失效的连接
        emitters.removeAll(deadEmitters);
    }

    /**
     * 状态变化请求
     */
    public static class StatusChangeRequest {
        private Integer oldStatus;
        private Integer newStatus;
        private String changedBy;
        private String message;

        // Getters and setters
        public Integer getOldStatus() { return oldStatus; }
        public void setOldStatus(Integer oldStatus) { this.oldStatus = oldStatus; }
        public Integer getNewStatus() { return newStatus; }
        public void setNewStatus(Integer newStatus) { this.newStatus = newStatus; }
        public String getChangedBy() { return changedBy; }
        public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 状态变化事件
     */
    public static class StatusChangeEvent {
        private Long stationId;
        private String stationCode;
        private String stationName;
        private Integer oldStatus;
        private Integer newStatus;
        private Long changeTime;
        private String changedBy;
        private String message;

        // Getters and setters
        public Long getStationId() { return stationId; }
        public void setStationId(Long stationId) { this.stationId = stationId; }
        public String getStationCode() { return stationCode; }
        public void setStationCode(String stationCode) { this.stationCode = stationCode; }
        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }
        public Integer getOldStatus() { return oldStatus; }
        public void setOldStatus(Integer oldStatus) { this.oldStatus = oldStatus; }
        public Integer getNewStatus() { return newStatus; }
        public void setNewStatus(Integer newStatus) { this.newStatus = newStatus; }
        public Long getChangeTime() { return changeTime; }
        public void setChangeTime(Long changeTime) { this.changeTime = changeTime; }
        public String getChangedBy() { return changedBy; }
        public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * 充电站统计数据
     */
    public static class StationStatistics {
        private Long totalStations;
        private Long onlineStations;
        private Long offlineStations;
        private Long totalChargers;
        private Long availableChargers;
        private Long chargingChargers;
        private Long faultChargers;
        private Double averageUtilizationRate;

        // Getters and setters
        public Long getTotalStations() { return totalStations; }
        public void setTotalStations(Long totalStations) { this.totalStations = totalStations; }
        public Long getOnlineStations() { return onlineStations; }
        public void setOnlineStations(Long onlineStations) { this.onlineStations = onlineStations; }
        public Long getOfflineStations() { return offlineStations; }
        public void setOfflineStations(Long offlineStations) { this.offlineStations = offlineStations; }
        public Long getTotalChargers() { return totalChargers; }
        public void setTotalChargers(Long totalChargers) { this.totalChargers = totalChargers; }
        public Long getAvailableChargers() { return availableChargers; }
        public void setAvailableChargers(Long availableChargers) { this.availableChargers = availableChargers; }
        public Long getChargingChargers() { return chargingChargers; }
        public void setChargingChargers(Long chargingChargers) { this.chargingChargers = chargingChargers; }
        public Long getFaultChargers() { return faultChargers; }
        public void setFaultChargers(Long faultChargers) { this.faultChargers = faultChargers; }
        public Double getAverageUtilizationRate() { return averageUtilizationRate; }
        public void setAverageUtilizationRate(Double averageUtilizationRate) { this.averageUtilizationRate = averageUtilizationRate; }
    }
}