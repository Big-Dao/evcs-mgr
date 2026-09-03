package com.evcs.order.dto;

import com.evcs.order.entity.BillingRate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 计费费率响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class BillingRateResponse {
    Long stationId;
    Long id;
    Long tenantId;
    Integer touEnabled;
    String peakStart;
    String peakEnd;
    BigDecimal peakPrice;
    BigDecimal offpeakPrice;
    BigDecimal flatPrice;
    BigDecimal serviceFee;
    Integer status;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static BillingRateResponse from(com.evcs.order.entity.BillingRate e) {
        if (e == null) {
            return null;
        }
        return BillingRateResponse.builder()
                .stationId(e.getStationId())
                .id(e.getId())
                .tenantId(e.getTenantId())
                .touEnabled(e.getTouEnabled())
                .peakStart(e.getPeakStart())
                .peakEnd(e.getPeakEnd())
                .peakPrice(e.getPeakPrice())
                .offpeakPrice(e.getOffpeakPrice())
                .flatPrice(e.getFlatPrice())
                .serviceFee(e.getServiceFee())
                .status(e.getStatus())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
