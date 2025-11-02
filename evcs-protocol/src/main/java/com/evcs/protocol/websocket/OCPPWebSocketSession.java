package com.evcs.protocol.websocket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OCPP WebSocket会话管理
 * 封装WebSocket会话和相关的OCPP协议信息
 */
@Slf4j
@Data
public class OCPPWebSocketSession {

    /**
     * WebSocket会话
     */
    private WebSocketSession webSocketSession;

    /**
     * 充电站标识
     */
    private String chargerCode;

    /**
     * 会话创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 心跳间隔（秒）
     */
    private Integer heartbeatInterval;

    /**
     * 消息计数器
     */
    private final AtomicLong messageCount = new AtomicLong(0);

    /**
     * 会话状态
     */
    private SessionStatus status = SessionStatus.CONNECTING;

    /**
     * 扩展属性
     */
    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * 会话状态枚举
     */
    public enum SessionStatus {
        CONNECTING,    // 连接中
        CONNECTED,     // 已连接
        AUTHENTICATED, // 已认证
        DISCONNECTED,  // 已断开
        ERROR          // 错误状态
    }

    public OCPPWebSocketSession(WebSocketSession webSocketSession, String chargerCode) {
        this.webSocketSession = webSocketSession;
        this.chargerCode = chargerCode;
        this.createTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 更新活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 增加消息计数
     */
    public long incrementMessageCount() {
        return messageCount.incrementAndGet();
    }

    /**
     * 获取消息计数
     */
    public long getMessageCount() {
        return messageCount.get();
    }

    /**
     * 检查会话是否活跃
     */
    public boolean isActive() {
        return webSocketSession != null && webSocketSession.isOpen() &&
               (status == SessionStatus.CONNECTED || status == SessionStatus.AUTHENTICATED);
    }

    /**
     * 检查是否需要心跳
     */
    public boolean needsHeartbeat() {
        if (heartbeatInterval == null || heartbeatInterval <= 0) {
            return false;
        }

        LocalDateTime nextHeartbeatTime = lastActiveTime.plusSeconds(heartbeatInterval);
        return LocalDateTime.now().isAfter(nextHeartbeatTime);
    }

    /**
     * 获取会话ID
     */
    public String getSessionId() {
        return webSocketSession != null ? webSocketSession.getId() : null;
    }

    /**
     * 获取远程地址
     */
    public String getRemoteAddress() {
        if (webSocketSession == null) {
            return null;
        }
        return webSocketSession.getRemoteAddress() != null ?
               webSocketSession.getRemoteAddress().toString() : "unknown";
    }

    /**
     * 设置属性
     */
    public void setAttribute(String key, Object value) {
        if (value != null) {
            attributes.put(key, value);
        } else {
            attributes.remove(key);
        }
    }

    /**
     * 获取属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 获取属性（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type, T defaultValue) {
        T value = getAttribute(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * 移除属性
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * 检查属性是否存在
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * 清除所有属性
     */
    public void clearAttributes() {
        attributes.clear();
    }

    /**
     * 获取会话运行时长（秒）
     */
    public long getUptimeSeconds() {
        return java.time.Duration.between(createTime, LocalDateTime.now()).getSeconds();
    }

    /**
     * 获取空闲时长（秒）
     */
    public long getIdleTimeSeconds() {
        return java.time.Duration.between(lastActiveTime, LocalDateTime.now()).getSeconds();
    }

    /**
     * 关闭会话
     */
    public void close() {
        try {
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.close();
                log.info("WebSocket session closed for charger: {}", chargerCode);
            }
        } catch (Exception e) {
            log.error("Error closing WebSocket session for charger: {}", chargerCode, e);
        } finally {
            status = SessionStatus.DISCONNECTED;
        }
    }

    @Override
    public String toString() {
        return String.format("OCPPWebSocketSession{chargerCode='%s', sessionId='%s', status=%s, " +
                        "messageCount=%d, uptime=%ds, idle=%ds}",
                chargerCode, getSessionId(), status, getMessageCount(),
                getUptimeSeconds(), getIdleTimeSeconds());
    }

    /**
     * 获取会话统计信息
     */
    public SessionStatistics getStatistics() {
        return new SessionStatistics(
            chargerCode,
            getSessionId(),
            status,
            createTime,
            lastActiveTime,
            getMessageCount(),
            getUptimeSeconds(),
            getIdleTimeSeconds(),
            getRemoteAddress()
        );
    }

    /**
     * 会话统计信息
     */
    @Data
    public static class SessionStatistics {
        private final String chargerCode;
        private final String sessionId;
        private final SessionStatus status;
        private final LocalDateTime createTime;
        private final LocalDateTime lastActiveTime;
        private final long messageCount;
        private final long uptimeSeconds;
        private final long idleTimeSeconds;
        private final String remoteAddress;

        public SessionStatistics(String chargerCode, String sessionId, SessionStatus status,
                                LocalDateTime createTime, LocalDateTime lastActiveTime,
                                long messageCount, long uptimeSeconds, long idleTimeSeconds,
                                String remoteAddress) {
            this.chargerCode = chargerCode;
            this.sessionId = sessionId;
            this.status = status;
            this.createTime = createTime;
            this.lastActiveTime = lastActiveTime;
            this.messageCount = messageCount;
            this.uptimeSeconds = uptimeSeconds;
            this.idleTimeSeconds = idleTimeSeconds;
            this.remoteAddress = remoteAddress;
        }
    }
}