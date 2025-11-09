package com.evcs.payment.service.impl;

import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.PaymentIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 支付幂等性服务实现
 *
 * 基于Redis实现分布式幂等性控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentIdempotencyServiceImpl implements PaymentIdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentMetrics paymentMetrics;

    // Redis键前缀
    private static final String LOCK_KEY_PREFIX = "payment:lock:";
    private static final String CACHE_KEY_PREFIX = "payment:cache:";

    // 默认过期时间
    private static final long DEFAULT_CACHE_TIME = 24; // 24小时
    private static final long MAX_IDEMPOTENT_KEY_LENGTH = 64;

    @Override
    public boolean tryLock(String idempotentKey, String requestId, long lockTime) {
        if (!StringUtils.hasText(idempotentKey) || !StringUtils.hasText(requestId)) {
            return false;
        }

        String lockKey = buildLockKey(idempotentKey);
        try {
            java.util.Objects.requireNonNull(lockKey, "lockKey不能为null");
            java.util.Objects.requireNonNull(requestId, "requestId不能为null");
            
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                requestId,
                lockTime,
                TimeUnit.SECONDS
            );

            if (Boolean.TRUE.equals(success)) {
                log.debug("获取幂等性锁成功: idempotentKey={}, requestId={}", idempotentKey, requestId);
                paymentMetrics.recordCustomMetric("payment.idempotency.lock.success", 1.0,
                    java.util.Map.of("operation", "try_lock"));
            } else {
                log.debug("获取幂等性锁失败: idempotentKey={}, requestId={}", idempotentKey, requestId);
                paymentMetrics.recordCustomMetric("payment.idempotency.lock.failure", 1.0,
                    java.util.Map.of("operation", "try_lock"));
            }

            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("获取幂等性锁异常: idempotentKey={}, requestId={}", idempotentKey, requestId, e);
            paymentMetrics.recordCustomMetric("payment.idempotency.lock.error", 1.0,
                java.util.Map.of("operation", "try_lock"));
            return false;
        }
    }

    @Override
    public void unlock(String idempotentKey, String requestId) {
        if (!StringUtils.hasText(idempotentKey) || !StringUtils.hasText(requestId)) {
            return;
        }

        String lockKey = buildLockKey(idempotentKey);
        try {
            // Lua脚本确保原子性删除（只有持有锁的客户端才能删除）
            String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";

            java.util.Objects.requireNonNull(lockKey, "lockKey不能为null");
            java.util.Objects.requireNonNull(requestId, "requestId不能为null");
            
            // 使用RedisScript替代废弃的eval方法
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            redisScript.setResultType(Long.class);
            
            java.util.List<String> keys = java.util.Collections.singletonList(lockKey);
            java.util.Objects.requireNonNull(keys, "keys不能为null");
            
            Long result = redisTemplate.execute(
                redisScript,
                keys,
                requestId
            );

            if (result != null && result > 0) {
                log.debug("释放幂等性锁成功: idempotentKey={}, requestId={}", idempotentKey, requestId);
                paymentMetrics.recordCustomMetric("payment.idempotency.unlock.success", 1.0,
                    java.util.Map.of("operation", "unlock"));
            } else {
                log.warn("释放幂等性锁失败，可能不是锁持有者: idempotentKey={}, requestId={}", idempotentKey, requestId);
                paymentMetrics.recordCustomMetric("payment.idempotency.unlock.failure", 1.0,
                    java.util.Map.of("operation", "unlock"));
            }
        } catch (Exception e) {
            log.error("释放幂等性锁异常: idempotentKey={}, requestId={}", idempotentKey, requestId, e);
            paymentMetrics.recordCustomMetric("payment.idempotency.unlock.error", 1.0,
                java.util.Map.of("operation", "unlock"));
        }
    }

    @Override
    public PaymentOrder getExistingPayment(String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey)) {
            return null;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 先从缓存获取
            PaymentOrder cachedOrder = getCachedPaymentResult(idempotentKey);
            if (cachedOrder != null) {
                recordIdempotencyMetrics(idempotentKey, true, System.currentTimeMillis() - startTime);
                log.debug("从缓存获取已存在的支付订单: idempotentKey={}, tradeNo={}",
                    idempotentKey, cachedOrder.getTradeNo());
                return cachedOrder;
            }

            // 缓存未命中，从数据库查询
            PaymentOrder order = paymentOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PaymentOrder>()
                    .eq("idempotent_key", idempotentKey)
                    .eq("deleted", 0)
            );

            if (order != null) {
                // 将结果缓存到Redis
                cachePaymentResult(idempotentKey, order, DEFAULT_CACHE_TIME, TimeUnit.HOURS);
                log.debug("从数据库获取已存在的支付订单并缓存: idempotentKey={}, tradeNo={}",
                    idempotentKey, order.getTradeNo());
            }

            recordIdempotencyMetrics(idempotentKey, false, System.currentTimeMillis() - startTime);
            return order;

        } catch (Exception e) {
            log.error("查询已存在的支付订单失败: idempotentKey={}", idempotentKey, e);
            paymentMetrics.recordCustomMetric("payment.idempotency.query.error", 1.0,
                java.util.Map.of("operation", "get_existing_payment"));
            return null;
        }
    }

    @Override
    public void cachePaymentResult(String idempotentKey, PaymentOrder paymentOrder, long expireTime, TimeUnit timeUnit) {
        if (!StringUtils.hasText(idempotentKey) || paymentOrder == null) {
            return;
        }

        String cacheKey = buildCacheKey(idempotentKey);
        try {
            java.util.Objects.requireNonNull(cacheKey, "cacheKey不能为null");
            java.util.Objects.requireNonNull(timeUnit, "timeUnit不能为null");
            
            redisTemplate.opsForValue().set(cacheKey, paymentOrder, expireTime, timeUnit);
            log.debug("缓存支付结果成功: idempotentKey={}, tradeNo={}, expireTime={}",
                idempotentKey, paymentOrder.getTradeNo(), expireTime);
            paymentMetrics.recordCustomMetric("payment.idempotency.cache.set", 1.0,
                java.util.Map.of("operation", "cache_payment_result"));
        } catch (Exception e) {
            log.error("缓存支付结果失败: idempotentKey={}, tradeNo={}",
                idempotentKey, paymentOrder.getTradeNo(), e);
            paymentMetrics.recordCustomMetric("payment.idempotency.cache.error", 1.0,
                java.util.Map.of("operation", "cache_payment_result"));
        }
    }

    @Override
    public PaymentOrder getCachedPaymentResult(String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey)) {
            return null;
        }

        String cacheKey = buildCacheKey(idempotentKey);
        try {
            java.util.Objects.requireNonNull(cacheKey, "cacheKey不能为null");
            
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof PaymentOrder) {
                return (PaymentOrder) cached;
            }
            return null;
        } catch (Exception e) {
            log.error("获取缓存的支付结果失败: idempotentKey={}", idempotentKey, e);
            paymentMetrics.recordCustomMetric("payment.idempotency.cache.error", 1.0,
                java.util.Map.of("operation", "get_cached_result"));
            return null;
        }
    }

    @Override
    public String generateIdempotentKey(PaymentRequest request) {
        if (request == null) {
            return null;
        }

        // 如果请求中已提供幂等键，验证后返回
        if (StringUtils.hasText(request.getIdempotentKey())) {
            String key = request.getIdempotentKey().trim();
            if (validateIdempotentKey(key)) {
                return key;
            }
            log.warn("无效的幂等键格式，将自动生成: providedKey={}", key);
        }

        // 自动生成幂等键：基于订单ID、用户ID、金额、支付方式等
        try {
            String keyData = String.format("%d_%d_%s_%s_%d",
                request.getOrderId(),
                request.getUserId(),
                request.getAmount().toPlainString(),
                request.getPaymentMethod().name(),
                System.currentTimeMillis()
            );

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(keyData.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String generatedKey = hexString.toString().substring(0, Math.min(32, (int) MAX_IDEMPOTENT_KEY_LENGTH));
            log.debug("生成幂等键: orderId={}, userId={}, generatedKey={}",
                request.getOrderId(), request.getUserId(), generatedKey);

            return generatedKey;

        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            log.error("生成幂等键失败，使用UUID作为备选方案", e);
            // 降级方案：使用UUID
            return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }
    }

    @Override
    public boolean validateIdempotentKey(String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey)) {
            return false;
        }

        // 检查长度
        if (idempotentKey.length() > MAX_IDEMPOTENT_KEY_LENGTH) {
            log.warn("幂等键长度超限: length={}, maxLength={}", idempotentKey.length(), MAX_IDEMPOTENT_KEY_LENGTH);
            return false;
        }

        // 检查是否包含非法字符（只允许字母、数字、下划线、连字符）
        if (!idempotentKey.matches("^[a-zA-Z0-9_-]+$")) {
            log.warn("幂等键包含非法字符: idempotentKey={}", idempotentKey);
            return false;
        }

        return true;
    }

    @Override
    public int cleanExpiredCache(LocalDateTime expireBefore) {
        // Redis的TTL机制会自动清理过期缓存，这里主要是为了清理数据库中可能存在的过期数据
        log.info("Redis缓存自动过期，无需手动清理: expireBefore={}", expireBefore);
        return 0;
    }

    @Override
    public boolean isDuplicateRequest(String idempotentKey, String requestId) {
        if (!StringUtils.hasText(idempotentKey) || !StringUtils.hasText(requestId)) {
            return false;
        }

        try {
            // 检查是否已有相同幂等键的请求在处理
            String lockKey = buildLockKey(idempotentKey);
            java.util.Objects.requireNonNull(lockKey, "lockKey不能为null");
            
            Object existingRequestId = redisTemplate.opsForValue().get(lockKey);

            boolean isDuplicate = requestId.equals(existingRequestId);
            if (isDuplicate) {
                log.debug("检测到重复请求: idempotentKey={}, requestId={}", idempotentKey, requestId);
                paymentMetrics.recordCustomMetric("payment.idempotency.duplicate.request", 1.0,
                    java.util.Map.of("operation", "duplicate_check"));
            }

            return isDuplicate;
        } catch (Exception e) {
            log.error("检查重复请求失败: idempotentKey={}, requestId={}", idempotentKey, requestId, e);
            return false;
        }
    }

    @Override
    public void recordIdempotencyMetrics(String idempotentKey, boolean hit, long processTime) {
        try {
            // 记录缓存命中率
            paymentMetrics.recordCustomMetric("payment.idempotency.cache.hit", hit ? 1.0 : 0.0,
                java.util.Map.of("result", hit ? "hit" : "miss"));

            // 记录处理时间
            paymentMetrics.recordTimer("payment.idempotency.process.duration", processTime,
                java.util.Map.of("operation", "query_existing_payment"));

            log.debug("记录幂等性指标: idempotentKey={}, hit={}, processTime={}ms",
                idempotentKey, hit, processTime);
        } catch (Exception e) {
            log.error("记录幂等性指标失败: idempotentKey={}", idempotentKey, e);
        }
    }

    // 私有方法：构建锁键
    private String buildLockKey(String idempotentKey) {
        return LOCK_KEY_PREFIX + idempotentKey;
    }

    // 私有方法：构建缓存键
    private String buildCacheKey(String idempotentKey) {
        return CACHE_KEY_PREFIX + idempotentKey;
    }
}