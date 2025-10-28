package com.evcs.station.metrics;

import com.evcs.common.metrics.BusinessMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 充电站服务业务监控指标
 * 
 * 监控指标包括：
 * - 充电桩总数
 * - 充电桩在线数/离线数
 * - 充电桩在线率
 * - 充电桩状态变更计数
 * - 充电桩心跳接收计数
 */
@Slf4j
@Component
public class StationMetrics extends BusinessMetrics {

    // 充电桩状态计数
    private final AtomicInteger totalChargersCount = new AtomicInteger(0);
    private final AtomicInteger onlineChargersCount = new AtomicInteger(0);
    private final AtomicInteger offlineChargersCount = new AtomicInteger(0);
    private final AtomicInteger chargingChargersCount = new AtomicInteger(0);
    private final AtomicInteger faultedChargersCount = new AtomicInteger(0);
    
    // 充电桩状态变更计数器
    private Counter chargerOnlineCounter;
    private Counter chargerOfflineCounter;
    private Counter chargerStatusChangeCounter;
    
    // 心跳接收计数器
    private Counter heartbeatReceivedCounter;
    private Counter heartbeatMissedCounter;
    
    // 充电站操作计数器
    private Counter stationCreatedCounter;
    private Counter stationUpdatedCounter;
    
    // 用于跟踪每个充电桩的在线状态
    private final ConcurrentHashMap<Long, Boolean> chargerOnlineStatus = new ConcurrentHashMap<>();

    public StationMetrics(MeterRegistry meterRegistry) {
        super(meterRegistry);
    }

    @Override
    protected void registerMetrics() {
        // 充电桩数量指标
        createGauge(
            "evcs.charger.total",
            "Total number of chargers",
            totalChargersCount
        );
        
        createGauge(
            "evcs.charger.online",
            "Number of online chargers",
            onlineChargersCount
        );
        
        createGauge(
            "evcs.charger.offline",
            "Number of offline chargers",
            offlineChargersCount
        );
        
        createGauge(
            "evcs.charger.charging",
            "Number of chargers currently charging",
            chargingChargersCount
        );
        
        createGauge(
            "evcs.charger.faulted",
            "Number of faulted chargers",
            faultedChargersCount
        );
        
        // 充电桩状态变更计数器
        chargerOnlineCounter = createCounter(
            "evcs.charger.online.total",
            "Total number of charger online events"
        );
        
        chargerOfflineCounter = createCounter(
            "evcs.charger.offline.total",
            "Total number of charger offline events"
        );
        
        chargerStatusChangeCounter = createCounter(
            "evcs.charger.status.change.total",
            "Total number of charger status changes"
        );
        
        // 心跳计数器
        heartbeatReceivedCounter = createCounter(
            "evcs.charger.heartbeat.received.total",
            "Total number of heartbeats received from chargers"
        );
        
        heartbeatMissedCounter = createCounter(
            "evcs.charger.heartbeat.missed.total",
            "Total number of missed heartbeats from chargers"
        );
        
        // 充电站操作计数器
        stationCreatedCounter = createCounter(
            "evcs.station.created.total",
            "Total number of stations created"
        );
        
        stationUpdatedCounter = createCounter(
            "evcs.station.updated.total",
            "Total number of stations updated"
        );
        
        log.info("Station business metrics registered successfully");
    }

    /**
     * 记录充电桩上线
     * 
     * @param chargerId 充电桩ID
     */
    public void recordChargerOnline(Long chargerId) {
        Boolean wasOnline = chargerOnlineStatus.put(chargerId, true);
        if (wasOnline == null || !wasOnline) {
            onlineChargersCount.incrementAndGet();
            if (wasOnline != null) {
                offlineChargersCount.decrementAndGet();
            }
            incrementCounter(chargerOnlineCounter);
            incrementCounter(chargerStatusChangeCounter);
        }
    }

    /**
     * 记录充电桩离线
     * 
     * @param chargerId 充电桩ID
     */
    public void recordChargerOffline(Long chargerId) {
        Boolean wasOnline = chargerOnlineStatus.put(chargerId, false);
        if (wasOnline == null || wasOnline) {
            offlineChargersCount.incrementAndGet();
            if (wasOnline != null) {
                onlineChargersCount.decrementAndGet();
            }
            incrementCounter(chargerOfflineCounter);
            incrementCounter(chargerStatusChangeCounter);
        }
    }

    /**
     * 记录充电桩开始充电
     */
    public void recordChargerStartCharging() {
        chargingChargersCount.incrementAndGet();
        incrementCounter(chargerStatusChangeCounter);
    }

    /**
     * 记录充电桩停止充电
     */
    public void recordChargerStopCharging() {
        chargingChargersCount.decrementAndGet();
        incrementCounter(chargerStatusChangeCounter);
    }

    /**
     * 记录充电桩故障
     */
    public void recordChargerFaulted() {
        faultedChargersCount.incrementAndGet();
        incrementCounter(chargerStatusChangeCounter);
    }

    /**
     * 记录充电桩故障恢复
     */
    public void recordChargerFaultRecovered() {
        faultedChargersCount.decrementAndGet();
        incrementCounter(chargerStatusChangeCounter);
    }

    /**
     * 记录心跳接收
     */
    public void recordHeartbeatReceived() {
        incrementCounter(heartbeatReceivedCounter);
    }

    /**
     * 记录心跳丢失
     */
    public void recordHeartbeatMissed() {
        incrementCounter(heartbeatMissedCounter);
    }

    /**
     * 记录充电站创建
     */
    public void recordStationCreated() {
        incrementCounter(stationCreatedCounter);
    }

    /**
     * 记录充电站更新
     */
    public void recordStationUpdated() {
        incrementCounter(stationUpdatedCounter);
    }

    /**
     * 更新充电桩总数
     * 
     * @param count 总数
     */
    public void setTotalChargersCount(int count) {
        totalChargersCount.set(count);
    }

    /**
     * 计算充电桩在线率
     * 
     * @return 在线率百分比 (0-100)
     */
    public double getChargerOnlineRate() {
        int total = totalChargersCount.get();
        if (total == 0) {
            return 0.0;
        }
        return ((double) onlineChargersCount.get() / total) * 100.0;
    }

    /**
     * 计算心跳接收率
     * 
     * @return 接收率百分比 (0-100)
     */
    public double getHeartbeatSuccessRate() {
        double total = heartbeatReceivedCounter.count() + heartbeatMissedCounter.count();
        if (total == 0) {
            return 100.0;
        }
        return (heartbeatReceivedCounter.count() / total) * 100.0;
    }
}
