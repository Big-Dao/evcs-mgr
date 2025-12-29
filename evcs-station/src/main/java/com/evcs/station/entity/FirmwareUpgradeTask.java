package com.evcs.station.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 固件升级任务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("firmware_upgrade_task")
public class FirmwareUpgradeTask extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联固件ID
     */
    private Long firmwareId;

    /**
     * 目标充电桩ID
     */
    private Long chargerId;

    /**
     * 状态: PENDING, DOWNLOADING, INSTALLING, INSTALLED, FAILED
     */
    private String status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime finishTime;

    /**
     * 失败原因
     */
    private String errorMessage;
}
