package com.evcs.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户实体
 */
@Data
@EqualsAndHashCode(callSuper = false)  // 不包含父类字段
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {
    /**
     * 主键ID (对应数据库id列)
     * 注意：这里使用独立的id字段而不是复用父类的tenantId
     */
    @TableId(value = "id", type = IdType.AUTO)
    @TableField("id")
    private Long id;
    
    private String tenantCode;
    private String tenantName;
    private Long parentId;
    private String ancestors;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String socialCode;
    private String licenseUrl;
    private Integer tenantType; // 1-平台方，2-运营商，3-第三方合作伙伴
    private Integer status;     // 0-禁用，1-启用
    private java.time.LocalDateTime expireTime;
    private Integer maxUsers;
    private Integer maxStations;
    private Integer maxChargers;
    private String remark;
}
