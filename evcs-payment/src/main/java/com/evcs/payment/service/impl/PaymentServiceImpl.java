package com.evcs.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.channel.AlipayChannelService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.evcs.payment.service.channel.WechatPayChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements IPaymentService {

    private final AlipayChannelService alipayChannelService;
    private final WechatPayChannelService wechatPayChannelService;
    private final PaymentMetrics paymentMetrics;

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createPayment(PaymentRequest request) {
        paymentMetrics.recordPaymentRequest();
        
        log.info("创建支付订单: orderId={}, amount={}, method={}", 
            request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        io.micrometer.core.instrument.Timer.Sample sample = 
            io.micrometer.core.instrument.Timer.start();
        
        try {
            // 幂等性检查：如果使用相同的幂等键，返回原订单
            if (request.getIdempotentKey() != null) {
                PaymentOrder existingOrder = baseMapper.selectOne(
                    new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getIdempotentKey, request.getIdempotentKey())
                        .eq(PaymentOrder::getTenantId, TenantContext.getCurrentTenantId())
                );
                if (existingOrder != null) {
                    log.info("幂等键已存在，返回原订单: tradeNo={}", existingOrder.getTradeNo());
                    return buildPaymentResponse(existingOrder);
                }
            }

            // 选择支付渠道
            IPaymentChannel channel = selectChannel(request.getPaymentMethod());

            // 调用支付渠道创建支付
            PaymentResponse channelResponse = channel.createPayment(request);

            // 保存支付订单
            PaymentOrder paymentOrder = new PaymentOrder();
            paymentOrder.setTenantId(TenantContext.getCurrentTenantId());
            paymentOrder.setOrderId(request.getOrderId());
            paymentOrder.setTradeNo(channelResponse.getTradeNo());
            paymentOrder.setPaymentMethod(request.getPaymentMethod().getCode());
            paymentOrder.setAmount(request.getAmount());
            paymentOrder.setStatusEnum(PaymentStatus.PENDING);
            paymentOrder.setIdempotentKey(request.getIdempotentKey());
            paymentOrder.setDescription(request.getDescription());
            paymentOrder.setPayParams(channelResponse.getPayParams());
            paymentOrder.setPayUrl(channelResponse.getPayUrl());
            paymentOrder.setCreateBy(TenantContext.getCurrentUserId());

            baseMapper.insert(paymentOrder);

            channelResponse.setPaymentId(paymentOrder.getId());
            
            // 记录监控指标
            String channelName = request.getPaymentMethod().name().toLowerCase();
            Long amountInCents = request.getAmount().multiply(new java.math.BigDecimal("100")).longValue();
            paymentMetrics.recordPaymentSuccess(channelName, amountInCents);
            
            log.info("支付订单创建成功: paymentId={}, tradeNo={}", 
                paymentOrder.getId(), channelResponse.getTradeNo());
            
            return channelResponse;
        } catch (Exception e) {
            String channelName = request.getPaymentMethod().name().toLowerCase();
            paymentMetrics.recordPaymentFailure(channelName);
            log.error("创建支付订单失败: orderId={}", request.getOrderId(), e);
            throw e;
        } finally {
            sample.stop(paymentMetrics.getPaymentProcessTimer());
        }
    }

    @Override
    @DataScope
    public PaymentResponse queryPayment(String tradeNo) {
        log.info("查询支付状态: tradeNo={}", tradeNo);

        PaymentOrder paymentOrder = baseMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getTradeNo, tradeNo)
        );

        if (paymentOrder == null) {
            log.warn("支付订单不存在: tradeNo={}", tradeNo);
            return null;
        }

        return buildPaymentResponse(paymentOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentCallback(String tradeNo, boolean success) {
        log.info("处理支付回调: tradeNo={}, success={}", tradeNo, success);

        PaymentOrder paymentOrder = baseMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getTradeNo, tradeNo)
        );

        if (paymentOrder == null) {
            log.warn("支付订单不存在: tradeNo={}", tradeNo);
            return false;
        }

        // 幂等性检查：如果已经是成功状态，直接返回
        if (PaymentStatus.SUCCESS.equals(paymentOrder.getStatusEnum())) {
            log.info("支付订单已经是成功状态，跳过: tradeNo={}", tradeNo);
            return true;
        }

        // 更新支付状态
        paymentOrder.setStatusEnum(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        if (success) {
            paymentOrder.setPaidTime(LocalDateTime.now());
        }
        paymentOrder.setUpdateBy(TenantContext.getCurrentUserId());

        boolean updated = baseMapper.updateById(paymentOrder) > 0;

        if (updated && success) {
            log.info("支付成功，触发业务回调: tradeNo={}, orderId={}", tradeNo, paymentOrder.getOrderId());
            // TODO: 发送消息到订单服务更新订单状态
        }

        return updated;
    }

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public RefundResponse refund(RefundRequest request) {
        log.info("处理退款请求: paymentId={}, amount={}", 
            request.getPaymentId(), request.getRefundAmount());

        // 查询支付订单
        PaymentOrder paymentOrder = baseMapper.selectById(request.getPaymentId());
        if (paymentOrder == null) {
            throw new IllegalArgumentException("支付订单不存在");
        }

        // 检查支付状态
        if (!PaymentStatus.SUCCESS.equals(paymentOrder.getStatusEnum())) {
            throw new IllegalStateException("支付订单状态不允许退款");
        }

        // 检查退款金额
        if (request.getRefundAmount().compareTo(paymentOrder.getAmount()) > 0) {
            throw new IllegalArgumentException("退款金额超过支付金额");
        }

        // 选择支付渠道
        PaymentMethod method = PaymentMethod.valueOf(
            paymentOrder.getPaymentMethod().toUpperCase().replace("_", "_")
        );
        IPaymentChannel channel = selectChannel(method);

        // 调用支付渠道退款
        RefundResponse refundResponse = channel.refund(request);

        // 更新支付订单
        paymentOrder.setStatusEnum(PaymentStatus.REFUNDED);
        paymentOrder.setRefundAmount(request.getRefundAmount());
        paymentOrder.setRefundTime(LocalDateTime.now());
        paymentOrder.setUpdateBy(TenantContext.getCurrentUserId());

        baseMapper.updateById(paymentOrder);

        log.info("退款成功: paymentId={}, refundNo={}", 
            request.getPaymentId(), refundResponse.getRefundNo());

        return refundResponse;
    }

    @Override
    @DataScope
    public PaymentOrder getByOrderId(Long orderId) {
        return baseMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderId, orderId)
                .orderByDesc(PaymentOrder::getCreateTime)
                .last("LIMIT 1")
        );
    }

    @Override
    @DataScope
    public PaymentOrder getByTradeNo(String tradeNo) {
        return baseMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getTradeNo, tradeNo)
        );
    }

    @Override
    public boolean updatePaymentOrder(PaymentOrder paymentOrder) {
        return baseMapper.updateById(paymentOrder) > 0;
    }

    @Override
    public IPaymentChannel selectChannel(PaymentMethod method) {
        if (method.name().startsWith("ALIPAY")) {
            return alipayChannelService;
        } else if (method.name().startsWith("WECHAT")) {
            return wechatPayChannelService;
        } else {
            throw new IllegalArgumentException("不支持的支付方式: " + method);
        }
    }

    /**
     * 选择支付渠道（私有方法，保持向后兼容）
     */
    private IPaymentChannel selectChannelPrivate(PaymentMethod method) {
        return selectChannel(method);
    }

    /**
     * 构建支付响应
     */
    private PaymentResponse buildPaymentResponse(PaymentOrder order) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(order.getId());
        response.setTradeNo(order.getTradeNo());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatusEnum());
        response.setPayParams(order.getPayParams());
        response.setPayUrl(order.getPayUrl());
        return response;
    }
}
