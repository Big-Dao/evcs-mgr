package com.evcs.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.auth.entity.Role;
import com.evcs.auth.service.IRoleService;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理", description = "角色增删改查接口")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {
    
    private final IRoleService roleService;
    
    /**
     * 分页查询角色列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询角色")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "查询当前租户角色")
    public Result<IPage<Role>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Page<Role> page = new Page<>(current, size);
        return Result.success(roleService.pageRoles(page));
    }
    
    /**
     * 查询所有角色列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有角色")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "查询当前租户角色")
    public Result<List<Role>> list() {
        return Result.success(roleService.listAllRoles());
    }
    
    /**
     * 根据ID查询角色详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询角色详情")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "查询当前租户角色")
    public Result<Role> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }
    
    /**
     * 新增角色
     */
    @PostMapping
    @Operation(summary = "新增角色")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "当前租户")
    public Result<Boolean> save(@RequestBody Role role) {
        return Result.success(roleService.save(role));
    }
    
    /**
     * 更新角色
     */
    @PutMapping
    @Operation(summary = "更新角色")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "当前租户")
    public Result<Boolean> update(@RequestBody Role role) {
        return Result.success(roleService.updateById(role));
    }
    
    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "当前租户")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(roleService.removeById(id));
    }
}
