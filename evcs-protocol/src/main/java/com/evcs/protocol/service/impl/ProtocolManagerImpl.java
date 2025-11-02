package com.evcs.protocol.service.impl;

import com.evcs.protocol.api.IProtocolService;
import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.api.ProtocolManager;
import com.evcs.protocol.config.ProtocolProperties;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.protocol.enums.ProtocolType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议管理器实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolManagerImpl implements ProtocolManager {

    private final ProtocolProperties protocolProperties;
    private final Map<ProtocolType, IProtocolService> protocolServices = new ConcurrentHashMap<>();
    private final Map<String, ProtocolType> deviceProtocolTypes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> deviceConnectionStatus = new ConcurrentHashMap<>();
    private volatile ProtocolEventListener globalEventListener;

    @Override
    public void registerProtocolService(ProtocolType protocolType, IProtocolService protocolService) {
        log.info("Registering protocol service: {} -> {}", protocolType, protocolService.getClass().getSimpleName());
        protocolServices.put(protocolType, protocolService);

        // 设置全局事件监听器
        if (globalEventListener != null) {
            protocolService.setEventListener(globalEventListener);
        }
    }

    @Override
    public IProtocolService getProtocolService(ProtocolType protocolType) {
        IProtocolService service = protocolServices.get(protocolType);
        if (service == null) {
            throw new IllegalArgumentException("Protocol service not found for type: " + protocolType);
        }
        return service;
    }

    @Override
    public ProtocolType getProtocolType(String deviceCode) {
        ProtocolType protocolType = deviceProtocolTypes.get(deviceCode);
        if (protocolType == null) {
            log.warn("Protocol type not found for device: {}, using default: {}", deviceCode, protocolProperties.getDefaultProtocol());
            return protocolProperties.getDefaultProtocol();
        }
        return protocolType;
    }

    @Override
    public void setDeviceProtocolType(String deviceCode, ProtocolType protocolType) {
        log.info("Setting protocol type for device {}: {}", deviceCode, protocolType);
        deviceProtocolTypes.put(deviceCode, protocolType);
    }

    @Override
    public ProtocolResponse handleRequest(ProtocolRequest request) {
        if (request == null) {
            return ProtocolResponse.failure("400", "Request cannot be null");
        }

        ProtocolType protocolType = request.getProtocolType();
        if (protocolType == null) {
            protocolType = getProtocolType(request.getDeviceCode());
            request.setProtocolType(protocolType);
        }

        try {
            IProtocolService protocolService = getProtocolService(protocolType);
            return protocolService.handleRequest(request);
        } catch (Exception e) {
            log.error("Error handling protocol request: {}", request, e);
            return ProtocolResponse.failure("500", "Internal server error: " + e.getMessage());
        }
    }

    @Override
    public boolean connect(String deviceCode, ProtocolType protocolType) {
        log.info("Connecting device {} with protocol {}", deviceCode, protocolType);

        try {
            IProtocolService protocolService = getProtocolService(protocolType);
            boolean connected = protocolService.connect(deviceCode, protocolType);

            if (connected) {
                deviceProtocolTypes.put(deviceCode, protocolType);
                deviceConnectionStatus.put(deviceCode, true);
                log.info("Device {} connected successfully with protocol {}", deviceCode, protocolType);
            } else {
                log.warn("Failed to connect device {} with protocol {}", deviceCode, protocolType);
            }

            return connected;
        } catch (Exception e) {
            log.error("Error connecting device {} with protocol {}", deviceCode, protocolType, e);
            deviceConnectionStatus.put(deviceCode, false);
            return false;
        }
    }

    @Override
    public void disconnect(String deviceCode) {
        log.info("Disconnecting device: {}", deviceCode);

        ProtocolType protocolType = getProtocolType(deviceCode);
        try {
            IProtocolService protocolService = getProtocolService(protocolType);
            protocolService.disconnect(deviceCode, protocolType);
            deviceConnectionStatus.put(deviceCode, false);
            log.info("Device {} disconnected successfully", deviceCode);
        } catch (Exception e) {
            log.error("Error disconnecting device {}", deviceCode, e);
        }
    }

    @Override
    public List<ProtocolType> getRegisteredProtocolTypes() {
        return List.copyOf(protocolServices.keySet());
    }

    @Override
    public boolean isDeviceConnected(String deviceCode) {
        return deviceConnectionStatus.getOrDefault(deviceCode, false);
    }

    @Override
    public int getConnectedDeviceCount(ProtocolType protocolType) {
        return (int) deviceConnectionStatus.entrySet().stream()
                .filter(entry -> entry.getValue() && protocolType.equals(getProtocolType(entry.getKey())))
                .count();
    }

    @Override
    public void setEventListener(ProtocolEventListener listener) {
        this.globalEventListener = listener;

        // 为所有已注册的协议服务设置监听器
        protocolServices.values().forEach(service -> service.setEventListener(listener));
    }

    /**
     * 获取协议统计信息
     */
    public Map<ProtocolType, Integer> getProtocolStatistics() {
        Map<ProtocolType, Integer> stats = new ConcurrentHashMap<>();
        protocolServices.keySet().forEach(type ->
            stats.put(type, getConnectedDeviceCount(type))
        );
        return stats;
    }

    /**
     * 获取所有设备状态
     */
    public Map<String, Boolean> getAllDeviceStatus() {
        return Map.copyOf(deviceConnectionStatus);
    }

    /**
     * 清理断开连接的设备
     */
    public void cleanupDisconnectedDevices() {
        deviceConnectionStatus.entrySet().removeIf(entry -> {
            if (!entry.getValue()) {
                log.info("Removing disconnected device from registry: {}", entry.getKey());
                deviceProtocolTypes.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}