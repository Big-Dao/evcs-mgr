package com.evcs.tenant.controller;

import com.evcs.common.annotation.DataScope;
import com.evcs.common.page.PageQuery;
import com.evcs.common.result.Result;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.service.ISysTenantService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 租户管理控制器
 */
@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {
    
    private final ISysTenantService tenantService;
    
    /**
     * 创建租户
     */
    @PostMapping
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY, 
              description = "只能在当前租户下创建子租户")
    public Result<SysTenant> createTenant(@Valid @RequestBody SysTenant tenant) {
        SysTenant created = tenantService.saveTenant(tenant) ? tenant : null;
        return Result.success("租户创建成功", created);
    }
    
    /**
     * 更新租户
     */
    @PutMapping("/{id}")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能更新本租户及下级租户信息")
    public Result<SysTenant> updateTenant(
            @Parameter(description = "租户ID") @PathVariable Long id,
            @Valid @RequestBody SysTenant tenant) {
        tenant.setId(id);
        boolean ok = tenantService.updateTenant(tenant);
        SysTenant updated = ok ? tenantService.getTenantById(id) : null;
        return Result.success("租户更新成功", updated);
    }
    
    /**
     * 删除租户
     */
    @DeleteMapping("/{id}")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能删除下级租户")
    public Result<Void> deleteTenant(@Parameter(description = "租户ID") @PathVariable Long id) {
        tenantService.deleteTenant(id);
        return Result.success("租户删除成功");
    }
    
    /**
     * 查询租户详情
     */
    @GetMapping("/{id}")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能查询本租户及下级租户信息")
    public Result<SysTenant> getTenant(@Parameter(description = "租户ID") @PathVariable Long id) {
        SysTenant tenant = tenantService.getTenantById(id);
        return Result.success("查询成功", tenant);
    }
    
    /**
     * 查询租户列表（不分页）
     */
    @GetMapping("/list")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能查询本租户及下级租户")
    public Result<List<SysTenant>> listTenants(SysTenant query) {
        List<SysTenant> list = tenantService.queryTenantList(query);
        return Result.success("查询成功", list);
    }
    
    /**
     * 分页查询租户
     */
    @GetMapping("/page")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能查询本租户及下级租户")
    public Result<IPage<SysTenant>> pageTenants(@Valid PageQuery pageQuery) {
        Page<SysTenant> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        IPage<SysTenant> result = tenantService.queryTenantPage(page, new SysTenant());
        return Result.success("查询成功", result);
    }
    
    /**
     * 查询子租户列表
     */
    @GetMapping("/{parentId}/children")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能查询本租户及下级租户的子租户")
    public Result<List<SysTenant>> getSubTenants(
            @Parameter(description = "父租户ID") @PathVariable Long parentId) {
        List<SysTenant> subTenants = tenantService.getSubTenants(parentId);
        return Result.success("查询成功", subTenants);
    }
    
    /**
     * 查询租户树
     */
    @GetMapping("/tree")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "从当前租户开始的层级树")
    public Result<List<SysTenant>> getTenantTree(
            @Parameter(description = "根节点ID，不传则从当前租户开始") 
            @RequestParam(required = false) Long rootId) {
        // 使用 queryTenantList 以应用租户隔离过滤
        List<SysTenant> tree = tenantService.queryTenantList(new SysTenant());
        return Result.success("查询成功", tree);
    }
    
    /**
     * 启用/禁用租户
     */
    @PutMapping("/{id}/status")
    @DataScope(value = DataScope.DataScopeType.TENANT_HIERARCHY,
              description = "只能修改下级租户状态")
    public Result<Void> changeStatus(
            @Parameter(description = "租户ID") @PathVariable Long id,
            @Parameter(description = "状态：0-禁用，1-启用") @RequestParam Integer status) {
        tenantService.changeStatus(id, status);
        return Result.success("状态修改成功");
    }
    
    /**
     * 检查租户编码是否存在
     */
    @GetMapping("/check-code")
    public Result<Boolean> checkTenantCode(
            @Parameter(description = "租户编码") @RequestParam String tenantCode) {
        boolean exists = tenantService.checkTenantCodeExists(tenantCode, null);
        return Result.success("检查完成", exists);
    }
}
