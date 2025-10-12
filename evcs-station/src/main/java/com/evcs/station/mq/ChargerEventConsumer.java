package com.evcs.station.mq;

import com.evcs.protocol.api.IProtocolEventConsumer;
import com.evcs.protocol.event.HeartbeatEvent;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StatusEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.station.mapper.ChargerMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 充电桩事件消费者
 * 监听协议事件队列，更新充电桩状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargerEventConsumer implements IProtocolEventConsumer {
    
    private final ChargerMapper chargerMapper;
    
    /**
     * 处理心跳事件 - 更新最后在线时间
     */
    @Override
    @RabbitHandler
    @RabbitListener(queues = "evcs.protocol.heartbeat")
    public void onHeartbeatEvent(HeartbeatEvent event) {
        log.debug("Received heartbeat event: chargerId={}, time={}", 
                event.getChargerId(), event.getLastHeartbeatTime());
        
        try {
            chargerMapper.updateStatus(
                    event.getChargerId(), 
                    null, // 不更新状态，只更新心跳时间
                    event.getLastHeartbeatTime()
            );
            log.debug("Updated charger heartbeat: chargerId={}", event.getChargerId());
        } catch (Exception e) {
            log.error("Failed to process heartbeat event: chargerId={}", 
                    event.getChargerId(), e);
            throw new RuntimeException("Failed to process heartbeat event", e);
        }
    }
    
    /**
     * 处理状态变更事件 - 更新充电桩状态
     */
    @Override
    @RabbitHandler
    @RabbitListener(queues = "evcs.protocol.status")
    public void onStatusEvent(StatusEvent event) {
        log.info("Received status event: chargerId={}, oldStatus={}, newStatus={}", 
                event.getChargerId(), event.getOldStatus(), event.getNewStatus());
        
        try {
            chargerMapper.updateStatus(
                    event.getChargerId(), 
                    event.getNewStatus(),
                    LocalDateTime.now()
            );
            log.info("Updated charger status: chargerId={}, status={}", 
                    event.getChargerId(), event.getNewStatus());
        } catch (Exception e) {
            log.error("Failed to process status event: chargerId={}, status={}", 
                    event.getChargerId(), event.getNewStatus(), e);
            throw new RuntimeException("Failed to process status event", e);
        }
    }
    
    /**
     * 处理开始充电事件 - 更新充电桩为充电中状态
     */
    @Override
    @RabbitHandler
    @RabbitListener(queues = "evcs.protocol.charging")
    public void onStartEvent(StartEvent event) {
        log.info("Received charging start event: chargerId={}, sessionId={}, orderNo={}", 
                event.getChargerId(), event.getSessionId(), event.getOrderNo());
        
        try {
            // 将充电桩状态更新为充电中（状态值2）
            chargerMapper.updateStatus(
                    event.getChargerId(), 
                    2, // 充电中
                    LocalDateTime.now()
            );
            log.info("Updated charger to charging status: chargerId={}", event.getChargerId());
        } catch (Exception e) {
            log.error("Failed to process charging start event: chargerId={}, sessionId={}", 
                    event.getChargerId(), event.getSessionId(), e);
            throw new RuntimeException("Failed to process charging start event", e);
        }
    }
    
    /**
     * 处理停止充电事件 - 更新充电桩为空闲状态
     */
    @Override
    @RabbitHandler
    @RabbitListener(queues = "evcs.protocol.charging")
    public void onStopEvent(StopEvent event) {
        log.info("Received charging stop event: chargerId={}, sessionId={}, energy={}, duration={}", 
                event.getChargerId(), event.getSessionId(), event.getEnergy(), event.getDuration());
        
        try {
            // 将充电桩状态更新为空闲（状态值1）
            chargerMapper.updateStatus(
                    event.getChargerId(), 
                    1, // 空闲
                    LocalDateTime.now()
            );
            log.info("Updated charger to idle status: chargerId={}", event.getChargerId());
        } catch (Exception e) {
            log.error("Failed to process charging stop event: chargerId={}, sessionId={}", 
                    event.getChargerId(), event.getSessionId(), e);
            throw new RuntimeException("Failed to process charging stop event", e);
        }
    }
}
