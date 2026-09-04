package com.evcs.protocol.api.impl;

import com.evcs.protocol.api.ProtocolEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 基础事件监听实现：仅记录日志，保证 Spring 注入存在。
 */
@Component
public class ProtocolEventListenerLoggingImpl implements ProtocolEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProtocolEventListenerLoggingImpl.class);

    @Override
    public void onHeartbeat(Long chargerId, LocalDateTime time) {
        log.debug("Protocol heartbeat chargerId={} time={}", chargerId, time);
    }

    @Override
    public void onStatusChange(Long chargerId, Integer status) {
        log.debug("Protocol status change chargerId={} status={}", chargerId, status);
    }

    @Override
    public void onStartAck(Long chargerId, String sessionId, boolean success, String message) {
        log.info("Start ACK chargerId={} sessionId={} success={} message={}", chargerId, sessionId, success, message);
    }

    @Override
    public void onStopAck(Long chargerId, boolean success, String message) {
        log.info("Stop ACK chargerId={} success={} message={}", chargerId, success, message);
    }

    @Override
    public void onError(Long chargerId, String code, String message) {
        log.warn("Protocol error chargerId={} code={} message={}", chargerId, code, message);
    }
}

