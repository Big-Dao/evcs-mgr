package com.evcs.station.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.result.Result;
import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.station.client.ProtocolClient;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.ChargingProfile;
import com.evcs.station.mapper.ChargingProfileMapper;
import com.evcs.station.service.IChargerService;
import com.evcs.station.service.ChargingProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargingProfileServiceImpl extends ServiceImpl<ChargingProfileMapper, ChargingProfile> implements ChargingProfileService {

    private final IChargerService chargerService;
    private final ProtocolClient protocolClient;

    @Override
    public void applyProfile(Long profileId) {
        ChargingProfile profile = getById(profileId);
        if (profile == null) {
            throw new RuntimeException("Profile not found");
        }

        Charger charger = chargerService.getById(profile.getChargerId());
        if (charger == null) {
            throw new RuntimeException("Charger not found");
        }

        try {
            ProtocolRequest request = new ProtocolRequest();
            request.setDeviceCode(charger.getChargerCode());
            request.setAction("setChargingProfile");

            Map<String, Object> data = new HashMap<>();
            data.put("connectorId", profile.getConnectorId());
            data.put("csChargingProfiles", buildOcppProfile(profile));
            request.setData(data);

            Result<ProtocolResponse> result = protocolClient.setChargingProfile(request);
            if (!result.isSuccess() || !result.getData().isSuccess()) {
                throw new RuntimeException("Failed to apply profile: " + (result.getData() != null ? result.getData().getMessage() : result.getMessage()));
            }
        } catch (Exception e) {
            log.error("Error applying charging profile: {}", profileId, e);
            throw new RuntimeException("Error applying charging profile: " + e.getMessage());
        }
    }

    private Map<String, Object> buildOcppProfile(ChargingProfile profile) {
        Map<String, Object> ocppProfile = new HashMap<>();
        ocppProfile.put("chargingProfileId", profile.getId());
        ocppProfile.put("stackLevel", profile.getStackLevel());
        ocppProfile.put("chargingProfilePurpose", profile.getPurpose());
        ocppProfile.put("chargingProfileKind", profile.getKind());
        if (profile.getValidFrom() != null) {
            ocppProfile.put("validFrom", profile.getValidFrom().toString());
        }
        if (profile.getValidTo() != null) {
            ocppProfile.put("validTo", profile.getValidTo().toString());
        }

        Map<String, Object> schedule = new HashMap<>();
        // Simplified schedule: one period with the limit
        Map<String, Object> period = new HashMap<>();
        period.put("startPeriod", 0);
        period.put("limit", profile.getLimitKw()); // Assuming limit is in kW or Amps depending on unit. OCPP usually uses Amps or Watts.
        // For simplicity, let's assume the limitKw is actually Amps or we convert it.
        // Standard OCPP ChargingSchedulePeriod limit is decimal.
        // ChargingSchedule also needs chargingRateUnit (A or W).
        schedule.put("chargingRateUnit", "W"); // Using Watts for kW input
        schedule.put("chargingSchedulePeriod", java.util.Collections.singletonList(period));

        // Convert kW to Watts
        if (profile.getLimitKw() != null) {
             period.put("limit", profile.getLimitKw().multiply(new java.math.BigDecimal(1000)));
        }

        ocppProfile.put("chargingSchedule", schedule);
        return ocppProfile;
    }
}
