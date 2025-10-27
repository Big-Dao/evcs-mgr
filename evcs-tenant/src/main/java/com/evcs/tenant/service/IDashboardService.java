package com.evcs.tenant.service;

import com.evcs.tenant.dto.DashboardStatsDTO;
import com.evcs.tenant.dto.RecentOrderDTO;

import java.util.List;

/**
 * Dashboard统计服务接口
 */
public interface IDashboardService {
    
    /**
     * 获取Dashboard统计数据
     * @return 统计数据
     */
    DashboardStatsDTO getDashboardStats();
    
    /**
     * 获取最近订单列表
     * @param limit 返回记录数量
     * @return 最近订单列表
     */
    List<RecentOrderDTO> getRecentOrders(Integer limit);
}
