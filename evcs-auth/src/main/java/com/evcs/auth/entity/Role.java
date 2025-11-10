package com.evcs.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体，对应 sys_role 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @TableField("id")
    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    /**
     * 数据权限范围 1-全部数据权限 2-自定义数据权限 3-本部门数据权限 4-本部门及以下数据权限 5-仅本人数据权限
     */
    private Integer dataScope;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

    /**
     * 排序值
     */
    private Integer sort;
}
