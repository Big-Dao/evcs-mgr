package com.evcs.payment.service;

import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.entity.PaymentOrder;

import java.util.concurrent.TimeUnit;

/**
 * 支付幂等性服务接口
 *
 * 提供支付操作的幂等性保证，防止重复操作
 */
public interface PaymentIdempotencyService {

    /**
     * 检查并创建幂等性锁
     *
     * @param idempotentKey 幂等键
     * @param requestId 请求ID
     * @param lockTime 锁定时间（秒）
     * @return 是否成功获取锁
     */
    boolean tryLock(String idempotentKey, String requestId, long lockTime);

    /**
     * 释放幂等性锁
     *
     * @param idempotentKey 幂等键
     * @param requestId 请求ID
     */
    void unlock(String idempotentKey, String requestId);

    /**
     * 检查幂等键是否已存在支付订单
     *
     * @param idempotentKey 幂等键
     * @return 已存在的支付订单，如果不存在则返回null
     */
    PaymentOrder getExistingPayment(String idempotentKey);

    /**
     * 缓存支付订单结果
     *
     * @param idempotentKey 幂等键
     * @param paymentOrder 支付订单
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    void cachePaymentResult(String idempotentKey, PaymentOrder paymentOrder, long expireTime, TimeUnit timeUnit);

    /**
     * 获取缓存的支付结果
     *
     * @param idempotentKey 幂等键
     * @return 缓存的支付订单，如果不存在则返回null
     */
    PaymentOrder getCachedPaymentResult(String idempotentKey);

    /**
     * 生成幂等键
     *
     * @param request 支付请求
     * @return 幂等键
     */
    String generateIdempotentKey(PaymentRequest request);

    /**
     * 验证幂等键格式
     *
     * @param idempotentKey 幂等键
     * @return 是否有效
     */
    boolean validateIdempotentKey(String idempotentKey);

    /**
     * 清理过期的幂等性缓存
     *
     * @param expireBefore 过期时间点
     * @return 清理的数量
     */
    int cleanExpiredCache(java.time.LocalDateTime expireBefore);

    /**
     * 检查请求是否重复
     *
     * @param idempotentKey 幂等键
     * @param requestId 请求ID
     * @return 是否重复请求
     */
    boolean isDuplicateRequest(String idempotentKey, String requestId);

    /**
     * 记录幂等性使用指标
     *
     * @param idempotentKey 幂等键
     * @param hit 是否命中缓存
     * @param processTime 处理时间（毫秒）
     */
    void recordIdempotencyMetrics(String idempotentKey, boolean hit, long processTime);
}