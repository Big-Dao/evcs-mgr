package com.evcs.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 租户实体（用于登录时查询租户ID）
 */
@Data
@TableName("sys_tenant")
public class Tenant {
    
    @TableId
    private Long id;
    
    private Long tenantId;
    
    private String tenantCode;
    
    private String tenantName;
    
    private Integer status;
}
