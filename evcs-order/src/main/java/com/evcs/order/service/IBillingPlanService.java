package com.evcs.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.order.entity.BillingPlan;
import com.evcs.order.entity.BillingPlanSegment;

import java.util.List;

public interface IBillingPlanService extends IService<BillingPlan> {
    BillingPlan getChargerPlan(Long chargerId, Long stationId);
    List<BillingPlanSegment> listSegments(Long planId);
    boolean saveSegments(Long planId, List<BillingPlanSegment> segments, boolean requireFullDay);
    boolean assignPlanToCharger(Long chargerId, Long planId);
    BillingPlan clonePlan(Long sourcePlanId, BillingPlan newPlan);
    boolean validateSegments(java.util.List<BillingPlanSegment> segments, boolean requireFullDay);
    void evictCache(Long planId);
}
