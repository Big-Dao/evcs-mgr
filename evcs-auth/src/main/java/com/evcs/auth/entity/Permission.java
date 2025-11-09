package com.evcs.auth.entity;

/**
 * 权限实体
 */
public class Permission {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限编码
     */
    private String perms;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 父权限ID
     */
    private Long parentId;

    /**
     * 权限类型：1-菜单 2-按钮
     */
    private Integer type;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 权限标识
     */
    private String permissionCode;

    /**
     * 图标
     */
    private String icon;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(final String perms) {
        this.perms = perms;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(final Integer type) {
        this.type = type;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(final Integer sort) {
        this.sort = sort;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(final String component) {
        this.component = component;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(final String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }
}