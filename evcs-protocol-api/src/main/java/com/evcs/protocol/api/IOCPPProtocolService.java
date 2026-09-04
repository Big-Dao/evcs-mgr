package com.evcs.protocol.api;

public interface IOCPPProtocolService {
    boolean connect(String chargerCode);
    void disconnect(String chargerCode);
    boolean sendHeartbeat(Long chargerId);
    boolean updateStatus(Long chargerId, Integer status);
    boolean startCharging(Long chargerId, String sessionId, Long userId);
    boolean stopCharging(Long chargerId);

    void setEventListener(ProtocolEventListener listener);
}
