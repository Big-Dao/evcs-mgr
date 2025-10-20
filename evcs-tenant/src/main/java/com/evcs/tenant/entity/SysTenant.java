package com.evcs.tenant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

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
    
    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;
    
    @NotBlank(message = "租户名称不能为空")
    private String tenantName;
    
    private Long parentId;
    private String ancestors;
    
    @NotBlank(message = "联系人不能为空")
    private String contactPerson;
    
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;
    
    @Email(message = "联系邮箱格式不正确")
    private String contactEmail;
    
    private String address;
    private String socialCode;
    private String licenseUrl;
    
    @NotNull(message = "租户类型不能为空")
    private Integer tenantType; // 1-平台方，2-运营商，3-第三方合作伙伴
    
    private Integer status;     // 0-禁用，1-启用
    private java.time.LocalDateTime expireTime;
    private Integer maxUsers;
    private Integer maxStations;
    private Integer maxChargers;
    private String remark;
}
