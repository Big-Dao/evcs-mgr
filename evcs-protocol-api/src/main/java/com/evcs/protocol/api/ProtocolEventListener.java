package com.evcs.protocol.api;

import java.time.LocalDateTime;

public interface ProtocolEventListener {
    default void onHeartbeat(Long chargerId, LocalDateTime time) {}
    default void onStatusChange(Long chargerId, Integer status) {}
    default void onStartAck(Long chargerId, String sessionId, boolean success, String message) {}
    default void onStopAck(Long chargerId, boolean success, String message) {}
    default void onError(Long chargerId, String code, String message) {}
}
