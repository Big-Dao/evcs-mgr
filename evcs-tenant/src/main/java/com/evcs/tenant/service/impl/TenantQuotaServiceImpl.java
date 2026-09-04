package com.evcs.tenant.service.impl;

import com.evcs.common.audit.TenantAuditService;
import com.evcs.tenant.client.AuthStatsClient;
import com.evcs.tenant.client.StationUsageClient;
import com.evcs.tenant.dto.StationUsageCount;
import com.evcs.tenant.entity.QuotaCheckResult;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.mapper.SysTenantMapper;
import com.evcs.tenant.service.ITenantQuotaService;
import com.evcs.tenant.service.ITenantAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 租户配额服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantQuotaServiceImpl implements ITenantQuotaService {

    private final SysTenantMapper tenantMapper;
    private final TenantAuditService tenantAuditService;
    private final StationUsageClient stationUsageClient;
    private final AuthStatsClient authStatsClient;

    @Override
    public QuotaCheckResult checkCanCreateChildTenant(Long parentId) {
        SysTenant parent = getTenantOrThrow(parentId);

        // 平台租户不限制子租户数量
        if (parent.getTenantType() == 1) {
            return QuotaCheckResult.ok();
        }

        // 计算当前子租户数量
        int currentCount = tenantMapper.countByParentId(parentId);
        Integer maxChildren = parent.getMaxChildren();

        if (maxChildren != null && maxChildren > 0 && currentCount >= maxChildren) {
            return QuotaCheckResult.denied(
                    String.format("子租户数量已达上限 (%d/%d)", currentCount, maxChildren),
                    currentCount, maxChildren, "children"
            );
        }

        return QuotaCheckResult.ok();
    }

    @Override
    public QuotaCheckResult checkCanAddStation(Long tenantId) {
        SysTenant tenant = getTenantOrThrow(tenantId);

        int currentCount = currentStationCount(tenantId);
        Integer maxStations = tenant.getMaxStations();

        if (maxStations != null && maxStations > 0 && currentCount >= maxStations) {
            return QuotaCheckResult.denied(
                    String.format("站点数量已达上限 (%d/%d)", currentCount, maxStations),
                    currentCount, maxStations, "stations"
            );
        }

        // 检查上级租户的配额
        return checkParentQuota(tenant, "stations", currentCount);
    }

    @Override
    public QuotaCheckResult checkCanAddCharger(Long tenantId) {
        SysTenant tenant = getTenantOrThrow(tenantId);

        int currentCount = currentChargerCount(tenantId);
        Integer maxChargers = tenant.getMaxChargers();

        if (maxChargers != null && maxChargers > 0 && currentCount >= maxChargers) {
            return QuotaCheckResult.denied(
                    String.format("充电桩数量已达上限 (%d/%d)", currentCount, maxChargers),
                    currentCount, maxChargers, "chargers"
            );
        }

        // 检查上级租户的配额
        return checkParentQuota(tenant, "chargers", currentCount);
    }

    @Override
    public QuotaCheckResult checkCanAddUser(Long tenantId) {
        SysTenant tenant = getTenantOrThrow(tenantId);

        // 用户数据归属 auth 服务，经内部 API 实时统计（当前租户，fail-closed）
        long currentCount = authStatsClient.countActiveUsers(List.of(tenantId));
        Integer maxUsers = tenant.getMaxUsers();

        if (maxUsers != null && maxUsers > 0 && currentCount >= maxUsers) {
            return QuotaCheckResult.denied(
                    String.format("用户数量已达上限 (%d/%d)", currentCount, maxUsers),
                    (int) currentCount, maxUsers, "users"
            );
        }

        return QuotaCheckResult.ok();
    }

    @Override
    public TenantQuotaUsage getQuotaUsage(Long tenantId) {
        SysTenant tenant = getTenantOrThrow(tenantId);

        // 获取所有子租户（包括自己）
        List<Long> allTenantIds = getAllDescendantIds(tenantId);
        allTenantIds.add(tenantId);

        // 统计资源使用
        int currentChildren = tenantMapper.countByParentId(tenantId);
        int currentStations = aggregateStations(allTenantIds);
        int currentChargers = aggregateChargers(allTenantIds);
        // 用户统计需要从 auth 服务获取，会话统计需要从 order 服务获取
        // 生产环境需要实现跨服务调用
        int currentUsers = 0;
        int currentSessions = 0;

        return new TenantQuotaUsage(
                tenantId,
                tenant.getTenantName(),
                tenant.getMaxChildren(),
                currentChildren,
                tenant.getMaxUsers(),
                currentUsers,
                tenant.getMaxStations(),
                currentStations,
                tenant.getMaxChargers(),
                currentChargers,
                tenant.getMaxSessions(),
                currentSessions
        );
    }

    @Override
    public void updateQuota(Long tenantId, Integer maxUsers, Integer maxStations, Integer maxChargers) {
        SysTenant tenant = getTenantOrThrow(tenantId);

        // 记录变更前的值
        String details = String.format(
                "Update quota: users [%d->%d], stations [%d->%d], chargers [%d->%d]",
                tenant.getMaxUsers(), maxUsers,
                tenant.getMaxStations(), maxStations,
                tenant.getMaxChargers(), maxChargers
        );

        tenant.setMaxUsers(maxUsers);
        tenant.setMaxStations(maxStations);
        tenant.setMaxChargers(maxChargers);

        tenantMapper.updateById(tenant);

        // 记录审计日志
        tenantAuditService.logOperation(
                ITenantAuditLogService.ACTION_UPDATE_QUOTA,
                tenantId,
                details
        );
    }

    /**
     * 站点/充电桩用量来自 station 服务内部 API（数据归属方），调用失败向上抛出（fail-closed）。
     */
    private int currentStationCount(Long tenantId) {
        return usageCounts(List.of(tenantId)).getOrDefault(tenantId, new StationUsageCount(tenantId, 0, 0)).stationCount();
    }

    private int currentChargerCount(Long tenantId) {
        return usageCounts(List.of(tenantId)).getOrDefault(tenantId, new StationUsageCount(tenantId, 0, 0)).chargerCount();
    }

    private int aggregateStations(List<Long> tenantIds) {
        return usageCounts(tenantIds).values().stream().mapToInt(StationUsageCount::stationCount).sum();
    }

    private int aggregateChargers(List<Long> tenantIds) {
        return usageCounts(tenantIds).values().stream().mapToInt(StationUsageCount::chargerCount).sum();
    }

    private Map<Long, StationUsageCount> usageCounts(List<Long> tenantIds) {
        return stationUsageClient.getUsageCounts(tenantIds);
    }

    /**
     * 递归获取所有后代租户ID
     */
    private List<Long> getAllDescendantIds(Long tenantId) {
        return tenantMapper.selectDescendantIds(tenantId);
    }

    /**
     * 检查上级租户的配额限制
     */
    private QuotaCheckResult checkParentQuota(SysTenant tenant, String resourceType, int currentCount) {
        Long parentId = tenant.getParentId();
        if (parentId == null || parentId == 0) {
            return QuotaCheckResult.ok();
        }

        // 递归检查所有上级租户
        while (parentId != null && parentId != 0) {
            SysTenant parent = tenantMapper.selectById(parentId);
                        if (parent == null) {
                break;
            }

            // 计算上级租户下所有子租户的资源总量
            List<Long> descendantIds = getAllDescendantIds(parentId);
            descendantIds.add(parentId);
            int totalCount = switch (resourceType) {
                case "stations" -> aggregateStations(descendantIds);
                case "chargers" -> aggregateChargers(descendantIds);
                default -> currentCount;
            };

            Integer limit = switch (resourceType) {
                case "stations" -> parent.getMaxStations();
                case "chargers" -> parent.getMaxChargers();
                default -> null;
            };

            if (limit != null && limit > 0 && totalCount >= limit) {
                return QuotaCheckResult.denied(
                        String.format("上级租户 %s 的%s数量已达上限 (%d/%d)",
                                parent.getTenantName(), resourceType, totalCount, limit),
                        totalCount, limit, resourceType
                );
            }

            parentId = parent.getParentId();
        }

        return QuotaCheckResult.ok();
    }

    private SysTenant getTenantOrThrow(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("租户不存在: " + tenantId);
        }
        return tenant;
    }
}
