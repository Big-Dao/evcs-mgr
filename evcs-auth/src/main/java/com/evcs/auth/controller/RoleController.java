package com.evcs.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.auth.entity.Role;
import com.evcs.auth.service.IRoleService;
import com.evcs.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    public Result<IPage<Role>> page(@RequestParam(defaultValue = "1") Long current,
                                    @RequestParam(defaultValue = "10") Long size,
                                    @RequestParam(required = false) String roleName) {
        Page<Role> page = new Page<>(current, size);
        QueryWrapper<Role> qw = new QueryWrapper<>();
        if (roleName != null && !roleName.isBlank()) {
            qw.like("role_name", roleName);
        }
        qw.orderByAsc("sort");
        return Result.success(roleService.page(page, qw));
    }

    @Operation(summary = "查询所有角色")
    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleService.list(new QueryWrapper<Role>().orderByAsc("sort")));
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public Result<Role> get(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public Result<Boolean> add(@RequestBody Role role) {
        return Result.success(roleService.save(role));
    }

    @Operation(summary = "修改角色")
    @PutMapping
    public Result<Boolean> update(@RequestBody Role role) {
        return Result.success(roleService.updateById(role));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(roleService.removeById(id));
    }
}
