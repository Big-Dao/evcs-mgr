package com.evcs.tenant.service.impl;

import com.evcs.common.tenant.TenantContext;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.tenant.dto.ChargerUtilizationDTO;
import com.evcs.tenant.dto.DashboardStatsDTO;
import com.evcs.tenant.dto.RecentOrderDTO;
import com.evcs.tenant.dto.StationRankingDTO;
import com.evcs.tenant.mapper.DashboardMapper;
import com.evcs.tenant.mapper.SysTenantMapper;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {
    
    private final DashboardMapper dashboardMapper;
    private final SysTenantMapper sysTenantMapper;
    
    /**
     * 平台类型租户
     */
    private static final Integer TENANT_TYPE_PLATFORM = 1;
    
    /**
     * 检查当前租户是否为平台类型
     */
    private boolean isPlatformTenant(Long tenantId) {
        SysTenant tenant = sysTenantMapper.selectById(tenantId);
        return tenant != null && TENANT_TYPE_PLATFORM.equals(tenant.getTenantType());
    }
    
    @Override
    public DashboardStatsDTO getDashboardStats() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的Dashboard统计数据", tenantId);
        
        // 获取今日开始和结束时间
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
        // 判断是否为平台类型租户
        boolean isPlatform = isPlatformTenant(tenantId);
        log.info("租户 {} 类型: {}", tenantId, isPlatform ? "平台" : "运营商");
        
        try {
            // 只有平台类型租户才禁用租户拦截器，以支持父子租户聚合查询
            if (isPlatform) {
                CustomTenantLineHandler.disableTenantFilter();
                log.info("平台租户 {} - 已禁用租户拦截器，启用父子租户聚合查询", tenantId);
            }
            
            try {
                // 查询各项统计数据
                Long tenantCount = dashboardMapper.countTenants(tenantId);
                Long userCount = dashboardMapper.countUsers(tenantId);
                Long stationCount = dashboardMapper.countStations(tenantId);
                Long chargerCount = dashboardMapper.countChargers(tenantId);
                Long todayOrderCount = dashboardMapper.countTodayOrders(tenantId, todayStart, todayEnd);
                BigDecimal todayChargingAmount = dashboardMapper.sumTodayChargingAmount(tenantId, todayStart, todayEnd);
                BigDecimal todayRevenue = dashboardMapper.sumTodayRevenue(tenantId, todayStart, todayEnd);
                
                // 构建返回结果
                return DashboardStatsDTO.builder()
                        .tenantCount(tenantCount != null ? tenantCount : 0L)
                        .userCount(userCount != null ? userCount : 0L)
                        .stationCount(stationCount != null ? stationCount : 0L)
                        .chargerCount(chargerCount != null ? chargerCount : 0L)
                        .todayOrderCount(todayOrderCount != null ? todayOrderCount : 0L)
                        .todayChargingAmount(todayChargingAmount != null ? todayChargingAmount : BigDecimal.ZERO)
                        .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                        .build();
            } finally {
                // 无论成功或失败，都要恢复租户拦截器
                if (isPlatform) {
                    CustomTenantLineHandler.enableTenantFilter();
                    log.info("平台租户 {} - 已恢复租户拦截器", tenantId);
                }
            }
        } catch (Exception e) {
            log.error("查询Dashboard统计数据失败", e);
            // 返回默认值，避免前端报错
            return DashboardStatsDTO.builder()
                    .tenantCount(0L)
                    .userCount(0L)
                    .stationCount(0L)
                    .chargerCount(0L)
                    .todayOrderCount(0L)
                    .todayChargingAmount(BigDecimal.ZERO)
                    .todayRevenue(BigDecimal.ZERO)
                    .build();
        }
    }
    
    @Override
    public List<RecentOrderDTO> getRecentOrders(Integer limit) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的最近 {} 条订单", tenantId, limit);
        
        // 判断是否为平台类型租户
        boolean isPlatform = isPlatformTenant(tenantId);
        
        try {
            // 只有平台类型租户才禁用租户拦截器，以支持父子租户聚合查询
            if (isPlatform) {
                CustomTenantLineHandler.disableTenantFilter();
                log.info("平台租户 {} - 已禁用租户拦截器，启用父子租户聚合查询", tenantId);
            }
            
            try {
                List<Map<String, Object>> orders = dashboardMapper.getRecentOrders(tenantId, limit);
                List<RecentOrderDTO> result = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                
                for (Map<String, Object> order : orders) {
                    RecentOrderDTO dto = RecentOrderDTO.builder()
                            .orderId(String.valueOf(order.get("order_no")))
                            .stationName(String.valueOf(order.get("station_name")))
                            .chargerCode(String.valueOf(order.get("charger_code")))
                            .userName(String.valueOf(order.get("user_name")))
                            .amount((BigDecimal) order.get("total_amount"))
                            .status(convertOrderStatus(String.valueOf(order.get("status"))))
                            .createTime(order.get("create_time") != null ? 
                                        ((LocalDateTime) order.get("create_time")).format(formatter) : "")
                            .build();
                    result.add(dto);
                }
                
                return result;
            } finally {
                // 无论成功或失败，都要恢复租户拦截器
                if (isPlatform) {
                    CustomTenantLineHandler.enableTenantFilter();
                    log.info("平台租户 {} - 已恢复租户拦截器", tenantId);
                }
            }
        } catch (Exception e) {
            log.error("查询最近订单失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<StationRankingDTO> getStationRanking() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的充电站排名（Top 5）", tenantId);
        
        boolean isPlatform = isPlatformTenant(tenantId);
        
        try {
            if (isPlatform) {
                CustomTenantLineHandler.disableTenantFilter();
            }
            
            try {
                List<Map<String, Object>> rankings = dashboardMapper.getStationRanking(tenantId, 5);
                List<StationRankingDTO> result = new ArrayList<>();
                
                // 找到最大订单数用于计算百分比
                Integer maxOrders = 0;
                if (!rankings.isEmpty()) {
                    Object firstCount = rankings.get(0).get("order_count");
                    maxOrders = firstCount != null ? ((Number) firstCount).intValue() : 0;
                }
                
                for (Map<String, Object> ranking : rankings) {
                    StationRankingDTO dto = new StationRankingDTO();
                    dto.setId(((Number) ranking.get("id")).longValue());
                    dto.setName(String.valueOf(ranking.get("station_name")));
                    Integer orders = ((Number) ranking.get("order_count")).intValue();
                    dto.setOrders(orders);
                    dto.setPercentage(maxOrders > 0 ? (orders * 100 / maxOrders) : 0);
                    result.add(dto);
                }
                
                return result;
            } finally {
                if (isPlatform) {
                    CustomTenantLineHandler.enableTenantFilter();
                }
            }
        } catch (Exception e) {
            log.error("查询充电站排名失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<ChargerUtilizationDTO> getChargerUtilization() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的充电桩利用率（Top 5）", tenantId);
        
        boolean isPlatform = isPlatformTenant(tenantId);
        
        try {
            if (isPlatform) {
                CustomTenantLineHandler.disableTenantFilter();
            }
            
            try {
                List<Map<String, Object>> utilizations = dashboardMapper.getChargerUtilization(tenantId, 5);
                List<ChargerUtilizationDTO> result = new ArrayList<>();
                
                for (Map<String, Object> util : utilizations) {
                    ChargerUtilizationDTO dto = new ChargerUtilizationDTO();
                    dto.setId(((Number) util.get("id")).longValue());
                    dto.setCode(String.valueOf(util.get("charger_code")));
                    Object utilizationObj = util.get("utilization");
                    dto.setUtilization(utilizationObj != null ? ((Number) utilizationObj).intValue() : 0);
                    result.add(dto);
                }
                
                return result;
            } finally {
                if (isPlatform) {
                    CustomTenantLineHandler.enableTenantFilter();
                }
            }
        } catch (Exception e) {
            log.error("查询充电桩利用率失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 转换订单状态为中文
     */
    private String convertOrderStatus(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "CREATED" -> "已创建";
            case "CHARGING" -> "充电中";
            case "COMPLETED" -> "已完成";
            case "PAID" -> "已支付";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
