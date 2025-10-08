package com.evcs.tenant.service;

import com.evcs.tenant.entity.Tenant;
import com.evcs.common.page.PageQuery;
import com.evcs.common.page.PageResult;

import java.util.List;

/**
 * 租户服务接口
 */
public interface TenantService {
    
    /**
     * 创建租户
     */
    Tenant createTenant(Tenant tenant);
    
    /**
     * 更新租户
     */
    Tenant updateTenant(Tenant tenant);
    
    /**
     * 删除租户
     */
    void deleteTenant(Long id);
    
    /**
     * 根据ID查询租户
     */
    Tenant getTenantById(Long id);
    
    /**
     * 根据编码查询租户
     */
    Tenant getTenantByCode(String tenantCode);
    
    /**
     * 分页查询租户
     */
    PageResult<Tenant> pageTenants(PageQuery pageQuery);
    
    /**
     * 查询子租户列表
     */
    List<Tenant> getSubTenants(Long parentId);
    
    /**
     * 查询租户层级树
     */
    List<Tenant> getTenantTree(Long rootId);
    
    /**
     * 检查租户编码是否存在
     */
    boolean existsByCode(String tenantCode);
    
    /**
     * 启用/禁用租户
     */
    void changeStatus(Long tenantId, Integer status);
    
    /**
     * 获取租户的祖级路径
     */
    String getTenantAncestors(Long tenantId);
    
    /**
     * 检查是否为上级租户
     */
    boolean isParentTenant(Long parentTenantId, Long childTenantId);
}