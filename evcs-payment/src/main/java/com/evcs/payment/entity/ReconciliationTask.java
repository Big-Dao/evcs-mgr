package com.evcs.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reconciliation_task")
public class ReconciliationTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private String channel;

    private LocalDate reconciliationDate;

    private String status;

    private Integer totalCount;

    private Integer matchedCount;

    private Integer unmatchedCount;

    private Integer exceptionCount;

    private BigDecimal totalAmount;

    private BigDecimal matchedAmount;

    private BigDecimal unmatchedAmount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long duration;

    private Long tenantId;

    private String statementFileUrl;

    private String reportFileUrl;

    private String errorMessage;

    private String createdBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
