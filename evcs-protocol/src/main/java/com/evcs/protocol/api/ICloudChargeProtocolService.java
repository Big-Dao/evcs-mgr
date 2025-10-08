package com.evcs.protocol.api;

public interface ICloudChargeProtocolService {
    boolean registerStation(String stationCode);
    boolean startCharging(Long chargerId, String sessionId, Long userId);
    boolean stopCharging(Long chargerId);
    boolean reportHeartbeat(Long chargerId);
    boolean reportStatus(Long chargerId, Integer status);

    void setEventListener(ProtocolEventListener listener);
}
