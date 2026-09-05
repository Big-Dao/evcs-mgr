package com.evcs.protocol.service;

import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.protocol.client.StationServiceClient;
import com.evcs.protocol.dto.ChargerBasicInfo;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.protocol.mq.ProtocolEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CloudChargeProtocolServiceImpl 租户归属安全测试。
 *
 * <p>充电事件必须携带从 station 服务解析出的真实租户 ID；
 * 解析失败时必须 fail-closed（拒绝发布），禁止兜底写入固定租户，
 * 否则充电事件会被错误归属到其他租户（如平台租户 1）。
 */
class CloudChargeProtocolServiceImplTest {

    private ProtocolEventPublisher publisher;
    private StationServiceClient stationServiceClient;
    private RecordingListener listener;
    private CloudChargeProtocolServiceImpl service;

    @BeforeEach
    void setUp() {
        publisher = mock(ProtocolEventPublisher.class);
        stationServiceClient = mock(StationServiceClient.class);
        listener = new RecordingListener();
        service = new CloudChargeProtocolServiceImpl(publisher, stationServiceClient);
        service.setEventListener(listener);
    }

    private ChargerBasicInfo chargerInfo(Long tenantId) {
        ChargerBasicInfo info = new ChargerBasicInfo();
        info.setId(1L);
        info.setTenantId(tenantId);
        info.setStationId(11L);
        info.setChargerCode("DEVICE_1");
        info.setChargerName("1号桩");
        return info;
    }

    @Test
    @DisplayName("开始充电 - 解析成功时应携带解析出的租户ID发布事件")
    void startChargingShouldPublishWithResolvedTenant() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(chargerInfo(5L));

        assertTrue(service.startCharging(1L, "SESSION_1", 100L));

        verify(publisher).publishChargingStart(
            eq(11L), eq(1L), eq(5L), eq("CloudCharge"), eq("SESSION_1"), eq(100L),
            isNull(), isNull(), eq(0.0), eq(true), anyString());
        assertTrue(listener.startAcks.get(0).success());
    }

    @Test
    @DisplayName("开始充电 - 充电器解析失败时应拒绝且不发布事件（禁止兜底租户）")
    void startChargingShouldFailClosedWhenChargerUnresolvable() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(null);

        assertFalse(service.startCharging(1L, "SESSION_1", 100L),
            "解析不到充电器信息时应返回失败");

        verify(publisher, never()).publishChargingStart(
            anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyLong(),
            any(), anyString(), anyDouble(), anyBoolean(), anyString());
        assertFalse(listener.startAcks.get(0).success());
    }

    @Test
    @DisplayName("开始充电 - 解析结果缺租户ID时应拒绝且不发布事件")
    void startChargingShouldFailClosedWhenTenantMissing() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(chargerInfo(null));

        assertFalse(service.startCharging(1L, "SESSION_1", 100L),
            "解析结果缺少租户ID时应返回失败");

        verify(publisher, never()).publishChargingStart(
            anyLong(), anyLong(), anyLong(), anyString(), anyString(), anyLong(),
            any(), anyString(), anyDouble(), anyBoolean(), anyString());
    }

    @Test
    @DisplayName("停止充电 - 应携带解析出的租户ID发布事件")
    void stopChargingShouldPublishWithResolvedTenant() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(chargerInfo(5L));

        assertTrue(service.stopCharging(1L));

        verify(publisher).publishChargingStop(
            eq(1L), eq(5L), isNull(), eq("CloudCharge"), any(), any(), anyDouble(), anyLong(),
            anyString(), anyBoolean(), anyString());
    }

    @Test
    @DisplayName("停止充电 - 充电器解析失败时应拒绝且不发布事件（禁止兜底租户）")
    void stopChargingShouldFailClosedWhenChargerUnresolvable() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(null);

        assertFalse(service.stopCharging(1L), "解析不到充电器信息时应返回失败");

        verify(publisher, never()).publishChargingStop(
            anyLong(), anyLong(), any(), anyString(), any(), any(), anyDouble(), anyLong(),
            anyString(), anyBoolean(), anyString());
        assertFalse(listener.stopAcks.get(0).success());
    }

    @Test
    @DisplayName("心跳 - 充电器解析失败时不得发布事件（禁止兜底租户）")
    void heartbeatShouldNotPublishWhenChargerUnresolvable() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(null);

        assertFalse(service.reportHeartbeat(1L));

        verify(publisher, never()).publishHeartbeat(anyLong(), anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("心跳 - 解析成功时应携带解析出的租户ID发布事件")
    void heartbeatShouldPublishWithResolvedTenant() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(chargerInfo(5L));

        assertTrue(service.reportHeartbeat(1L));

        verify(publisher).publishHeartbeat(eq(1L), eq(5L), eq("CloudCharge"), any());
    }

    @Test
    @DisplayName("状态上报 - 充电器解析失败时不得发布事件（禁止兜底租户）")
    void statusShouldNotPublishWhenChargerUnresolvable() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(null);

        assertFalse(service.reportStatus(1L, 3));

        verify(publisher, never()).publishStatusChange(
            anyLong(), anyLong(), anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("状态上报 - 解析成功时应携带解析出的租户ID发布事件")
    void statusShouldPublishWithResolvedTenant() {
        when(stationServiceClient.getChargerById(1L)).thenReturn(chargerInfo(5L));

        assertTrue(service.reportStatus(1L, 3));

        verify(publisher).publishStatusChange(
            eq(1L), eq(5L), eq("CloudCharge"), any(), eq(3), anyString());
    }

    private static final class RecordingListener implements ProtocolEventListener {
        private final List<Ack> startAcks = new ArrayList<>();
        private final List<Ack> stopAcks = new ArrayList<>();

        record Ack(boolean success, String message) {
        }

        @Override
        public void onStartAck(Long chargerId, String sessionId, boolean success, String message) {
            startAcks.add(new Ack(success, message));
        }

        @Override
        public void onStopAck(Long chargerId, boolean success, String message) {
            stopAcks.add(new Ack(success, message));
        }
    }
}
