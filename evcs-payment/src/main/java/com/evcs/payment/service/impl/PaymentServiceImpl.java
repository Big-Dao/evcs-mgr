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
import com.evcs.payment.exception.PaymentUnknownStateException;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.PaymentIdempotencyService;
import com.evcs.payment.service.message.PaymentMessageService;
import com.evcs.payment.service.channel.AlipayChannelService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.evcs.payment.service.channel.WechatPayChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Objects;

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
    private final PaymentMessageService paymentMessageService;

    @Override
    @DataScope
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createPayment(PaymentRequest request) {
        paymentMetrics.recordPaymentRequest();

        log.info("创建支付订单: orderId={}, amount={}, method={}",
            request.getOrderId(), request.getAmount(), request.getPaymentMethod());

        io.micrometer.core.instrument.Timer.Sample sample =
            io.micrometer.core.instrument.Timer.start();

        String requestId = UUID.randomUUID().toString();

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

        try {
            // 3. 幂等性检查 - 如已存在且无需恢复则直接返回
            PaymentOrder existingOrder = idempotencyService.getExistingPayment(idempotentKey);
            if (existingOrder != null && !needsCreateRecovery(existingOrder)) {
                log.info("幂等键已存在，返回原订单: tradeNo={}, status={}",
                    existingOrder.getTradeNo(), existingOrder.getStatusEnum());
                paymentMetrics.recordCustomMetric("payment.idempotency.hit", 1.0,
                    java.util.Map.of("source", "cache", "operation", "create_payment"));
                return buildPaymentResponse(existingOrder);
            }

            // 4. 分布式锁，防止并发创建/并发恢复
            if (!idempotencyService.tryLock(idempotentKey, requestId, 30L)) {
                log.warn("获取分布式锁失败，可能有并发请求: idempotentKey={}", idempotentKey);
                paymentMetrics.recordCustomMetric("payment.idempotency.lock.failure", 1.0,
                    java.util.Map.of("operation", "create_payment"));

                PaymentOrder retryOrder = idempotencyService.getExistingPayment(idempotentKey);
                if (retryOrder != null) {
                    log.info("锁失败后查询成功: tradeNo={}, status={}", retryOrder.getTradeNo(), retryOrder.getStatusEnum());
                    return buildPaymentResponse(retryOrder);
                }

                throw new IllegalStateException("系统繁忙，请稍后重试");
            }

            try {
                // 5. 双重检查
                existingOrder = idempotencyService.getExistingPayment(idempotentKey);

                PaymentOrder paymentOrder;
                boolean recovering = existingOrder != null;
                if (recovering) {
                    paymentOrder = existingOrder;
                } else {
                    // 6. DB-first：先落库为PROCESSING，避免下单异常导致本地无单
                    paymentOrder = new PaymentOrder();
                    paymentOrder.setTenantId(TenantContext.getCurrentTenantId());
                    paymentOrder.setOrderId(request.getOrderId());
                    paymentOrder.setTradeNo(resolveOrGenerateTradeNo(request, idempotentKey));
                    paymentOrder.setPaymentMethod(request.getPaymentMethod().getCode());
                    paymentOrder.setAmount(request.getAmount());
                    paymentOrder.setStatusEnum(PaymentStatus.PROCESSING);
                    paymentOrder.setIdempotentKey(idempotentKey);
                    paymentOrder.setDescription(request.getDescription());
                    paymentOrder.setCreateBy(TenantContext.getCurrentUserId());

                    baseMapper.insert(paymentOrder);
                    idempotencyService.cachePaymentResult(idempotentKey, paymentOrder, 24, java.util.concurrent.TimeUnit.HOURS);
                }

                paymentOrder = Objects.requireNonNull(paymentOrder, "paymentOrder must not be null");

                // 7. 选择支付渠道，并确保tradeNo稳定传入渠道（outTradeNo）
                IPaymentChannel channel = selectChannel(request.getPaymentMethod());
                request.setTradeNo(paymentOrder.getTradeNo());

                try {
                    // 8. 调用支付渠道创建支付（不自动重试；未知状态则转入PROCESSING）
                    PaymentResponse channelResponse = channel.createPayment(request);

                    // 9. 渠道成功返回：更新订单为PENDING，并保存支付参数
                    paymentOrder.setStatusEnum(PaymentStatus.PENDING);
                    paymentOrder.setPayParams(channelResponse.getPayParams());
                    paymentOrder.setPayUrl(channelResponse.getPayUrl());
                    paymentOrder.setUpdateBy(TenantContext.getCurrentUserId());
                    baseMapper.updateById(paymentOrder);
                    idempotencyService.cachePaymentResult(idempotentKey, paymentOrder, 24, java.util.concurrent.TimeUnit.HOURS);

                    channelResponse.setPaymentId(paymentOrder.getId());
                    channelResponse.setTradeNo(paymentOrder.getTradeNo());
                    channelResponse.setStatus(PaymentStatus.PENDING);

                    // 10. 记录成功指标
                    String channelName = request.getPaymentMethod().name().toLowerCase();
                    Long amountInCents = request.getAmount().multiply(new java.math.BigDecimal("100")).longValue();
                    paymentMetrics.recordPaymentSuccess(channelName, amountInCents);
                    paymentMetrics.recordCustomMetric("payment.idempotency.new_order", recovering ? 0.0 : 1.0,
                        java.util.Map.of("payment_method", channelName, "operation", "create_payment"));

                    log.info("支付订单创建成功: paymentId={}, tradeNo={}, idempotentKey={}, recovering={}",
                        paymentOrder.getId(), paymentOrder.getTradeNo(), idempotentKey, recovering);
                    return channelResponse;

                } catch (PaymentUnknownStateException ex) {
                    // 11. 未知状态：不回滚、不重试，保留PROCESSING并返回可追踪tradeNo
                    paymentOrder.setStatusEnum(PaymentStatus.PROCESSING);
                    paymentOrder.setUpdateBy(TenantContext.getCurrentUserId());
                    baseMapper.updateById(paymentOrder);
                    idempotencyService.cachePaymentResult(idempotentKey, paymentOrder, 24, java.util.concurrent.TimeUnit.HOURS);
                    paymentMetrics.recordCustomMetric("payment.create.unknown_state", 1.0,
                        java.util.Map.of("payment_method", request.getPaymentMethod().name().toLowerCase()));
                    log.warn("创建支付进入未知状态（将等待后续补偿/重试调用恢复）: paymentId={}, tradeNo={}, idempotentKey={}",
                        paymentOrder.getId(), paymentOrder.getTradeNo(), idempotentKey, ex);
                    return buildPaymentResponse(paymentOrder);
                }

            } finally {
                idempotencyService.unlock(idempotentKey, requestId);
            }

        } catch (Exception e) {
            String channelName = request != null && request.getPaymentMethod() != null ?
                request.getPaymentMethod().name().toLowerCase() : "unknown";
            paymentMetrics.recordPaymentFailure(channelName);
            log.error("创建支付订单失败: orderId={}", request != null ? request.getOrderId() : null, e);
            throw e;
        } finally {
            sample.stop(paymentMetrics.getPaymentProcessTimer());
        }
    }

    private boolean needsCreateRecovery(PaymentOrder order) {
        if (order == null) {
            return false;
        }
        if (!PaymentStatus.PROCESSING.equals(order.getStatusEnum())) {
            return false;
        }
        boolean hasPayInfo = StringUtils.hasText(order.getPayParams()) || StringUtils.hasText(order.getPayUrl());
        return !hasPayInfo && StringUtils.hasText(order.getTradeNo());
    }

    private String resolveOrGenerateTradeNo(PaymentRequest request, String idempotentKey) {
        if (request != null && StringUtils.hasText(request.getTradeNo())) {
            return request.getTradeNo();
        }

        String prefix;
        if (request != null && request.getPaymentMethod() != null && request.getPaymentMethod().name().startsWith("WECHAT")) {
            prefix = "WXP";
        } else if (request != null && request.getPaymentMethod() != null && request.getPaymentMethod().name().startsWith("ALIPAY")) {
            prefix = "ALI";
        } else {
            prefix = "PAY";
        }

        String suffix = idempotentKey;
        if (!StringUtils.hasText(suffix) || suffix.length() < 8) {
            suffix = UUID.randomUUID().toString().replace("-", "");
        }
        suffix = suffix.substring(0, 8);

        Long orderId = request != null ? request.getOrderId() : null;
        return prefix + orderId + "_" + suffix;
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
        PaymentStatus currentStatus = paymentOrder.getStatusEnum();
        if (PaymentStatus.SUCCESS.equals(currentStatus) ||
            PaymentStatus.FAILED.equals(currentStatus) ||
            PaymentStatus.CLOSED.equals(currentStatus)) {
            log.info("支付订单已经是最终状态，跳过回调处理: tradeNo={}, status={}",
                tradeNo, currentStatus);
            paymentMetrics.recordCallbackSuccess();
            return PaymentStatus.SUCCESS.equals(currentStatus);
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
                PaymentStatus.FAILED.equals(retryOrder.getStatusEnum()) ||
                PaymentStatus.CLOSED.equals(retryOrder.getStatusEnum()))) {
                log.info("锁失败后重试查询发现订单已处理: tradeNo={}, status={}",
                    tradeNo, retryOrder.getStatusEnum());
                return PaymentStatus.SUCCESS.equals(retryOrder.getStatusEnum());
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
                PaymentStatus.FAILED.equals(latestOrder.getStatusEnum()) ||
                PaymentStatus.CLOSED.equals(latestOrder.getStatusEnum()))) {
                log.info("获取锁后发现订单已处理: tradeNo={}, status={}",
                    tradeNo, latestOrder.getStatusEnum());
                return PaymentStatus.SUCCESS.equals(latestOrder.getStatusEnum());
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

            // 发送消息通知
            try {
                if (success) {
                    paymentMessageService.sendPaymentSuccessMessage(paymentOrder);
                } else {
                    paymentMessageService.sendPaymentFailureMessage(paymentOrder);
                }
            } catch (Exception e) {
                log.error("发送支付状态消息失败: tradeNo={}", tradeNo, e);
                // 不影响主流程
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
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentFinalStatus(String tradeNo, PaymentStatus finalStatus) {
        if (!StringUtils.hasText(tradeNo) || finalStatus == null) {
            return false;
        }
        if (!(PaymentStatus.SUCCESS.equals(finalStatus)
            || PaymentStatus.FAILED.equals(finalStatus)
            || PaymentStatus.CLOSED.equals(finalStatus))) {
            throw new IllegalArgumentException("不支持的支付最终状态: " + finalStatus);
        }

        PaymentOrder paymentOrder = baseMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getTradeNo, tradeNo)
        );

        if (paymentOrder == null) {
            log.warn("支付订单不存在，无法收敛最终态: tradeNo={}, finalStatus={}", tradeNo, finalStatus);
            return false;
        }

        PaymentStatus currentStatus = paymentOrder.getStatusEnum();
        if (PaymentStatus.SUCCESS.equals(currentStatus)
            || PaymentStatus.FAILED.equals(currentStatus)
            || PaymentStatus.CLOSED.equals(currentStatus)
            || PaymentStatus.REFUNDED.equals(currentStatus)) {
            return true;
        }

        String lockKey = "payment:callback:" + tradeNo;
        String requestId = java.util.UUID.randomUUID().toString();

        if (!idempotencyService.tryLock(lockKey, requestId, 10L)) {
            PaymentOrder retryOrder = baseMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTradeNo, tradeNo)
            );
            if (retryOrder == null) {
                return false;
            }
            PaymentStatus retryStatus = retryOrder.getStatusEnum();
            if (PaymentStatus.SUCCESS.equals(retryStatus)
                || PaymentStatus.FAILED.equals(retryStatus)
                || PaymentStatus.CLOSED.equals(retryStatus)
                || PaymentStatus.REFUNDED.equals(retryStatus)) {
                return true;
            }
            return false;
        }

        try {
            PaymentOrder latestOrder = baseMapper.selectOne(
                new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getTradeNo, tradeNo)
            );
            if (latestOrder == null) {
                return false;
            }
            PaymentStatus latestStatus = latestOrder.getStatusEnum();
            if (PaymentStatus.SUCCESS.equals(latestStatus)
                || PaymentStatus.FAILED.equals(latestStatus)
                || PaymentStatus.CLOSED.equals(latestStatus)
                || PaymentStatus.REFUNDED.equals(latestStatus)) {
                return true;
            }

            latestOrder.setStatusEnum(finalStatus);
            if (PaymentStatus.SUCCESS.equals(finalStatus) && latestOrder.getPaidTime() == null) {
                latestOrder.setPaidTime(LocalDateTime.now());
            }
            latestOrder.setUpdateBy(TenantContext.getCurrentUserId());
            baseMapper.updateById(latestOrder);

            if (latestOrder.getIdempotentKey() != null) {
                idempotencyService.cachePaymentResult(
                    latestOrder.getIdempotentKey(),
                    latestOrder,
                    24,
                    java.util.concurrent.TimeUnit.HOURS
                );
            }

            try {
                if (PaymentStatus.SUCCESS.equals(finalStatus)) {
                    paymentMessageService.sendPaymentSuccessMessage(latestOrder);
                } else {
                    paymentMessageService.sendPaymentFailureMessage(latestOrder);
                }
            } catch (Exception e) {
                log.error("发送支付状态消息失败: tradeNo={}", tradeNo, e);
            }

            if (PaymentStatus.SUCCESS.equals(finalStatus)) {
                paymentMetrics.recordCallbackSuccess();
            } else {
                paymentMetrics.recordCallbackFailure();
            }

            log.info("支付最终态收敛完成: tradeNo={}, fromStatus={}, toStatus={}",
                tradeNo,
                latestStatus,
                finalStatus);
            return true;

        } finally {
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

        PaymentStatus currentPaymentStatus = paymentOrder.getStatusEnum();
        // 检查支付状态
        if (!(PaymentStatus.SUCCESS.equals(currentPaymentStatus)
            || PaymentStatus.PARTIALLY_REFUNDED.equals(currentPaymentStatus))) {
            throw new IllegalStateException("支付订单状态不允许退款");
        }

        if (PaymentStatus.REFUNDING.equals(currentPaymentStatus)) {
            throw new IllegalStateException("退款处理中，请稍后再试");
        }

        if (PaymentStatus.REFUNDED.equals(currentPaymentStatus)) {
            throw new IllegalStateException("订单已退款完成");
        }

        if (PaymentStatus.CLOSED.equals(currentPaymentStatus)) {
            throw new IllegalStateException("订单已关闭，不允许退款");
        }

        BigDecimal alreadyRefunded = paymentOrder.getRefundAmount() != null
            ? paymentOrder.getRefundAmount()
            : BigDecimal.ZERO;

        if (request.getRefundAmount() == null || request.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("退款金额无效");
        }

        BigDecimal remainingRefundable = paymentOrder.getAmount().subtract(alreadyRefunded);
        if (remainingRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("无可退金额");
        }

        if (request.getRefundAmount().compareTo(remainingRefundable) > 0) {
            throw new IllegalArgumentException("退款金额超过可退金额");
        }

        // 选择支付渠道
        PaymentMethod method = PaymentMethod.valueOf(
            paymentOrder.getPaymentMethod().toUpperCase().replace("_", "_")
        );
        request.setTotalAmount(paymentOrder.getAmount());
        request.setTradeNo(paymentOrder.getTradeNo());
        request.setTransactionId(paymentOrder.getOutTradeNo());
        IPaymentChannel channel = selectChannel(method);

        // 调用支付渠道退款
        RefundResponse refundResponse = channel.refund(request);
        if (refundResponse == null) {
            throw new IllegalStateException("退款响应为空");
        }

        String refundStatus = refundResponse.getRefundStatus();
        boolean acceptedAsSuccess = refundStatus != null && "SUCCESS".equalsIgnoreCase(refundStatus);
        boolean acceptedAsProcessing = refundStatus != null && "PROCESSING".equalsIgnoreCase(refundStatus);

        if (acceptedAsSuccess) {
            BigDecimal newRefundTotal = alreadyRefunded.add(request.getRefundAmount());
            PaymentStatus newStatus = computeRefundedPaymentStatus(paymentOrder.getAmount(), newRefundTotal);

            paymentOrder.setStatusEnum(newStatus);
            paymentOrder.setRefundAmount(newRefundTotal);
            paymentOrder.setRefundTime(LocalDateTime.now());
            paymentOrder.setUpdateBy(TenantContext.getCurrentUserId());
            baseMapper.updateById(paymentOrder);

            try {
                paymentMessageService.sendRefundSuccessMessage(paymentOrder);
            } catch (Exception e) {
                log.error("发送退款成功消息失败: paymentId={}", request.getPaymentId(), e);
            }

            log.info("退款提交成功（已确认成功）: paymentId={}, refundNo={}, refundTotal={}, status={}",
                request.getPaymentId(),
                refundResponse.getRefundNo(),
                newRefundTotal,
                newStatus);
            return refundResponse;
        }

        if (acceptedAsProcessing) {
            paymentOrder.setStatusEnum(PaymentStatus.REFUNDING);
            paymentOrder.setUpdateBy(TenantContext.getCurrentUserId());
            baseMapper.updateById(paymentOrder);

            log.info("退款提交成功（处理中）: paymentId={}, refundNo={}",
                request.getPaymentId(),
                refundResponse.getRefundNo());
            return refundResponse;
        }

        log.warn("退款提交未确认成功: paymentId={}, refundNo={}, refundStatus={}",
            request.getPaymentId(),
            refundResponse.getRefundNo(),
            refundStatus);
        return refundResponse;
    }

    private PaymentStatus computeRefundedPaymentStatus(BigDecimal totalAmount, BigDecimal refundedAmount) {
        if (totalAmount == null || refundedAmount == null) {
            return PaymentStatus.REFUNDED;
        }
        if (refundedAmount.compareTo(totalAmount) >= 0) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.PARTIALLY_REFUNDED;
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

        if (request.getPaymentMethod().name().startsWith("WECHAT")) {
            if (request.getWechatOptions() == null) {
                log.warn("微信支付缺少专属参数");
                return false;
            }
            if (request.getPaymentMethod() == PaymentMethod.WECHAT_JSAPI) {
                if (!StringUtils.hasText(request.getWechatOptions().getAppId())) {
                    log.warn("微信JSAPI支付缺少appId");
                    return false;
                }
                if (!StringUtils.hasText(request.getWechatOptions().getOpenId())) {
                    log.warn("微信JSAPI支付缺少openId");
                    return false;
                }
            }
        }

        return true;
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
