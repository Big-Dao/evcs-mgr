package com.evcs.auth.controller;

import com.evcs.auth.entity.Permission;
import com.evcs.auth.service.IPermissionService;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单权限管理控制器
 */
@Tag(name = "菜单权限管理", description = "菜单权限查询接口")
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {
    
    private final IPermissionService permissionService;
    
    /**
     * 查询所有菜单权限树
     */
    @GetMapping("/list")
    @Operation(summary = "查询菜单权限树")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "查询当前租户菜单")
    public Result<List<Permission>> list() {
        return Result.success(permissionService.listMenuTree());
    }
    
    /**
     * 查询用户的菜单权限
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户菜单权限")
    @DataScope(value = DataScope.DataScopeType.TENANT, description = "查询当前租户")
    public Result<List<Permission>> getUserMenus(@PathVariable Long userId) {
        return Result.success(permissionService.listUserMenus(userId));
    }
}
