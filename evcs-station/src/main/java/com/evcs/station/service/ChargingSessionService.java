package com.evcs.station.service;

import com.evcs.common.result.Result;
import com.evcs.station.client.OrderClient;
import com.evcs.station.client.ProtocolClient;
import com.evcs.station.dto.ChargingDispatchOutcome;
import com.evcs.station.entity.Charger;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 充电会话指令编排服务。
 *
 * <p>负责"创建/结算订单 → 下发协议指令 → 更新枪口会话"的跨服务编排
 * （含分布式锁），Controller 只做参数与结果映射。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargingSessionService {

    private final IChargerService chargerService;
    private final IChargerConnectorService chargerConnectorService;
    private final OrderClient orderClient;
    private final ProtocolClient protocolClient;
    private final RedissonClient redissonClient;

    /**
     * 开始充电（按枪口）：创建订单 → 下发启动指令 → 记录会话开始。
     */
    public ChargingDispatchOutcome startCharging(Long chargerId, Integer connectorNo,
                                                 String sessionId, Long userId, Double initialEnergy) {
        Charger charger = chargerService.getById(chargerId);
        if (charger == null) {
            return ChargingDispatchOutcome.fail("充电桩不存在");
        }

        String lockKey = "lock:charger:connector:" + chargerId + ":" + connectorNo;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                return ChargingDispatchOutcome.fail("充电桩正在处理中，请稍后重试");
            }

            String actualSessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();

            // 1. Create Order (Pre-check)
            try {
                Result<Boolean> orderResult = orderClient.startOrder(
                    charger.getStationId(),
                    chargerId,
                    actualSessionId,
                    userId,
                    null // billingPlanId optional
                );
                if (!orderResult.isSuccess()) {
                    log.warn("Order creation failed: {}", orderResult.getMessage());
                    return ChargingDispatchOutcome.fail("创建订单失败: " + orderResult.getMessage());
                }
            } catch (Exception e) {
                log.error("Order creation exception", e);
                return ChargingDispatchOutcome.fail("创建订单异常: " + e.getMessage());
            }

            // 2. Call Protocol Service
            try {
                ProtocolRequest req = new ProtocolRequest();
                req.setDeviceCode(charger.getChargerCode());
                req.setSessionId(actualSessionId);
                req.setUserId(userId);
                req.setChargerId(chargerId);
                req.setTenantId(charger.getTenantId());

                Map<String, Object> data = new HashMap<>();
                data.put("connectorId", connectorNo);
                req.setData(data);

                Result<ProtocolResponse> protoResult = protocolClient.startCharging(req);
                if (!protoResult.isSuccess()) {
                    log.warn("Remote start failed: {}", protoResult.getMessage());
                    // 注意：协议启动失败时，订单处于待启动状态，需要人工处理或自动取消
                    // 建议订单服务实现超时自动取消机制
                    return ChargingDispatchOutcome.fail("远程启动失败: " + protoResult.getMessage());
                }
            } catch (Exception e) {
                log.error("Remote start exception", e);
                return ChargingDispatchOutcome.fail("远程启动异常: " + e.getMessage());
            }

            boolean ok = chargerConnectorService.updateSessionStart(
                    chargerId,
                    connectorNo,
                    actualSessionId,
                    userId,
                    LocalDateTime.now(),
                    initialEnergy
            );
            return ok
                    ? ChargingDispatchOutcome.success("开始充电指令已下发")
                    : ChargingDispatchOutcome.fail("开始充电失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ChargingDispatchOutcome.fail("获取锁被中断");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 结束充电（按枪口）：下发停止指令 → 结算订单 → 记录会话结束。
     */
    public ChargingDispatchOutcome stopCharging(Long chargerId, Integer connectorNo,
                                                String sessionId, Double energy, Long duration) {
        Charger charger = chargerService.getById(chargerId);
        if (charger == null) {
            return ChargingDispatchOutcome.fail("充电桩不存在");
        }

        String lockKey = "lock:charger:connector:" + chargerId + ":" + connectorNo;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                return ChargingDispatchOutcome.fail("充电桩正在处理中，请稍后重试");
            }

            // 1. Call Protocol Service
            try {
                ProtocolRequest req = new ProtocolRequest();
                req.setDeviceCode(charger.getChargerCode());
                req.setSessionId(sessionId);
                req.setChargerId(chargerId);
                req.setTenantId(charger.getTenantId());

                Map<String, Object> data = new HashMap<>();
                data.put("connectorId", connectorNo);
                req.setData(data);

                Result<ProtocolResponse> protoResult = protocolClient.stopCharging(req);
                if (!protoResult.isSuccess()) {
                     log.warn("Remote stop failed: {}", protoResult.getMessage());
                     return ChargingDispatchOutcome.fail("远程停止失败: " + protoResult.getMessage());
                }
            } catch (Exception e) {
                log.error("Remote stop exception", e);
                return ChargingDispatchOutcome.fail("远程停止异常: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ChargingDispatchOutcome.fail("获取锁被中断");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        Double energyValue = energy != null ? energy : 0.0;
        Long durationValue = duration != null ? duration : 0L;

        // 2. Settle Order
        try {
            Result<Boolean> orderResult = orderClient.stopOrder(
                sessionId,
                energyValue,
                durationValue
            );
            if (!orderResult.isSuccess()) {
                log.warn("Order settlement failed: {}", orderResult.getMessage());
                // Continue to update local session even if order fails?
                // return ChargingDispatchOutcome.fail("结算订单失败: " + orderResult.getMessage());
            }
        } catch (Exception e) {
            log.error("Order settlement exception", e);
        }

        boolean ok = chargerConnectorService.updateSessionStop(
                chargerId,
                connectorNo,
                sessionId,
                energyValue,
                durationValue
        );
        return ok
                ? ChargingDispatchOutcome.success("结束充电指令已下发")
                : ChargingDispatchOutcome.fail("结束充电失败");
    }
}
