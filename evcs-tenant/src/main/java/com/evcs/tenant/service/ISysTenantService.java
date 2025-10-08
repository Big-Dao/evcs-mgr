package com.evcs.tenant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.tenant.entity.SysTenant;
import cn.hutool.core.lang.tree.Tree;
import java.util.List;

public interface ISysTenantService extends IService<SysTenant> {
    IPage<SysTenant> queryTenantPage(Page<SysTenant> page, SysTenant tenant);
    List<Tree<Long>> getTenantTree();
    SysTenant getTenantById(Long tenantId);
    boolean saveTenant(SysTenant tenant);
    boolean updateTenant(SysTenant tenant);
    boolean deleteTenant(Long tenantId);
    boolean moveTenant(Long tenantId, Long newParentId);
    List<Long> getTenantChildren(Long tenantId);
    List<SysTenant> getSubTenants(Long parentId);
    boolean changeStatus(Long tenantId, Integer status);
    boolean checkTenantCodeExists(String tenantCode, Long excludeId);
}
