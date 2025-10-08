package com.evcs.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.evcs.order.entity.ChargingOrder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface IChargingOrderService extends IService<ChargingOrder> {
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
}
