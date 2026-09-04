package com.evcs.station.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.result.Result;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.station.client.ProtocolClient;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Firmware;
import com.evcs.station.entity.FirmwareUpgradeTask;
import com.evcs.station.mapper.FirmwareUpgradeTaskMapper;
import com.evcs.station.service.IChargerService;
import com.evcs.station.service.FirmwareService;
import com.evcs.station.service.FirmwareUpgradeTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirmwareUpgradeTaskServiceImpl extends ServiceImpl<FirmwareUpgradeTaskMapper, FirmwareUpgradeTask> implements FirmwareUpgradeTaskService {

    private final FirmwareService firmwareService;
    private final IChargerService chargerService;
    private final ProtocolClient protocolClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndStartTask(Long firmwareId, Long chargerId) {
        Firmware firmware = firmwareService.getById(firmwareId);
        if (firmware == null) {
            throw new RuntimeException("Firmware not found");
        }

        Charger charger = chargerService.getById(chargerId);
        if (charger == null) {
            throw new RuntimeException("Charger not found");
        }

        FirmwareUpgradeTask task = new FirmwareUpgradeTask();
        task.setFirmwareId(firmwareId);
        task.setChargerId(chargerId);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setStartTime(LocalDateTime.now());
        save(task);

        startUpgrade(task, firmware, charger);

        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Long taskId) {
        FirmwareUpgradeTask task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found");
        }

        Firmware firmware = firmwareService.getById(task.getFirmwareId());
        Charger charger = chargerService.getById(task.getChargerId());

        task.setRetryCount(task.getRetryCount() + 1);
        task.setStatus("PENDING");
        task.setErrorMessage(null);
        updateById(task);

        startUpgrade(task, firmware, charger);
    }

    private void startUpgrade(FirmwareUpgradeTask task, Firmware firmware, Charger charger) {
        try {
            ProtocolRequest request = new ProtocolRequest();
            request.setDeviceCode(charger.getChargerCode());
            request.setAction("updateFirmware");

            Map<String, Object> data = new HashMap<>();
            data.put("location", firmware.getUrl());
            data.put("retrieveDate", LocalDateTime.now().toString()); // Should be formatted properly
            // data.put("retries", 3);
            // data.put("retryInterval", 60);
            request.setData(data);

            Result<ProtocolResponse> result = protocolClient.updateFirmware(request);
            if (result.isSuccess() && result.getData().isSuccess()) {
                task.setStatus("DOWNLOADING");
            } else {
                task.setStatus("FAILED");
                task.setErrorMessage(result.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to start firmware upgrade task: {}", task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
        }
        updateById(task);
    }
}
