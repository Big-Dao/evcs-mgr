package com.evcs.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户与角色关联实体，对应 sys_user_role 表。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @TableField("id")
    private Long id;

    private Long userId;

    private Long roleId;

    private Long tenantId;

    private LocalDateTime createTime;

    private Long createBy;
}

