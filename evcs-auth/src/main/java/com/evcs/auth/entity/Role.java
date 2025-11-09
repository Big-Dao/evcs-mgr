package com.evcs.auth.entity;

/**
 * 角色实体
 */
public class Role {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 数据权限范围
     * 1-全部数据权限 2-自定义数据权限 3-本部门数据权限 4-本部门及以下数据权限 5-仅本人数据权限
     */
    private Integer dataScope;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(final String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(final String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getDataScope() {
        return dataScope;
    }

    public void setDataScope(final Integer dataScope) {
        this.dataScope = dataScope;
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
}