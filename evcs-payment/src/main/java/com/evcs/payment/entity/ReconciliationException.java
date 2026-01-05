package com.evcs.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reconciliation_exception")
public class ReconciliationException {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String tradeNo;

    private String orderNo;

    private BigDecimal amount;

    private String exceptionType;

    private String description;

    private String handleStatus;

    private String handleResult;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
