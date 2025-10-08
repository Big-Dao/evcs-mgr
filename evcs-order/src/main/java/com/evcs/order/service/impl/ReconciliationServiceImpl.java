package com.evcs.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.mapper.ChargingOrderMapper;
import com.evcs.order.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {
    private final ChargingOrderMapper orderMapper;

    @Override
    @DataScope
    public ReconcileResult runDailyCheck() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(1).atStartOfDay();
        LocalDateTime end = today.atStartOfDay();
        var qw = new QueryWrapper<ChargingOrder>()
                .eq("tenant_id", TenantContext.getCurrentTenantId())
                .ge("create_time", start)
                .lt("create_time", end);
        var list = orderMapper.selectList(qw);
        ReconcileResult r = new ReconcileResult();
        r.total = list.size();
        for (ChargingOrder o : list) {
            boolean bad = false;
            if (o.getStartTime() != null && o.getEndTime() != null && !o.getEndTime().isAfter(o.getStartTime())) {
                r.invalidTimeRange++;
                bad = true;
            }
            if (o.getEndTime() == null && (o.getStatus() != null && o.getStatus() >= ChargingOrder.STATUS_COMPLETED)) {
                r.missingEndTime++;
                bad = true;
            }
            if (o.getAmount() != null && o.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                r.negativeAmount++;
                bad = true;
            }
            if (bad) r.needAttention++;
        }
        return r;
    }
}
