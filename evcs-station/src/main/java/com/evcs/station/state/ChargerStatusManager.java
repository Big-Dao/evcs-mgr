package com.evcs.station.state;

import com.evcs.station.enums.ChargerStatus;
import com.evcs.station.event.ChargerStatusChangeEvent;
import com.evcs.station.metrics.StationMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 充电桩状态管理器
 * 负责校验状态流转并处理副作用（监控、事件）
 */
@Slf4j
@Component
public class ChargerStatusManager {

    private final StationMetrics stationMetrics;
    private final ApplicationEventPublisher eventPublisher;

    // 状态流转图
    private static final Map<ChargerStatus, Set<ChargerStatus>> TRANSITIONS = new EnumMap<>(ChargerStatus.class);

    static {
        // OFFLINE -> IDLE, FAULT, MAINTAIN
        TRANSITIONS.put(ChargerStatus.OFFLINE,
                EnumSet.of(ChargerStatus.IDLE, ChargerStatus.FAULT, ChargerStatus.MAINTAIN));

        // IDLE -> CHARGING, BOOKED, OFFLINE, FAULT, MAINTAIN
        TRANSITIONS.put(ChargerStatus.IDLE, EnumSet.allOf(ChargerStatus.class)); // 允许跳到任何状态（除了自己，但也允许）

        // CHARGING -> IDLE, BOOKED, FAULT, OFFLINE, MAINTAIN
        TRANSITIONS.put(ChargerStatus.CHARGING,
            EnumSet.of(
                ChargerStatus.IDLE,
                ChargerStatus.BOOKED,
                ChargerStatus.FAULT,
                ChargerStatus.OFFLINE,
                ChargerStatus.MAINTAIN
            ));

        // FAULT -> IDLE (recover), OFFLINE, MAINTAIN, CHARGING
        TRANSITIONS.put(ChargerStatus.FAULT,
            EnumSet.of(ChargerStatus.IDLE, ChargerStatus.OFFLINE, ChargerStatus.MAINTAIN, ChargerStatus.CHARGING));

        // MAINTAIN -> IDLE, OFFLINE, FAULT, CHARGING
        TRANSITIONS.put(ChargerStatus.MAINTAIN,
            EnumSet.of(ChargerStatus.IDLE, ChargerStatus.OFFLINE, ChargerStatus.FAULT, ChargerStatus.CHARGING));

        // BOOKED -> IDLE, CHARGING, FAULT, OFFLINE
        TRANSITIONS.put(ChargerStatus.BOOKED,
            EnumSet.of(
                ChargerStatus.IDLE,
                ChargerStatus.CHARGING,
                ChargerStatus.FAULT,
                ChargerStatus.MAINTAIN,
                ChargerStatus.OFFLINE
            ));
    }

    public ChargerStatusManager(StationMetrics stationMetrics, ApplicationEventPublisher eventPublisher) {
        this.stationMetrics = stationMetrics;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 校验状态流转是否合法
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return true 合法, false 非法
     */
    public boolean validateTransition(ChargerStatus from, ChargerStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true; // 状态不变视为合法，但通常不触发后续逻辑
        }
        // 如果当前状态不在映射表中（理应都在），默认允许转去任何状态（降级策略），或者严格禁止
        // 这里采用白名单策略
        Set<ChargerStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * 执行状态流转的副作用
     *
     * @param chargerId 充电桩ID
     * @param from      源状态
     * @param to        目标状态
     */
    public void onTransition(Long chargerId, ChargerStatus from, ChargerStatus to, Long tenantId) {
        if (from == to) {
            return;
        }
        log.info("State transition for charger {}: {} -> {}", chargerId, from, to);

        // 1. 更新监控指标
        updateMetrics(chargerId, from, to);

        // 2. 发布事件
        eventPublisher.publishEvent(new ChargerStatusChangeEvent(this, chargerId, from, to, tenantId));
    }

    private void updateMetrics(Long chargerId, ChargerStatus from, ChargerStatus to) {
        // 先处理离线/在线变化
        if (from == ChargerStatus.OFFLINE && to != ChargerStatus.OFFLINE) {
            stationMetrics.recordChargerOnline(chargerId);
        } else if (to == ChargerStatus.OFFLINE) {
            stationMetrics.recordChargerOffline(chargerId);
        }

        // 处理业务状态变化
        switch (to) {
            case CHARGING:
                stationMetrics.recordChargerStartCharging();
                break;
            case FAULT:
                stationMetrics.recordChargerFaulted();
                break;
            default:
                break;
        }

        switch (from) {
            case CHARGING:
                stationMetrics.recordChargerStopCharging();
                break;
            case FAULT:
                stationMetrics.recordChargerFaultRecovered();
                break;
            default:
                break;
        }
    }
}
