package com.evcs.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.order.entity.BillingPlan;
import com.evcs.order.entity.BillingPlanSegment;
import com.evcs.order.mapper.BillingPlanMapper;
import com.evcs.order.mapper.BillingPlanSegmentMapper;
import com.evcs.order.service.IBillingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BillingPlanServiceImpl extends ServiceImpl<BillingPlanMapper, BillingPlan> implements IBillingPlanService {
    private final Map<Long, List<BillingPlanSegment>> segmentCache = new ConcurrentHashMap<>();
    private final BillingPlanSegmentMapper segmentMapper;

    @Override
    @DataScope
    public BillingPlan getChargerPlan(Long chargerId, Long stationId) {
        // 简化：根据站点默认，再回退到全局默认（station_id 为空）
        java.time.LocalDate today = java.time.LocalDate.now();
        BillingPlan stationDefault = this.getOne(new QueryWrapper<BillingPlan>()
                .eq("tenant_id", TenantContext.getCurrentTenantId())
                .eq("station_id", stationId)
                .eq("status", 1)
                .and(q -> q.isNull("effective_start_date").or().le("effective_start_date", today))
                .and(q -> q.isNull("effective_end_date").or().ge("effective_end_date", today))
                .orderByDesc("priority").orderByDesc("is_default").orderByDesc("id").last("limit 1"));
        if (stationDefault != null) return stationDefault;
        return this.getOne(new QueryWrapper<BillingPlan>()
                .eq("tenant_id", TenantContext.getCurrentTenantId())
                .isNull("station_id")
                .eq("status", 1)
                .and(q -> q.isNull("effective_start_date").or().le("effective_start_date", today))
                .and(q -> q.isNull("effective_end_date").or().ge("effective_end_date", today))
                .orderByDesc("priority").orderByDesc("is_default").orderByDesc("id").last("limit 1"));
    }


    @Override
    public boolean validateSegments(List<BillingPlanSegment> segments, boolean requireFullDay) {
        return validateNoOverlap(segments, requireFullDay);
    }

    @Override
    @DataScope
    public List<BillingPlanSegment> listSegments(Long planId) {
        return segmentCache.computeIfAbsent(planId, id ->
                segmentMapper.selectList(new QueryWrapper<BillingPlanSegment>().eq("plan_id", id).orderByAsc("segment_index"))
        );
    }

    @Override
    @DataScope
    public boolean saveSegments(Long planId, List<BillingPlanSegment> segments, boolean requireFullDay) {
        if (segments != null && segments.size() > 96) {
            throw new RuntimeException("分段数量不能超过96");
        }

        // 校验分段不重叠
        if (segments != null && !segments.isEmpty()) {
            boolean ok = validateNoOverlap(segments, requireFullDay);
            if (!ok) {
                throw new RuntimeException("分段存在时间重叠或非法时间");
            }
        }
        // 先删后插
        segmentCache.remove(planId);
        segmentMapper.delete(new QueryWrapper<BillingPlanSegment>().eq("plan_id", planId));
        int i = 1;
        for (BillingPlanSegment s : segments) {
            s.setPlanId(planId);
            if (s.getSegmentIndex() == null) s.setSegmentIndex(i++);
            segmentMapper.insert(s);
        }
        return true;
    }

    @Override
    @DataScope
    public BillingPlan clonePlan(Long sourcePlanId, BillingPlan newPlan) {
        BillingPlan src = this.getById(sourcePlanId);
        if (src == null) return null;

        // 限制同站点启用计划数量（如新计划启用）
        if (newPlan.getStatus() != null && newPlan.getStatus() == 1 && newPlan.getStationId() != null) {
            long cnt = this.count(new QueryWrapper<BillingPlan>()
                    .eq("tenant_id", src.getTenantId())
                    .eq("station_id", newPlan.getStationId())
                    .eq("status", 1));
            if (cnt >= 16) {
                throw new RuntimeException("每站点启用的计费计划不能超过16个");
            }
        }
        newPlan.setTenantId(src.getTenantId());
        this.save(newPlan);
        List<BillingPlanSegment> segs = listSegments(sourcePlanId);
        if (segs != null && !segs.isEmpty()) {
            for (BillingPlanSegment s : segs) {
                BillingPlanSegment cp = new BillingPlanSegment();
                cp.setPlanId(newPlan.getId());
        // 清除新计划缓存
        segmentCache.remove(newPlan.getId());
                cp.setSegmentIndex(s.getSegmentIndex());
                cp.setStartTime(s.getStartTime());
                cp.setEndTime(s.getEndTime());
                cp.setEnergyPrice(s.getEnergyPrice());
                cp.setServiceFee(s.getServiceFee());
                segmentMapper.insert(cp);
            }
        }
        return newPlan;
    }

    public boolean validateNoOverlap(List<BillingPlanSegment> segments, boolean requireFullDay) {
        if (segments == null) return !requireFullDay; // 允许空并非全天
        boolean[] used = new boolean[1440];
        for (BillingPlanSegment s : segments) {
            java.time.LocalTime st;
            java.time.LocalTime et;
            try {
                st = java.time.LocalTime.parse(s.getStartTime());
                et = java.time.LocalTime.parse(s.getEndTime());
            } catch (Exception e) {
                return false;
            }
            int si = st.getHour() * 60 + st.getMinute();
            int ei = et.getHour() * 60 + et.getMinute();
            if (si == ei) { // 0长度非法
                return false;
            }
            if (ei > si) {
                for (int m = si; m < ei; m++) {
                    if (used[m]) return false;
                    used[m] = true;
                }
            } else { // 跨午夜，拆两段
                for (int m = si; m < 1440; m++) {
                    if (used[m]) return false;
                    used[m] = true;
                }
                for (int m = 0; m < ei; m++) {
                    if (used[m]) return false;
                    used[m] = true;
                }
            }
        }
        return true;
    }

    @Override
    @DataScope
    public boolean assignPlanToCharger(Long chargerId, Long planId) {
        // 由 station 模块维护 charger 记录，这里仅提供接口供调用；实际赋值在控制器里调 station 的服务
        return true;
    }

    @Override
    public void evictCache(Long planId) {
        if (planId != null) {
            segmentCache.remove(planId);
        }
    }

}
