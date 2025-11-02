package com.evcs.protocol.service.impl;

import com.evcs.protocol.api.IProtocolService;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.config.ProtocolProperties;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.protocol.enums.ProtocolType;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 协议服务基础实现类
 * 提供通用的协议服务功能和模板方法
 */
@Slf4j
public abstract class BaseProtocolService implements IProtocolService {

    protected final ProtocolProperties protocolProperties;
    protected volatile ProtocolEventListener eventListener;
    protected final ConcurrentHashMap<String, Boolean> connectionStatus = new ConcurrentHashMap<>();
    protected final AtomicLong messageCounter = new AtomicLong(0);

    protected BaseProtocolService(ProtocolProperties protocolProperties) {
        this.protocolProperties = protocolProperties;
    }

    @Override
    public boolean connect(String deviceCode, ProtocolType protocolType) {
        log.info("[{}] Connecting device: {}", getSupportedProtocolType(), deviceCode);

        try {
            boolean connected = doConnect(deviceCode, protocolType);
            connectionStatus.put(deviceCode, connected);

            if (connected) {
                onDeviceConnected(deviceCode, protocolType);
                log.info("[{}] Device connected successfully: {}", getSupportedProtocolType(), deviceCode);
            } else {
                log.warn("[{}] Failed to connect device: {}", getSupportedProtocolType(), deviceCode);
            }

            return connected;
        } catch (Exception e) {
            log.error("[{}] Error connecting device: {}", getSupportedProtocolType(), deviceCode, e);
            connectionStatus.put(deviceCode, false);
            return false;
        }
    }

    @Override
    public void disconnect(String deviceCode, ProtocolType protocolType) {
        log.info("[{}] Disconnecting device: {}", getSupportedProtocolType(), deviceCode);

        try {
            doDisconnect(deviceCode, protocolType);
            connectionStatus.put(deviceCode, false);
            onDeviceDisconnected(deviceCode, protocolType);
            log.info("[{}] Device disconnected successfully: {}", getSupportedProtocolType(), deviceCode);
        } catch (Exception e) {
            log.error("[{}] Error disconnecting device: {}", getSupportedProtocolType(), deviceCode, e);
        }
    }

    @Override
    public ProtocolResponse handleRequest(ProtocolRequest request) {
        if (request == null) {
            return ProtocolResponse.failure("400", "Request cannot be null");
        }

        if (!getSupportedProtocolType().equals(request.getProtocolType())) {
            return ProtocolResponse.failure("400", "Protocol type mismatch");
        }

        try {
            long messageId = messageCounter.incrementAndGet();
            log.debug("[{}] Handling request #{}: {}", getSupportedProtocolType(), messageId, request.getAction());

            switch (request.getAction()) {
                case "heartbeat":
                    return handleHeartbeat(request);
                case "status":
                    return handleStatusUpdate(request);
                case "start":
                    return handleStartCharging(request);
                case "stop":
                    return handleStopCharging(request);
                case "register":
                    return registerStation(request);
                default:
                    return handleCustomRequest(request);
            }
        } catch (Exception e) {
            log.error("[{}] Error handling request: {}", getSupportedProtocolType(), request, e);
            return ProtocolResponse.failure("500", "Internal server error: " + e.getMessage());
        }
    }

    @Override
    public ProtocolResponse registerStation(ProtocolRequest request) {
        log.info("[{}] Registering station: {}", getSupportedProtocolType(), request.getDeviceCode());

        try {
            boolean registered = doRegisterStation(request);
            if (registered) {
                onStationRegistered(request);
                return ProtocolResponse.success("Station registered successfully");
            } else {
                return ProtocolResponse.failure("500", "Failed to register station");
            }
        } catch (Exception e) {
            log.error("[{}] Error registering station: {}", getSupportedProtocolType(), request.getDeviceCode(), e);
            return ProtocolResponse.failure("500", "Registration error: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected(String deviceCode, ProtocolType protocolType) {
        return connectionStatus.getOrDefault(deviceCode, false);
    }

    @Override
    public void setEventListener(ProtocolEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * 处理心跳请求的模板方法
     */
    protected ProtocolResponse handleHeartbeat(ProtocolRequest request) {
        log.debug("[{}] Processing heartbeat for device: {}", getSupportedProtocolType(), request.getDeviceCode());

        try {
            boolean success = doSendHeartbeat(request);
            if (success) {
                onHeartbeatReceived(request);
                return ProtocolResponse.success("Heartbeat processed");
            } else {
                return ProtocolResponse.failure("500", "Heartbeat processing failed");
            }
        } catch (Exception e) {
            log.error("[{}] Error processing heartbeat", getSupportedProtocolType(), e);
            return ProtocolResponse.failure("500", "Heartbeat error: " + e.getMessage());
        }
    }

    /**
     * 处理状态更新的模板方法
     */
    protected ProtocolResponse handleStatusUpdate(ProtocolRequest request) {
        log.debug("[{}] Processing status update for device: {}", getSupportedProtocolType(), request.getDeviceCode());

        try {
            boolean success = doUpdateStatus(request);
            if (success) {
                onStatusUpdated(request);
                return ProtocolResponse.success("Status updated");
            } else {
                return ProtocolResponse.failure("500", "Status update failed");
            }
        } catch (Exception e) {
            log.error("[{}] Error processing status update", getSupportedProtocolType(), e);
            return ProtocolResponse.failure("500", "Status update error: " + e.getMessage());
        }
    }

    /**
     * 处理开始充电的模板方法
     */
    protected ProtocolResponse handleStartCharging(ProtocolRequest request) {
        log.debug("[{}] Processing start charging for device: {}", getSupportedProtocolType(), request.getDeviceCode());

        try {
            boolean success = doStartCharging(request);
            if (success) {
                onChargingStarted(request);
                return ProtocolResponse.success("Charging started");
            } else {
                return ProtocolResponse.failure("500", "Start charging failed");
            }
        } catch (Exception e) {
            log.error("[{}] Error processing start charging", getSupportedProtocolType(), e);
            return ProtocolResponse.failure("500", "Start charging error: " + e.getMessage());
        }
    }

    /**
     * 处理停止充电的模板方法
     */
    protected ProtocolResponse handleStopCharging(ProtocolRequest request) {
        log.debug("[{}] Processing stop charging for device: {}", getSupportedProtocolType(), request.getDeviceCode());

        try {
            boolean success = doStopCharging(request);
            if (success) {
                onChargingStopped(request);
                return ProtocolResponse.success("Charging stopped");
            } else {
                return ProtocolResponse.failure("500", "Stop charging failed");
            }
        } catch (Exception e) {
            log.error("[{}] Error processing stop charging", getSupportedProtocolType(), e);
            return ProtocolResponse.failure("500", "Stop charging error: " + e.getMessage());
        }
    }

    /**
     * 处理自定义请求的模板方法
     */
    protected ProtocolResponse handleCustomRequest(ProtocolRequest request) {
        log.debug("[{}] Processing custom request: {}", getSupportedProtocolType(), request.getAction());
        return ProtocolResponse.failure("404", "Unsupported action: " + request.getAction());
    }

    // ============ 抽象方法 - 子类必须实现 ============

    /**
     * 执行连接操作
     */
    protected abstract boolean doConnect(String deviceCode, ProtocolType protocolType);

    /**
     * 执行断开连接操作
     */
    protected abstract void doDisconnect(String deviceCode, ProtocolType protocolType);

    /**
     * 执行注册站点操作
     */
    protected abstract boolean doRegisterStation(ProtocolRequest request);

    /**
     * 执行心跳操作
     */
    protected abstract boolean doSendHeartbeat(ProtocolRequest request);

    /**
     * 执行状态更新操作
     */
    protected abstract boolean doUpdateStatus(ProtocolRequest request);

    /**
     * 执行开始充电操作
     */
    protected abstract boolean doStartCharging(ProtocolRequest request);

    /**
     * 执行停止充电操作
     */
    protected abstract boolean doStopCharging(ProtocolRequest request);

    // ============ 事件回调方法 - 子类可以重写 ============

    /**
     * 设备连接事件
     */
    protected void onDeviceConnected(String deviceCode, ProtocolType protocolType) {
        // 默认实现为空，子类可以重写
    }

    /**
     * 设备断开连接事件
     */
    protected void onDeviceDisconnected(String deviceCode, ProtocolType protocolType) {
        // 默认实现为空，子类可以重写
    }

    /**
     * 站点注册事件
     */
    protected void onStationRegistered(ProtocolRequest request) {
        // 默认实现为空，子类可以重写
    }

    /**
     * 心跳接收事件
     */
    protected void onHeartbeatReceived(ProtocolRequest request) {
        if (eventListener != null) {
            eventListener.onHeartbeat(request.getChargerId(), request.getTimestamp());
        }
    }

    /**
     * 状态更新事件
     */
    protected void onStatusUpdated(ProtocolRequest request) {
        Integer status = request.getData("status", Integer.class);
        if (eventListener != null && status != null) {
            eventListener.onStatusChange(request.getChargerId(), status);
        }
    }

    /**
     * 充电开始事件
     */
    protected void onChargingStarted(ProtocolRequest request) {
        if (eventListener != null) {
            eventListener.onStartAck(request.getChargerId(), request.getSessionId(), true, "Charging started");
        }
    }

    /**
     * 充电停止事件
     */
    protected void onChargingStopped(ProtocolRequest request) {
        if (eventListener != null) {
            eventListener.onStopAck(request.getChargerId(), true, "Charging stopped");
        }
    }

    // ============ 工具方法 ============

    /**
     * 获取连接的设备数量
     */
    public int getConnectedDeviceCount() {
        return (int) connectionStatus.values().stream().mapToInt(connected -> connected ? 1 : 0).sum();
    }

    /**
     * 获取消息计数
     */
    public long getMessageCount() {
        return messageCounter.get();
    }

    /**
     * 重置消息计数
     */
    public void resetMessageCount() {
        messageCounter.set(0);
    }

    /**
     * 检查设备是否连接
     */
    protected boolean isDeviceConnected(String deviceCode) {
        return connectionStatus.getOrDefault(deviceCode, false);
    }

    /**
     * 设置设备连接状态
     */
    protected void setDeviceConnectionStatus(String deviceCode, boolean connected) {
        connectionStatus.put(deviceCode, connected);
    }
}