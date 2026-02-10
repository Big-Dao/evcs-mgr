# EVCS 分布式锁使用规范

> **版本**: v1.1
> **创建日期**: 2026-01-13
> **更新日期**: 2026-02-10
> **维护者**: 架构团队
> **状态**: 已发布

---

## 1. 概述

本文档定义 EVCS 充电站管理系统中分布式锁的使用规范，确保并发操作的正确性和一致性。

### 1.1 适用场景

| 场景 | 锁类型 | 说明 | 实现状态 |
|------|--------|------|----------|
| 充电桩启停操作 | 互斥锁 | 防止并发启停 | ✅ 已实现 |
| 订单状态变更 | 互斥锁 | 防止重复状态转换 | ⚠️ 待实现 |
| 支付操作 | 互斥锁 | 防止重复扣款 | ⚠️ 待实现 |
| 库存/配额扣减 | 互斥锁 | 防止超卖 | ⚠️ 待实现 |
| 定时任务 | 分布式调度锁 | 单节点执行 | ⚠️ 待实现 |

### 1.2 技术选型

- **Redisson 3.24.3**: ✅ 生产使用，功能完善
- **Spring Integration**: 轻量级场景（备选）
- **数据库锁**: 备选方案

---

## 2. Redisson 配置

### 2.1 Gradle 依赖

```gradle
// evcs-station/build.gradle, evcs-payment/build.gradle
implementation 'org.redisson:redisson-spring-boot-starter:3.24.3'
```

### 2.2 已集成模块

| 模块 | 状态 | 文件位置 |
|------|------|----------|
| evcs-station | ✅ 已集成 | `evcs-station/src/main/java/com/evcs/station/service/impl/ChargerServiceImpl.java:323-452` |
| evcs-payment | ✅ 已集成 | `evcs-payment/build.gradle` |

### 2.3 配置类

```java
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 单节点模式
        config.useSingleServer()
            .setAddress("redis://redis:6379")
            .setDatabase(0)
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(24)
            .setConnectTimeout(10000)
            .setTimeout(3000)
            .setRetryAttempts(3)
            .setRetryInterval(1500);

        // 使用正确的 JsonJacksonCodec 包路径
        config.setCodec(new org.redisson.client.codec.JsonJacksonCodec());

        return Redisson.create(config);
    }
}
```

### 2.4 集群模式配置

```java
@Bean
public RedissonClient redissonClient() {
    Config config = new Config();

    config.useClusterServers()
        .addNodeAddress(
            "redis://redis-1:6379",
            "redis://redis-2:6379",
            "redis://redis-3:6379"
        )
        .setScanInterval(2000)
        .setMasterConnectionPoolSize(64)
        .setSlaveConnectionPoolSize(64);

    return Redisson.create(config);
}
```

---

## 3. 锁命名规范

### 3.1 命名格式

```
lock:<module>:<resource>:<id>
```

### 3.2 命名示例（已实现）

| 场景 | 锁 Key | 说明 | 实现状态 |
|------|--------|------|----------|
| 充电桩启动 | `charger:lock:start:{chargerId}` | 充电桩启动锁 | ✅ 已实现 |
| 充电桩停止 | `charger:lock:stop:{chargerId}` | 充电桩停止锁 | ✅ 已实现 |
| 充电桩操作 | `lock:charger:{chargerId}` | 充电桩通用锁 | ⚠️ 待实现 |
| 订单状态变更 | `lock:order:status:{orderId}` | 订单状态锁 | ⚠️ 待实现 |
| 支付操作 | `lock:payment:{orderNo}` | 支付锁 | ⚠️ 待实现 |
| 用户余额 | `lock:user:balance:{userId}` | 用户余额锁 | ⚠️ 待实现 |
| 定时任务 | `lock:scheduler:{taskName}` | 调度任务锁 | ⚠️ 待实现 |

---

## 4. 实现示例

### 4.1 充电桩启动锁（已实现）

```java
// evcs-station/src/main/java/com/evcs/station/service/impl/ChargerServiceImpl.java
@Service
@RequiredArgsConstructor
public class ChargerServiceImpl implements IChargerService {

    private final RedissonClient redissonClient;

    @Override
    public Boolean startChargingSession(Long chargerId, String sessionId, Long userId) {
        String lockKey = "charger:lock:start:" + chargerId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待 5 秒，锁定 30 秒
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 检查充电桩状态
                    Charger charger = getChargerOrThrow(chargerId);

                    // 检查是否已有进行中的会话
                    ChargingSession existingSession = getActiveSession(chargerId);
                    if (existingSession != null) {
                        throw new ResourceConflictException("充电桩已有进行中的会话");
                    }

                    // 调用协议栈启动充电
                    Boolean started = invokeStartProtocol(charger, sessionId, userId);
                    if (!started) {
                        throw new ServiceUnavailableException.forService("充电");
                    }

                    // 创建会话记录
                    createSession(charger, sessionId, userId);
                    return true;

                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new ResourceConflictException("充电桩正在处理中，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("获取锁被中断");
        }
    }

    @Override
    public Boolean endChargingSession(Long chargerId, String sessionId, Long userId) {
        String lockKey = "charger:lock:stop:" + chargerId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 停止充电逻辑
                    return doStopCharging(chargerId, sessionId, userId);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new ResourceConflictException("充电桩正在处理中，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("获取锁被中断");
        }
    }
}
```

### 4.2 公平锁

```java
// 公平锁，按请求顺序获取（适用于支付场景）
RLock fairLock = redissonClient.getFairLock("lock:payment:" + orderNo);

try {
    if (fairLock.tryLock(10, 60, TimeUnit.SECONDS)) {
        try {
            processPayment(orderNo);
        } finally {
            fairLock.unlock();
        }
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

### 4.3 读写锁

```java
// 读写锁适用于读多写少场景（如配置读取）
RReadWriteLock rwLock = redissonClient.getReadWriteLock("lock:station:config:" + stationId);

// 读操作
RLock readLock = rwLock.readLock();
try {
    readLock.lock();
    return getStationConfig(stationId);
} finally {
    readLock.unlock();
}

// 写操作
RLock writeLock = rwLock.writeLock();
try {
    writeLock.lock();
    updateStationConfig(stationId, config);
} finally {
    writeLock.unlock();
}
```

---

## 5. 注解方式

### 5.1 自定义锁注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁的 Key，支持 SpEL 表达式
     */
    String key();

    /**
     * 等待锁的最大时间（秒）
     */
    long waitTime() default 5;

    /**
     * 锁的持有时间（秒）
     */
    long leaseTime() default 30;

    /**
     * 获取锁失败时的错误消息
     */
    String errorMessage() default "操作正在处理中，请稍后重试";
}
```

### 5.2 AOP 切面

```java
@Aspect
@Component
@RequiredArgsConstructor
@Order(1) // 在事务之前执行
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {

        // 解析锁 Key
        String lockKey = parseKey(distributedLock.key(), joinPoint);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), TimeUnit.SECONDS)) {
                try {
                    return joinPoint.proceed();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                throw new ResourceConflictException(distributedLock.errorMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("获取锁被中断", e);
        }
    }

    private String parseKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        if (!keyExpression.contains("#")) {
            return keyExpression;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        Expression expression = parser.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}
```

### 5.3 使用示例

```java
@Service
public class ChargerService {

    @DistributedLock(
        key = "'lock:charger:start:' + #chargerId",
        waitTime = 5,
        leaseTime = 30,
        errorMessage = "充电桩正在处理中，请稍后重试"
    )
    public void startCharging(Long chargerId, Long userId) {
        // 业务逻辑
    }

    @DistributedLock(key = "'lock:order:status:' + #orderId")
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        // 业务逻辑
    }
}
```

---

## 6. 定时任务锁

### 6.1 分布式调度锁

```java
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 0 * * * *") // 每小时执行
    public void hourlyTask() {
        String lockKey = "lock:scheduler:hourlyTask";
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，不等待，锁定 50 分钟
            if (lock.tryLock(0, 50, TimeUnit.MINUTES)) {
                try {
                    log.info("开始执行定时任务");
                    executeTask();
                } finally {
                    lock.unlock();
                }
            } else {
                log.info("其他节点正在执行，跳过");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 6.2 使用 @SchedulerLock

```gradle
// build.gradle
implementation 'net.javacrumbs.shedlock:shedlock-spring:5.10.0'
implementation 'net.javacrumbs.shedlock:shedlock-provider-redis-spring:5.10.0'
```

```java
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "evcs");
    }
}

@Component
public class ScheduledTasks {

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "hourlyTask", lockAtLeastFor = "PT5M", lockAtMostFor = "PT50M")
    public void hourlyTask() {
        // 业务逻辑
    }
}
```

---

## 7. 最佳实践

### 7.1 锁粒度

```java
// ✅ 正确：细粒度锁
@DistributedLock(key = "'lock:charger:' + #chargerId")
public void operateCharger(Long chargerId) { }

// ❌ 错误：粗粒度锁（会影响所有充电桩）
@DistributedLock(key = "'lock:charger'")
public void operateCharger(Long chargerId) { }
```

### 7.2 锁超时

| 场景 | 等待时间 | 持有时间 | 说明 |
|------|----------|----------|------|
| 充电桩操作 | 5 秒 | 30 秒 | 已实现 |
| 支付操作 | 10 秒 | 60 秒 | 待实现 |
| 批量操作 | 30 秒 | 300 秒 | 待实现 |
| 定时任务 | 0 秒 | 任务时长 × 1.5 | 待实现 |

### 7.3 异常处理

```java
public void safeOperation(Long resourceId) {
    String lockKey = "lock:resource:" + resourceId;
    RLock lock = redissonClient.getLock(lockKey);

    boolean acquired = false;
    try {
        acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
        if (!acquired) {
            throw new ResourceConflictException("资源正在被其他操作占用");
        }

        // 业务逻辑
        doOperation();

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ServiceUnavailableException("操作被中断", e);
    } catch (Exception e) {
        log.error("操作失败", e);
        throw e;
    } finally {
        if (acquired && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (Exception e) {
                log.warn("释放锁失败", e);
            }
        }
    }
}
```

### 7.4 避免死锁

```java
// ✅ 正确：使用 tryLock 带超时
if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
    // ...
}

// ❌ 错误：无超时等待（可能死锁）
lock.lock();
```

### 7.5 锁续期

```java
// Redisson 默认启用看门狗机制，自动续期
// 如果指定了 leaseTime，则不会自动续期

// 需要自动续期时，不指定 leaseTime
lock.tryLock(5, TimeUnit.SECONDS); // 自动续期，直到主动释放

// 不需要自动续期时，指定 leaseTime
lock.tryLock(5, 30, TimeUnit.SECONDS); // 30 秒后自动释放
```

---

## 8. 监控

### 8.1 锁监控指标

```java
@Component
public class LockMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter lockAcquiredCounter;
    private final Counter lockFailedCounter;
    private final Timer lockWaitTimer;

    public LockMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.lockAcquiredCounter = Counter.builder("distributed.lock.acquired")
            .description("成功获取锁次数")
            .register(meterRegistry);

        this.lockFailedCounter = Counter.builder("distributed.lock.failed")
            .description("获取锁失败次数")
            .register(meterRegistry);

        this.lockWaitTimer = Timer.builder("distributed.lock.wait.time")
            .description("等待锁时间")
            .register(meterRegistry);
    }

    public void recordAcquired(String lockKey, long waitTimeMs) {
        lockAcquiredCounter.increment();
        lockWaitTimer.record(waitTimeMs, TimeUnit.MILLISECONDS);
    }

    public void recordFailed(String lockKey) {
        lockFailedCounter.increment();
    }
}
```

### 8.2 告警规则

```yaml
groups:
  - name: distributed-lock
    rules:
      - alert: HighLockFailureRate
        expr: rate(distributed_lock_failed_total[5m]) / rate(distributed_lock_acquired_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "分布式锁获取失败率过高"

      - alert: LongLockWaitTime
        expr: histogram_quantile(0.99, rate(distributed_lock_wait_time_seconds_bucket[5m])) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "分布式锁等待时间过长"
```

---

## 9. 故障处理

### 9.1 锁未释放

```bash
# 查看 Redis 中的锁
redis-cli keys "lock:*"

# 强制删除锁（谨慎使用）
redis-cli del "lock:charger:12345"
```

### 9.2 Redis 不可用

```java
// 降级处理
public void operationWithFallback(Long resourceId) {
    try {
        executeWithLock(resourceId);
    } catch (RedisConnectionFailureException e) {
        log.warn("Redis 不可用，尝试本地锁降级");
        executeWithLocalLock(resourceId);
    }
}
```

---

## 10. 相关文档

- [系统架构风险审计报告](RISK-AUDIT-REPORT.md)
- [消息队列设计规范](MESSAGE-QUEUE-DESIGN.md)
- [项目编码规范](../overview/PROJECT-CODING-STANDARDS.md)
- [统一异常处理](../overview/ERROR-CODE-STANDARDS.md)

---

## 11. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-02-10 | v1.1 | 更新 Redisson 3.24.3 集成状态、添加实现示例、更新锁命名规范 |
| 2026-01-13 | v1.0 | 初始版本 |
