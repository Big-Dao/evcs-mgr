package com.evcs.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {
    
    /**
     * 权限编码
     */
    private String permissionCode;
    
    /**
     * 权限名称
     */
    private String permissionName;
    
    /**
     * 父权限ID
     */
    private Long parentId;
    
    /**
     * 权限类型：1-菜单，2-按钮，3-接口
     */
    private Integer type;
    
    /**
     * 路由路径
     */
    private String path;
    
    /**
     * 组件路径
     */
    private String component;
    
    /**
     * 权限标识
     */
    private String perms;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 显示顺序
     */
    private Integer sort;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 是否为外链：0-否，1-是
     */
    private Integer isFrame;
    
    /**
     * 是否缓存：0-否，1-是
     */
    private Integer isCache;
    
    /**
     * 显示状态：0-隐藏，1-显示
     */
    private Integer visible;
    
    /**
     * 备注
     */
    private String remark;
}