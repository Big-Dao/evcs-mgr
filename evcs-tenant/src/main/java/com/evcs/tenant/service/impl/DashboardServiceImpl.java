package com.evcs.tenant.service.impl;

import com.evcs.common.tenant.TenantContext;
import com.evcs.tenant.dto.DashboardStatsDTO;
import com.evcs.tenant.dto.RecentOrderDTO;
import com.evcs.tenant.mapper.DashboardMapper;
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
    
    @Override
    public DashboardStatsDTO getDashboardStats() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("查询租户 {} 的Dashboard统计数据", tenantId);
        
        // 获取今日开始和结束时间
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        
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
        } catch (Exception e) {
            log.error("查询最近订单失败", e);
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
