package com.evcs.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.order.entity.BillingRate;
import com.evcs.order.mapper.BillingRateMapper;
import com.evcs.order.service.IBillingRateService;
import org.springframework.stereotype.Service;

@Service
public class BillingRateServiceImpl extends ServiceImpl<BillingRateMapper, BillingRate> implements IBillingRateService {
    @Override
    @DataScope
    public BillingRate getEffectiveRate(Long stationId) {
        if (stationId != null) {
            BillingRate stationRate = this.getOne(new QueryWrapper<BillingRate>()
                    .eq("station_id", stationId)
                    .eq("status", 1)
                    .orderByDesc("id").last("limit 1"));
            if (stationRate != null) return stationRate;
        }
        return this.getOne(new QueryWrapper<BillingRate>()
                .isNull("station_id")
                .eq("status", 1)
                .orderByDesc("id").last("limit 1"));
    }
}
