package com.evcs.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.order.entity.BillingRate;

public interface IBillingRateService extends IService<BillingRate> {
    BillingRate getEffectiveRate(Long stationId);
}
