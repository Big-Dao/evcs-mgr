package com.evcs.station.dto;

import com.evcs.station.entity.FirmwareUpgradeTask;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 固件升级任务响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class FirmwareTaskResponse {
    Long id;
    Long tenantId;
    Long firmwareId;
    Long chargerId;
    String status;
    Integer retryCount;
    LocalDateTime startTime;
    LocalDateTime finishTime;
    String errorMessage;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static FirmwareTaskResponse from(com.evcs.station.entity.FirmwareUpgradeTask e) {
        if (e == null) {
            return null;
        }
        return FirmwareTaskResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .firmwareId(e.getFirmwareId())
                .chargerId(e.getChargerId())
                .status(e.getStatus())
                .retryCount(e.getRetryCount())
                .startTime(e.getStartTime())
                .finishTime(e.getFinishTime())
                .errorMessage(e.getErrorMessage())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
