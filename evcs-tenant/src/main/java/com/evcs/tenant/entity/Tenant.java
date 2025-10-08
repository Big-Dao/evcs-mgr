package com.evcs.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class Tenant extends BaseEntity {
    
    /**
     * 租户编码
     */
    private String tenantCode;
    
    /**
     * 租户名称
     */
    private String tenantName;
    
    /**
     * 父租户ID（实现分层结构）
     */
    private Long parentId;
    
    /**
     * 祖级列表（逗号分隔的ID）
     */
    private String ancestors;
    
    /**
     * 联系人
     */
    private String contactPerson;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 联系邮箱
     */
    private String contactEmail;
    
    /**
     * 企业地址
     */
    private String address;
    
    /**
     * 统一社会信用代码
     */
    private String socialCode;
    
    /**
     * 营业执照URL
     */
    private String licenseUrl;
    
    /**
     * 租户类型：1-平台方，2-运营商，3-第三方合作伙伴
     */
    private Integer tenantType;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 过期时间
     */
    private java.time.LocalDateTime expireTime;
    
    /**
     * 最大用户数
     */
    private Integer maxUsers;
    
    /**
     * 最大充电站数
     */
    private Integer maxStations;
    
    /**
     * 最大充电桩数
     */
    private Integer maxChargers;
    
    /**
     * 备注
     */
    private String remark;
}