package com.evcs.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.order.entity.BillingPlan;
import com.evcs.order.entity.BillingPlanSegment;

import java.util.List;

public interface IBillingPlanService extends IService<BillingPlan> {
    /**
     * 计划写入结果：业务规则（每站点启用上限/默认计划唯一）冲突时 success=false 并携带 error。
     */
    record PlanWriteOutcome(boolean success, String error, BillingPlan plan) {
        public static PlanWriteOutcome ok(BillingPlan plan) {
            return new PlanWriteOutcome(true, null, plan);
        }

        public static PlanWriteOutcome violated(String error) {
            return new PlanWriteOutcome(false, error, null);
        }
    }

    /**
     * 创建计费计划，落库并执行业务规则：
     * 每站点启用中的计划不超过 16 个；默认计划在同站点内唯一。
     */
    PlanWriteOutcome createPlan(BillingPlan plan);

    /**
     * 更新计费计划，落库并执行与创建相同的业务规则，随后失效相关缓存。
     */
    PlanWriteOutcome updatePlan(BillingPlan plan);

    /**
     * 将指定计划设为同站点默认（取消其他默认）。
     */
    PlanWriteOutcome setDefaultPlan(Long planId, Long stationId);

    BillingPlan getChargerPlan(Long chargerId, Long stationId);
    List<BillingPlanSegment> listSegments(Long planId);
    boolean saveSegments(Long planId, List<BillingPlanSegment> segments, boolean requireFullDay);
    boolean assignPlanToCharger(Long chargerId, Long planId);
    BillingPlan clonePlan(Long sourcePlanId, BillingPlan newPlan);
    boolean validateSegments(java.util.List<BillingPlanSegment> segments, boolean requireFullDay);
    void evictCache(Long planId);

    /**
     * 填充计划的统计信息（分段数、站点数）
     * @param plans 计划列表
     */
    void fillPlanStats(List<BillingPlan> plans);
}
