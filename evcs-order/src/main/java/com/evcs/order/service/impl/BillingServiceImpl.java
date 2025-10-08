package com.evcs.order.service.impl;

import com.evcs.order.service.IBillingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.evcs.order.service.IBillingRateService;
import com.evcs.order.entity.BillingRate;
import com.evcs.order.service.IBillingPlanService;
import com.evcs.order.entity.BillingPlanSegment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements IBillingService {
    private final IBillingRateService rateService;
    private final IBillingPlanService planService;

    @Value("${billing.tou.enabled:false}")
    private boolean touEnabled;

    @Value("${billing.tou.peak_start:06:00}")
    private String peakStart;

    @Value("${billing.tou.peak_end:22:00}")
    private String peakEnd;

    @Value("${billing.tou.peak_price:1.50}")
    private BigDecimal peakPrice;

    @Value("${billing.tou.offpeak_price:0.80}")
    private BigDecimal offpeakPrice;

    @Value("${billing.service_fee:0.00}")
    private BigDecimal serviceFee;

    @Value("${billing.price_per_kwh:1.00}")
    private BigDecimal flatPrice;

    @Override
    public BigDecimal calculateAmount(LocalDateTime startTime, LocalDateTime endTime, Double energyKwh, Long stationId, Long chargerId, Long planId) {
        // 1) 优先使用显式指定的计费计划ID；否则按充电桩/站点默认
        var plan = (planId != null) ? planService.getById(planId) : planService.getChargerPlan(chargerId, stationId);
        if (plan != null) {
            List<BillingPlanSegment> segments = planService.listSegments(plan.getId());
            if (segments != null && !segments.isEmpty()) {
                return calculateBySegments(startTime, endTime, energyKwh, segments);
            }
        }
        // 2) 回落到旧的峰平谷/平价配置
        BillingRate rate = rateService.getEffectiveRate(stationId);
        boolean enabled = rate != null && rate.getTouEnabled() != null && rate.getTouEnabled() == 1 ? true : touEnabled;
        String ps = rate != null && rate.getPeakStart() != null ? rate.getPeakStart() : peakStart;
        String pe = rate != null && rate.getPeakEnd() != null ? rate.getPeakEnd() : peakEnd;
        BigDecimal pp = rate != null && rate.getPeakPrice() != null ? rate.getPeakPrice() : peakPrice;
        BigDecimal op = rate != null && rate.getOffpeakPrice() != null ? rate.getOffpeakPrice() : offpeakPrice;
        BigDecimal sf = rate != null && rate.getServiceFee() != null ? rate.getServiceFee() : serviceFee;
        BigDecimal fp = rate != null && rate.getFlatPrice() != null ? rate.getFlatPrice() : flatPrice;
        double energy = energyKwh == null ? 0.0 : Math.max(0.0, energyKwh);
        if (startTime == null || endTime == null || !enabled) {
            return BigDecimal.valueOf(energy).multiply(nvl(fp, BigDecimal.ONE)).add(nvl(sf, BigDecimal.ZERO)).setScale(2, RoundingMode.HALF_UP);
        }
        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return BigDecimal.valueOf(energy).multiply(nvl(fp, BigDecimal.ONE)).add(nvl(sf, BigDecimal.ZERO)).setScale(2, RoundingMode.HALF_UP);
        }
        double energyPerMinute = energy / minutes;
        LocalTime peakStartTime = LocalTime.parse(ps);
        LocalTime peakEndTime = LocalTime.parse(pe);
        long peakMinutes = 0;
        LocalDateTime cursor = startTime;
        while (!cursor.isAfter(endTime)) {
            LocalTime t = cursor.toLocalTime();
            boolean isPeak = !t.isBefore(peakStartTime) && t.isBefore(peakEndTime);
            if (isPeak) peakMinutes++;
            cursor = cursor.plusMinutes(1);
        }
        long offMinutes = Math.max(0, minutes - peakMinutes);
        double peakEnergy = energyPerMinute * peakMinutes;
        double offEnergy = energyPerMinute * offMinutes;
        BigDecimal amount = BigDecimal.valueOf(peakEnergy).multiply(nvl(pp, fp))
                .add(BigDecimal.valueOf(offEnergy).multiply(nvl(op, fp)))
                .add(nvl(sf, BigDecimal.ZERO))
                .setScale(2, RoundingMode.HALF_UP);
        return amount;
    }

    private BigDecimal calculateBySegments(LocalDateTime start, LocalDateTime end, Double energyKwh, List<BillingPlanSegment> segments) {
        double energy = energyKwh == null ? 0.0 : Math.max(0.0, energyKwh);
        long totalMinutes = java.time.Duration.between(start, end).toMinutes();
        if (totalMinutes <= 0) return BigDecimal.ZERO.setScale(2);
        double epm = energy / totalMinutes; // energy per minute
        BigDecimal amount = BigDecimal.ZERO;
        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            BillingPlanSegment seg = matchSegment(cursor.toLocalTime(), segments);
            BigDecimal price = (seg != null && seg.getEnergyPrice() != null) ? seg.getEnergyPrice() : BigDecimal.ZERO;
            BigDecimal fee = (seg != null && seg.getServiceFee() != null) ? seg.getServiceFee() : BigDecimal.ZERO;
            LocalDateTime boundary = nextBoundary(cursor, seg);
            if (boundary.isAfter(end)) boundary = end;
            long minutes = Math.max(0, (int) java.time.Duration.between(cursor, boundary).toMinutes());
            if (minutes == 0) {
                // 防止死循环，至少推进1分钟
                boundary = cursor.plusMinutes(1);
                if (boundary.isAfter(end)) boundary = end;
                minutes = Math.max(0, (int) java.time.Duration.between(cursor, boundary).toMinutes());
                if (minutes == 0) break;
            }
            BigDecimal minuteAmt = BigDecimal.valueOf(epm * minutes).multiply(price.add(fee));
            amount = amount.add(minuteAmt);
            cursor = boundary;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime nextBoundary(LocalDateTime cursor, BillingPlanSegment seg) {
        if (seg == null || seg.getStartTime() == null || seg.getEndTime() == null) {
            // 无匹配或不完整，按分钟推进
            return cursor.plusMinutes(1);
        }
        LocalTime st = LocalTime.parse(seg.getStartTime());
        LocalTime et = LocalTime.parse(seg.getEndTime());
        if (et.isAfter(st)) {
            // 当天内结束
            LocalDateTime todayEt = LocalDateTime.of(cursor.toLocalDate(), et);
            if (!todayEt.isAfter(cursor)) {
                // 已过本段结束，推进到下一天该结束时刻
                return todayEt.plusDays(1);
            }
            return todayEt;
        } else {
            // 跨午夜：段1 [st, 24:00)，段2 [00:00, et)
            LocalDateTime midnight = LocalDateTime.of(cursor.toLocalDate(), LocalTime.MIDNIGHT).plusDays(cursor.toLocalTime().isAfter(st) || cursor.toLocalTime().equals(st) ? 1 : 0);
            if (!cursor.toLocalTime().isBefore(et)) {
                // 在[st,24:00)内，则边界是午夜
                if (!cursor.toLocalTime().isBefore(st)) {
                    return LocalDateTime.of(cursor.toLocalDate(), LocalTime.MIDNIGHT).plusDays(1);
                }
            }
            // 在[00:00, et) 内，则边界是当天 et
            LocalDateTime todayEt = LocalDateTime.of(cursor.toLocalDate(), et);
            if (cursor.toLocalTime().isBefore(et)) {
                return todayEt;
            }
            // 兜底：推进1分钟
            return cursor.plusMinutes(1);
        }
    }

    private BillingPlanSegment matchSegment(LocalTime time, List<BillingPlanSegment> segments) {
        for (BillingPlanSegment s : segments) {
            LocalTime st = LocalTime.parse(s.getStartTime());
            LocalTime et = LocalTime.parse(s.getEndTime());
            boolean in;
            if (et.isAfter(st)) {
                in = !time.isBefore(st) && time.isBefore(et);
            } else {
                // 跨午夜
                in = !time.isBefore(st) || time.isBefore(et);
            }
            if (in) return s;
        }
        return null;
    }

    private static BigDecimal nvl(BigDecimal v, BigDecimal def) {
        return v == null ? def : v;
    }
}
