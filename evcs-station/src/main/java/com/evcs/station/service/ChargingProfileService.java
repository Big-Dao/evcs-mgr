package com.evcs.station.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.station.entity.ChargingProfile;

public interface ChargingProfileService extends IService<ChargingProfile> {

    /**
     * 下发充电策略到充电桩
     * @param profileId 策略ID
     */
    void applyProfile(Long profileId);
}
