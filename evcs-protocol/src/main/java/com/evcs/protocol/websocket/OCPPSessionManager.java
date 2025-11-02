package com.evcs.protocol.websocket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OCPP会话管理器
 * 负责管理所有OCPP WebSocket会话的生命周期
 */
@Slf4j
@Component
public class OCPPSessionManager {

    /**
     * 活跃会话映射 (chargerCode -> session)
     */
    private final ConcurrentHashMap<String, OCPPWebSocketSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * WebSocket会话映射 (sessionId -> session)
     */
    private final ConcurrentHashMap<String, OCPPWebSocketSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 最大会话数
     */
    private final int maxSessions = 1000;

    public OCPPSessionManager() {
        // 启动会话清理任务
        startSessionCleanupTask();
        // 启动心跳检测任务
        startHeartbeatCheckTask();
    }

    /**
     * 添加新会话
     */
    public boolean addSession(OCPPWebSocketSession session) {
        if (session == null || session.getChargerCode() == null) {
            log.warn("Invalid session: session or charger code is null");
            return false;
        }

        if (activeSessions.size() >= maxSessions) {
            log.warn("Maximum session limit reached: {}", maxSessions);
            return false;
        }

        String chargerCode = session.getChargerCode();
        String sessionId = session.getSessionId();

        // 检查是否已存在会话
        OCPPWebSocketSession existingSession = activeSessions.get(chargerCode);
        if (existingSession != null && existingSession.isActive()) {
            log.warn("Session already exists for charger: {}, closing old session", chargerCode);
            removeSession(chargerCode);
        }

        activeSessions.put(chargerCode, session);
        if (sessionId != null) {
            sessionMap.put(sessionId, session);
        }

        session.setStatus(OCPPWebSocketSession.SessionStatus.CONNECTED);
        log.info("Session added: charger={}, sessionId={}, totalSessions={}",
                chargerCode, sessionId, activeSessions.size());

        return true;
    }

    /**
     * 移除会话
     */
    public boolean removeSession(String chargerCode) {
        OCPPWebSocketSession session = activeSessions.remove(chargerCode);
        if (session != null) {
            String sessionId = session.getSessionId();
            if (sessionId != null) {
                sessionMap.remove(sessionId);
            }

            session.setStatus(OCPPWebSocketSession.SessionStatus.DISCONNECTED);
            session.close();

            log.info("Session removed: charger={}, sessionId={}, totalSessions={}",
                    chargerCode, sessionId, activeSessions.size());
            return true;
        }
        return false;
    }

    /**
     * 根据充电站编码获取会话
     */
    public OCPPWebSocketSession getSession(String chargerCode) {
        return activeSessions.get(chargerCode);
    }

    /**
     * 根据WebSocket会话ID获取会话
     */
    public OCPPWebSocketSession getSessionByWebSocketId(String webSocketSessionId) {
        return sessionMap.get(webSocketSessionId);
    }

    /**
     * 获取所有活跃会话
     */
    public Collection<OCPPWebSocketSession> getAllSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    /**
     * 获取所有活跃充电站编码
     */
    public Set<String> getActiveChargerCodes() {
        return new HashSet<>(activeSessions.keySet());
    }

    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * 检查充电站是否在线
     */
    public boolean isChargerOnline(String chargerCode) {
        OCPPWebSocketSession session = activeSessions.get(chargerCode);
        return session != null && session.isActive();
    }

    /**
     * 更新会话活跃时间
     */
    public void updateSessionActivity(String chargerCode) {
        OCPPWebSocketSession session = activeSessions.get(chargerCode);
        if (session != null) {
            session.updateLastActiveTime();
            session.incrementMessageCount();
        }
    }

    /**
     * 获取会话统计信息
     */
    public List<OCPPWebSocketSession.SessionStatistics> getSessionStatistics() {
        return activeSessions.values().stream()
                .map(OCPPWebSocketSession::getStatistics)
                .collect(Collectors.toList());
    }

    /**
     * 获取会话统计摘要
     */
    public SessionStatisticsSummary getStatisticsSummary() {
        int totalSessions = activeSessions.size();
        long activeSessionCount = activeSessions.values().stream()
                .mapToLong(session -> session.isActive() ? 1 : 0)
                .sum();
        long totalMessages = activeSessions.values().stream()
                .mapToLong(OCPPWebSocketSession::getMessageCount)
                .sum();

        Map<OCPPWebSocketSession.SessionStatus, Long> statusCounts = activeSessions.values().stream()
                .collect(Collectors.groupingBy(
                        OCPPWebSocketSession::getStatus,
                        Collectors.counting()
                ));

        return new SessionStatisticsSummary(totalSessions, activeSessionCount, totalMessages, statusCounts);
    }

    /**
     * 启动会话清理任务
     */
    private void startSessionCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupInactiveSessions();
            } catch (Exception e) {
                log.error("Error in session cleanup task", e);
            }
        }, 5, 5, TimeUnit.MINUTES); // 每5分钟执行一次
    }

    /**
     * 启动心跳检测任务
     */
    private void startHeartbeatCheckTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkHeartbeats();
            } catch (Exception e) {
                log.error("Error in heartbeat check task", e);
            }
        }, 1, 1, TimeUnit.MINUTES); // 每分钟执行一次
    }

    /**
     * 清理非活跃会话
     */
    private void cleanupInactiveSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30); // 30分钟未活跃的会话

        List<String> inactiveChargers = activeSessions.entrySet().stream()
                .filter(entry -> {
                    OCPPWebSocketSession session = entry.getValue();
                    return !session.isActive() || session.getLastActiveTime().isBefore(cutoffTime);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String chargerCode : inactiveChargers) {
            log.info("Cleaning up inactive session: {}", chargerCode);
            removeSession(chargerCode);
        }

        if (!inactiveChargers.isEmpty()) {
            log.info("Session cleanup completed: removed {} inactive sessions", inactiveChargers.size());
        }
    }

    /**
     * 检查心跳
     */
    private void checkHeartbeats() {
        List<String> missedHeartbeatChargers = activeSessions.values().stream()
                .filter(OCPPWebSocketSession::needsHeartbeat)
                .map(OCPPWebSocketSession::getChargerCode)
                .collect(Collectors.toList());

        for (String chargerCode : missedHeartbeatChargers) {
            OCPPWebSocketSession session = activeSessions.get(chargerCode);
            if (session != null) {
                long idleTime = session.getIdleTimeSeconds();
                log.warn("Charger {} missed heartbeat, idle time: {}s", chargerCode, idleTime);

                // 如果超过3个心跳周期未响应，断开连接
                if (idleTime > (session.getHeartbeatInterval() * 3)) {
                    log.error("Charger {} heartbeat timeout, disconnecting", chargerCode);
                    session.setStatus(OCPPWebSocketSession.SessionStatus.ERROR);
                    removeSession(chargerCode);
                }
            }
        }
    }

    /**
     * 关闭所有会话
     */
    public void shutdown() {
        log.info("Shutting down session manager, closing {} sessions", activeSessions.size());

        // 关闭所有会话
        activeSessions.values().forEach(session -> {
            try {
                session.close();
            } catch (Exception e) {
                log.error("Error closing session for charger: {}", session.getChargerCode(), e);
            }
        });

        // 清空映射
        activeSessions.clear();
        sessionMap.clear();

        // 关闭调度器
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Session manager shutdown completed");
    }

    /**
     * 会话统计摘要
     */
    @Data
    public static class SessionStatisticsSummary {
        private final int totalSessions;
        private final long activeSessions;
        private final long totalMessages;
        private final Map<OCPPWebSocketSession.SessionStatus, Long> statusCounts;
        private final LocalDateTime timestamp;

        public SessionStatisticsSummary(int totalSessions, long activeSessions, long totalMessages,
                                      Map<OCPPWebSocketSession.SessionStatus, Long> statusCounts) {
            this.totalSessions = totalSessions;
            this.activeSessions = activeSessions;
            this.totalMessages = totalMessages;
            this.statusCounts = statusCounts;
            this.timestamp = LocalDateTime.now();
        }

        @Override
        public String toString() {
            return String.format("SessionStatisticsSummary{total=%d, active=%d, messages=%d, status=%s, time=%s}",
                    totalSessions, activeSessions, totalMessages, statusCounts, timestamp);
        }
    }
}