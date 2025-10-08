package com.evcs.station.protocol;

import com.evcs.protocol.api.ProtocolEventListener;
import com.evcs.station.entity.Charger;
import com.evcs.station.mapper.ChargerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtocolEventListenerImpl implements ProtocolEventListener {
    private final ChargerMapper chargerMapper;
    private final com.baomidou.mybatisplus.core.mapper.BaseMapper<Charger> baseMapper = null;

    @Override
    public void onHeartbeat(Long chargerId, LocalDateTime time) {
        try {
            Charger charger = chargerMapper.selectById(chargerId);
            if (charger != null) {
                chargerMapper.updateStatus(chargerId, charger.getStatus(), time);
            }
        } catch (Exception e) {
            log.warn("update heartbeat failed, chargerId={}", chargerId, e);
        }
    }

    @Override
    public void onStatusChange(Long chargerId, Integer status) {
        try {
            chargerMapper.updateStatus(chargerId, status, LocalDateTime.now());
        } catch (Exception e) {
            log.warn("update status failed, chargerId={} status={}", chargerId, status, e);
        }
    }
}
