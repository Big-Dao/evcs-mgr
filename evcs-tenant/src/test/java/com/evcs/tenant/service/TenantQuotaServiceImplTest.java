package com.evcs.tenant.service;

import com.evcs.common.audit.TenantAuditService;
import com.evcs.tenant.client.AuthStatsClient;
import com.evcs.tenant.client.StationUsageClient;
import com.evcs.tenant.dto.StationUsageCount;
import com.evcs.tenant.entity.QuotaCheckResult;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.mapper.SysTenantMapper;
import com.evcs.tenant.service.impl.TenantQuotaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 租户配额服务单元测试：站点/充电桩用量必须来自 station 服务内部 API
 * （数据归属方），而不是跨服务直查数据库。
 */
class TenantQuotaServiceImplTest {

    private SysTenantMapper mapper;
    private StationUsageClient stationUsageClient;
    private AuthStatsClient authStatsClient;
    private TenantQuotaServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysTenantMapper.class);
        stationUsageClient = mock(StationUsageClient.class);
        authStatsClient = mock(AuthStatsClient.class);
        service = new TenantQuotaServiceImpl(mapper, mock(TenantAuditService.class), stationUsageClient, authStatsClient);
    }

    private SysTenant tenant(Long id, Integer maxStations, Integer maxChargers) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setTenantName("配额测试租户" + id);
        tenant.setTenantType(2);
        tenant.setParentId(null);
        tenant.setMaxStations(maxStations);
        tenant.setMaxChargers(maxChargers);
        tenant.setMaxUsers(null);
        return tenant;
    }

    private SysTenant tenantWithUsers(Long id, Integer maxUsers) {
        SysTenant tenant = tenant(id, null, null);
        tenant.setMaxUsers(maxUsers);
        return tenant;
    }

    @Test
    @DisplayName("新增站点 - station 服务计数达到配额应拒绝")
    void checkCanAddStationShouldDenyWhenQuotaReached() {
        when(mapper.selectById(5L)).thenReturn(tenant(5L, 1, null));
        when(stationUsageClient.getUsageCounts(List.of(5L)))
                .thenReturn(Map.of(5L, new StationUsageCount(5L, 1, 0)));

        QuotaCheckResult result = service.checkCanAddStation(5L);

        assertFalse(result.allowed(), "站点数已达配额时应拒绝");
        assertEquals(1, result.current());
        assertEquals("stations", result.resourceType());
    }

    @Test
    @DisplayName("新增站点 - 未达配额应允许")
    void checkCanAddStationShouldAllowUnderQuota() {
        when(mapper.selectById(5L)).thenReturn(tenant(5L, 1, null));
        when(stationUsageClient.getUsageCounts(List.of(5L)))
                .thenReturn(Map.of(5L, new StationUsageCount(5L, 0, 3)));

        QuotaCheckResult result = service.checkCanAddStation(5L);

        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("新增充电桩 - station 服务计数达到配额应拒绝")
    void checkCanAddChargerShouldDenyWhenQuotaReached() {
        when(mapper.selectById(6L)).thenReturn(tenant(6L, null, 2));
        when(stationUsageClient.getUsageCounts(List.of(6L)))
                .thenReturn(Map.of(6L, new StationUsageCount(6L, 0, 2)));

        QuotaCheckResult result = service.checkCanAddCharger(6L);

        assertFalse(result.allowed(), "充电桩数已达配额时应拒绝");
        assertEquals("chargers", result.resourceType());
    }

    @Test
    @DisplayName("新增用户 - auth 服务计数达到配额应拒绝")
    void checkCanAddUserShouldDenyWhenQuotaReached() {
        when(mapper.selectById(8L)).thenReturn(tenantWithUsers(8L, 2));
        when(authStatsClient.countActiveUsers(List.of(8L))).thenReturn(2L);

        QuotaCheckResult result = service.checkCanAddUser(8L);

        assertFalse(result.allowed(), "用户数已达配额时应拒绝");
        assertEquals("users", result.resourceType());
    }

    @Test
    @DisplayName("新增用户 - 未达配额应允许")
    void checkCanAddUserShouldAllowUnderQuota() {
        when(mapper.selectById(8L)).thenReturn(tenantWithUsers(8L, 5));
        when(authStatsClient.countActiveUsers(List.of(8L))).thenReturn(2L);

        QuotaCheckResult result = service.checkCanAddUser(8L);

        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("配额用量 - 应聚合自身与后代租户的 station 服务计数")
    void getQuotaUsageShouldAggregateDescendantsViaClient() {
        when(mapper.selectById(5L)).thenReturn(tenant(5L, 10, 10));
        when(mapper.selectDescendantIds(5L)).thenReturn(new ArrayList<>(List.of(6L, 7L)));
        when(stationUsageClient.getUsageCounts(anyList())).thenReturn(Map.of(
                5L, new StationUsageCount(5L, 1, 1),
                6L, new StationUsageCount(6L, 2, 0),
                7L, new StationUsageCount(7L, 0, 3)));

        var usage = service.getQuotaUsage(5L);

        assertEquals(3, usage.currentStations());
        assertEquals(4, usage.currentChargers());
    }
}
