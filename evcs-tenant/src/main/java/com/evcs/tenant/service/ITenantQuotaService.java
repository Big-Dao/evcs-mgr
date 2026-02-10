package com.evcs.tenant.service;

import com.evcs.tenant.entity.QuotaCheckResult;
import com.evcs.tenant.entity.SysTenant;

/**
 * 租户配额服务
 * <p>负责检查和管理租户的资源配额
 */
public interface ITenantQuotaService {

    /**
     * 检查租户是否可以创建新的子租户
     *
     * @param parentId 父租户ID
     * @return 配额检查结果
     */
    QuotaCheckResult checkCanCreateChildTenant(Long parentId);

    /**
     * 检查租户是否可以添加新站点
     *
     * @param tenantId 租户ID
     * @return 配额检查结果
     */
    QuotaCheckResult checkCanAddStation(Long tenantId);

    /**
     * 检查租户是否可以添加新充电桩
     *
     * @param tenantId 租户ID
     * @return 配额检查结果
     */
    QuotaCheckResult checkCanAddCharger(Long tenantId);

    /**
     * 检查租户是否可以添加新用户
     *
     * @param tenantId 租户ID
     * @return 配额检查结果
     */
    QuotaCheckResult checkCanAddUser(Long tenantId);

    /**
     * 获取租户配额使用情况
     *
     * @param tenantId 租户ID
     * @return 配额使用情况
     */
    TenantQuotaUsage getQuotaUsage(Long tenantId);

    /**
     * 更新租户配额（需要上级权限）
     *
     * @param tenantId 租户ID
     * @param maxUsers 最大用户数
     * @param maxStations 最大站点数
     * @param maxChargers 最大充电桩数
     */
    void updateQuota(Long tenantId, Integer maxUsers, Integer maxStations, Integer maxChargers);

    /**
     * 租户配额使用情况
     */
    record TenantQuotaUsage(
            Long tenantId,
            String tenantName,
            Integer maxChildren,      // 最大子租户数
            Integer currentChildren,  // 当前子租户数
            Integer maxUsers,         // 最大用户数
            Integer currentUsers,     // 当前用户数
            Integer maxStations,      // 最大站点数
            Integer currentStations,  // 当前站点数
            Integer maxChargers,      // 最大充电桩数
            Integer currentChargers,  // 当前充电桩数
            Integer maxSessions,      // 最大并发会话数
            Integer currentSessions   // 当前并发会话数
    ) {
        public int getChildrenUsagePercent() {
            return maxChildren == null || maxChildren == 0 ? 0 : (currentChildren * 100 / maxChildren);
        }

        public int getUsersUsagePercent() {
            return maxUsers == null || maxUsers == 0 ? 0 : (currentUsers * 100 / maxUsers);
        }

        public int getStationsUsagePercent() {
            return maxStations == null || maxStations == 0 ? 0 : (currentStations * 100 / maxStations);
        }

        public int getChargersUsagePercent() {
            return maxChargers == null || maxChargers == 0 ? 0 : (currentChargers * 100 / maxChargers);
        }
    }
}
