package com.evcs.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evcs.auth.dto.UserStatsProjection;
import com.evcs.auth.entity.SysUser;
import com.evcs.auth.mapper.SysUserMapper;
import com.evcs.common.tenant.CustomTenantLineHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户统计服务（tenant 仪表盘数据来源，跨服务 SQL 拆解后的归属端）。
 *
 * <p>用户数据归属 auth 服务，按调用方给定的租户ID集合显式统计
 * （受控禁用租户过滤，等价于跨租户管理查询）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final SysUserMapper sysUserMapper;

    /**
     * 统计租户集合内启用中的用户数（语义对齐原 countUsers：deleted=0 AND status=1）。
     */
    public long countActiveUsers(List<Long> tenantIds) {
        try {
            CustomTenantLineHandler.disableTenantFilter();
            QueryWrapper<SysUser> query = new QueryWrapper<SysUser>()
                    .in("tenant_id", tenantIds)
                    .eq("deleted", 0)
                    .eq("status", 1);
            Long count = sysUserMapper.selectCount(query);
            return count == null ? 0L : count;
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    /**
     * 按用户ID批量查询用户名（最近订单展示用）。
     */
    public List<UserStatsProjection> getUsernamesByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        try {
            CustomTenantLineHandler.disableTenantFilter();
            QueryWrapper<SysUser> query = new QueryWrapper<SysUser>()
                    .select("id", "username")
                    .in("id", userIds)
                    .eq("deleted", 0);
            List<UserStatsProjection> rows = new ArrayList<>();
            for (SysUser user : sysUserMapper.selectList(query)) {
                rows.add(new UserStatsProjection(user.getId(), user.getUsername()));
            }
            return rows;
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }
}
