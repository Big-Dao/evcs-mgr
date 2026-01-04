package com.evcs.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.order.dto.CityOrderStatistics;
import com.evcs.order.dto.OrderDTO;
import com.evcs.order.entity.ChargingOrder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface IChargingOrderService extends IService<ChargingOrder> {
    /**
     * 分页查询订单列表
     * @param page 分页参数
     * @param queryParams 查询参数
     * @return 订单DTO分页
     */
    IPage<OrderDTO> getOrderPage(Page<OrderDTO> page, ChargingOrder queryParams);

    boolean createOrderOnStart(Long stationId, Long chargerId, String sessionId, Long userId, Long billingPlanId);
    boolean completeOrderOnStop(String sessionId, Double energy, Long duration);

    // 状态流转
    boolean markToPay(Long orderId);
    boolean markPaid(Long orderId);

    // 支付占位
    com.evcs.order.dto.PayParams createPayment(Long orderId);
    boolean paymentCallback(String tradeId, boolean success);

    // 取消与退款占位
    boolean cancelOrder(Long orderId);
    boolean markRefunding(Long orderId);
    boolean markRefunded(Long orderId);

    ChargingOrder getBySessionId(String sessionId);
    IPage<ChargingOrder> pageOrders(Page<ChargingOrder> page, Long stationId, Long chargerId, Long userId, Integer status);
    
    /**
     * 获取城市级别订单统计
     * 用于地图可视化分析
     * 
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 城市订单统计列表
     */
    List<CityOrderStatistics> getCityOrderStatistics(LocalDateTime startTime, LocalDateTime endTime);
}
