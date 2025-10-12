package com.evcs.protocol.api;

import com.evcs.protocol.event.*;

/**
 * 协议事件消费者接口
 * 业务服务实现此接口来消费协议事件
 */
public interface IProtocolEventConsumer {
    
    /**
     * 处理心跳事件
     * @param event 心跳事件
     */
    void onHeartbeatEvent(HeartbeatEvent event);
    
    /**
     * 处理状态变更事件
     * @param event 状态变更事件
     */
    void onStatusEvent(StatusEvent event);
    
    /**
     * 处理开始充电事件
     * @param event 开始充电事件
     */
    void onStartEvent(StartEvent event);
    
    /**
     * 处理停止充电事件
     * @param event 停止充电事件
     */
    void onStopEvent(StopEvent event);
}
