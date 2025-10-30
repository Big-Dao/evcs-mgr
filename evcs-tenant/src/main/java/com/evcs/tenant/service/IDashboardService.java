package com.evcs.tenant.service;

import com.evcs.tenant.dto.ChargerUtilizationDTO;
import com.evcs.tenant.dto.DashboardStatsDTO;
import com.evcs.tenant.dto.RecentOrderDTO;
import com.evcs.tenant.dto.StationRankingDTO;

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
    
    /**
     * 获取充电站订单排名（Top 5）
     * @return 充电站排名列表
     */
    List<StationRankingDTO> getStationRanking();
    
    /**
     * 获取充电桩利用率排名（Top 5）
     * @return 充电桩利用率列表
     */
    List<ChargerUtilizationDTO> getChargerUtilization();
}
