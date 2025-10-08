package com.evcs.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {
    
    /**
     * 角色编码
     */
    private String roleCode;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 显示顺序
     */
    private Integer sort;
    
    /**
     * 数据范围：1-全部数据权限，2-自定义数据权限，3-本部门数据权限，4-本部门及以下数据权限，5-仅本人数据权限
     */
    private Integer dataScope;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;
}