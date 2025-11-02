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
import com.evcs.payment.service.PaymentIdempotencyService;
import com.evcs.payment.service.channel.AlipayChannelService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.evcs.payment.service.channel.WechatPayChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final PaymentIdempotencyService idempotencyService;

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createPayment(PaymentRequest request) {
        paymentMetrics.recordPaymentRequest();

        log.info("创建支付订单: orderId={}, amount={}, method={}",
            request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        io.micrometer.core.instrument.Timer.Sample sample =
            io.micrometer.core.instrument.Timer.start();

        String requestId = java.util.UUID.randomUUID().toString();

        try {
            // 1. 参数验证
            if (!validatePaymentRequest(request)) {
                paymentMetrics.recordPaymentFailure(null);
                throw new IllegalArgumentException("支付请求参数验证失败");
            }

            // 2. 生成或验证幂等键
            String idempotentKey = idempotencyService.generateIdempotentKey(request);
            if (idempotentKey == null) {
                paymentMetrics.recordPaymentFailure(null);
                throw new IllegalStateException("生成幂等键失败");
            }

            log.debug("使用幂等键: idempotentKey={}, requestId={}", idempotentKey, requestId);

            // 3. 增强的幂等性检查 - 先从缓存检查
            PaymentOrder existingOrder = idempotencyService.getExistingPayment(idempotentKey);
            if (existingOrder != null) {
                log.info("幂等键已存在（缓存命中），返回原订单: tradeNo={}, status={}",
                    existingOrder.getTradeNo(), existingOrder.getStatusEnum());

                // 记录幂等性命中指标
                paymentMetrics.recordCustomMetric("payment.idempotency.hit", 1.0,
                    java.util.Map.of("source", "cache", "operation", "create_payment"));

                return buildPaymentResponse(existingOrder);
            }

            // 4. 尝试获取分布式锁，防止并发创建
            if (!idempotencyService.tryLock(idempotentKey, requestId, 30L)) {
                log.warn("获取分布式锁失败，可能有并发请求: idempotentKey={}", idempotentKey);
                paymentMetrics.recordCustomMetric("payment.idempotency.lock.failure", 1.0,
                    java.util.Map.of("operation", "create_payment"));

                // 尝试再次查询，可能有其他请求已经创建了订单
                PaymentOrder retryOrder = idempotencyService.getExistingPayment(idempotentKey);
                if (retryOrder != null) {
                    log.info("锁失败后重试查询成功: tradeNo={}", retryOrder.getTradeNo());
                    return buildPaymentResponse(retryOrder);
                }

                throw new IllegalStateException("系统繁忙，请稍后重试");
            }

            try {
                // 5. 双重检查 - 再次确认幂等性
                existingOrder = idempotencyService.getExistingPayment(idempotentKey);
                if (existingOrder != null) {
                    log.info("获取锁后再次检查发现已存在订单: tradeNo={}", existingOrder.getTradeNo());
                    return buildPaymentResponse(existingOrder);
                }

                // 6. 选择支付渠道
                IPaymentChannel channel = selectChannel(request.getPaymentMethod());

                // 7. 调用支付渠道创建支付
                PaymentResponse channelResponse = channel.createPayment(request);

                // 8. 保存支付订单
                PaymentOrder paymentOrder = new PaymentOrder();
                paymentOrder.setTenantId(TenantContext.getCurrentTenantId());
                paymentOrder.setOrderId(request.getOrderId());
                paymentOrder.setTradeNo(channelResponse.getTradeNo());
                paymentOrder.setPaymentMethod(request.getPaymentMethod().getCode());
                paymentOrder.setAmount(request.getAmount());
                paymentOrder.setStatusEnum(PaymentStatus.PENDING);
                paymentOrder.setIdempotentKey(idempotentKey);
                paymentOrder.setDescription(request.getDescription());
                paymentOrder.setPayParams(channelResponse.getPayParams());
                paymentOrder.setPayUrl(channelResponse.getPayUrl());
                paymentOrder.setCreateBy(TenantContext.getCurrentUserId());

                baseMapper.insert(paymentOrder);

                // 9. 缓存支付结果
                idempotencyService.cachePaymentResult(idempotentKey, paymentOrder, 24, java.util.concurrent.TimeUnit.HOURS);

                channelResponse.setPaymentId(paymentOrder.getId());

                // 10. 记录成功指标
                String channelName = request.getPaymentMethod().name().toLowerCase();
                Long amountInCents = request.getAmount().multiply(new java.math.BigDecimal("100")).longValue();
                paymentMetrics.recordPaymentSuccess(channelName, amountInCents);

                // 记录幂等性新创建指标
                paymentMetrics.recordCustomMetric("payment.idempotency.new_order", 1.0,
                    java.util.Map.of("payment_method", channelName, "operation", "create_payment"));

                log.info("支付订单创建成功: paymentId={}, tradeNo={}, idempotentKey={}",
                    paymentOrder.getId(), channelResponse.getTradeNo(), idempotentKey);

                return channelResponse;

            } finally {
                // 11. 释放分布式锁
                idempotencyService.unlock(idempotentKey, requestId);
            }

        } catch (Exception e) {
            String channelName = request != null && request.getPaymentMethod() != null ?
                request.getPaymentMethod().name().toLowerCase() : "unknown";
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

        // 先从缓存查询订单，提升性能
        String cacheKey = "payment:callback:" + tradeNo;
        PaymentOrder paymentOrder = null;

        // 尝试从缓存获取订单
        try {
            paymentOrder = baseMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTradeNo, tradeNo)
            );

            if (paymentOrder != null && paymentOrder.getIdempotentKey() != null) {
                // 如果有幂等键，可以查询缓存
                PaymentOrder cachedOrder = idempotencyService.getExistingPayment(paymentOrder.getIdempotentKey());
                if (cachedOrder != null && cachedOrder.getTradeNo().equals(tradeNo)) {
                    paymentOrder = cachedOrder;
                    log.debug("从缓存获取支付订单: tradeNo={}", tradeNo);
                }
            }
        } catch (Exception e) {
            log.warn("查询缓存失败，使用数据库查询: tradeNo={}", tradeNo, e);
            // 降级到数据库查询
            paymentOrder = baseMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTradeNo, tradeNo)
            );
        }

        if (paymentOrder == null) {
            log.warn("支付订单不存在: tradeNo={}", tradeNo);
            paymentMetrics.recordCallbackFailure();
            return false;
        }

        // 增强的幂等性检查：如果已经是最终状态，直接返回
        if (PaymentStatus.SUCCESS.equals(paymentOrder.getStatusEnum()) ||
            PaymentStatus.FAILED.equals(paymentOrder.getStatusEnum())) {
            log.info("支付订单已经是最终状态，跳过回调处理: tradeNo={}, status={}",
                tradeNo, paymentOrder.getStatusEnum());
            paymentMetrics.recordCallbackSuccess();
            return true;
        }

        // 获取分布式锁进行回调处理，防止并发回调
        String lockKey = "payment:callback:" + tradeNo;
        String requestId = java.util.UUID.randomUUID().toString();

        if (!idempotencyService.tryLock(lockKey, requestId, 10L)) {
            log.warn("获取回调处理锁失败，可能有并发回调: tradeNo={}", tradeNo);
            paymentMetrics.recordCustomMetric("payment.callback.lock.failure", 1.0,
                java.util.Map.of("operation", "handle_callback"));

            // 重试查询订单状态
            PaymentOrder retryOrder = baseMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTradeNo, tradeNo)
            );

            if (retryOrder != null && (PaymentStatus.SUCCESS.equals(retryOrder.getStatusEnum()) ||
                PaymentStatus.FAILED.equals(retryOrder.getStatusEnum()))) {
                log.info("锁失败后重试查询发现订单已处理: tradeNo={}, status={}",
                    tradeNo, retryOrder.getStatusEnum());
                return true;
            }

            return false;
        }

        try {
            // 双重检查 - 再次确认订单状态
            PaymentOrder latestOrder = baseMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTradeNo, tradeNo)
            );

            if (latestOrder != null && (PaymentStatus.SUCCESS.equals(latestOrder.getStatusEnum()) ||
                PaymentStatus.FAILED.equals(latestOrder.getStatusEnum()))) {
                log.info("获取锁后发现订单已处理: tradeNo={}, status={}",
                    tradeNo, latestOrder.getStatusEnum());
                return true;
            }

            // 更新支付状态
            PaymentStatus newStatus = success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            paymentOrder.setStatusEnum(newStatus);
            if (success) {
                paymentOrder.setPaidTime(LocalDateTime.now());
            }

            baseMapper.updateById(paymentOrder);

            // 更新缓存
            if (paymentOrder.getIdempotentKey() != null) {
                idempotencyService.cachePaymentResult(paymentOrder.getIdempotentKey(), paymentOrder, 24, java.util.concurrent.TimeUnit.HOURS);
            }

            // 记录指标
            if (success) {
                paymentMetrics.recordCallbackSuccess();
                log.info("支付回调处理成功: tradeNo={}", tradeNo);
            } else {
                paymentMetrics.recordCallbackFailure();
                log.warn("支付回调处理失败: tradeNo={}", tradeNo);
            }

            return success;

        } finally {
            // 释放回调处理锁
            idempotencyService.unlock(lockKey, requestId);
        }
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
     * 验证支付请求参数
     */
    private boolean validatePaymentRequest(PaymentRequest request) {
        if (request == null) {
            log.warn("支付请求为空");
            return false;
        }

        if (request.getOrderId() == null) {
            log.warn("订单ID为空");
            return false;
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("支付金额无效: {}", request.getAmount());
            return false;
        }

        if (request.getPaymentMethod() == null) {
            log.warn("支付方式为空");
            return false;
        }

        if (request.getUserId() == null) {
            log.warn("用户ID为空");
            return false;
        }

        return true;
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
