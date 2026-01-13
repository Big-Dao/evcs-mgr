# EVCS 应急预案

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: 运维团队  
> **状态**: 已发布

---

## 1. 概述

本文档定义 EVCS 充电站管理系统的应急响应预案，包括服务降级、故障转移、数据恢复等应急处理流程。

### 1.1 应急等级

| 等级 | 定义 | 影响范围 | 响应时间 | 升级要求 |
|------|------|----------|----------|----------|
| **L1 紧急** | 系统完全不可用 | 全部用户 | 5 分钟 | 立即通知技术总监 |
| **L2 严重** | 核心功能不可用 | 无法充电/支付 | 15 分钟 | 通知技术主管 |
| **L3 一般** | 部分功能异常 | 功能降级 | 30 分钟 | 值班处理 |
| **L4 轻微** | 非核心问题 | 影响有限 | 工作时间 | 正常流程 |

### 1.2 应急组织

| 角色 | 职责 | 联系方式 |
|------|------|----------|
| 应急总指挥 | 决策、资源协调 | - |
| 技术负责人 | 技术方案、执行指导 | - |
| 值班运维 | 一线响应、初步处理 | - |
| 开发代表 | 代码级问题定位 | - |
| DBA | 数据库问题处理 | - |
| 客服代表 | 用户沟通、公告发布 | - |

---

## 2. 服务降级预案

### 2.1 降级策略矩阵

| 故障类型 | 降级措施 | 影响评估 | 恢复条件 |
|----------|----------|----------|----------|
| 支付服务不可用 | 余额支付优先，第三方降级 | 新用户无法支付 | 支付服务恢复 |
| 监控服务不可用 | 临时关闭监控 | 无实时数据 | 服务恢复 |
| 通知服务不可用 | 异步队列积压 | 消息延迟 | 服务恢复后补发 |
| 协议服务过载 | 限制新连接 | 新设备无法接入 | 负载下降 |
| 数据库高负载 | 只读降级 | 写操作失败 | 负载恢复 |

### 2.2 熔断降级配置

```java
// Sentinel 降级规则
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void initRules() {
        // 支付服务降级规则
        List<DegradeRule> rules = new ArrayList<>();
        
        DegradeRule paymentRule = new DegradeRule();
        paymentRule.setResource("paymentService");
        paymentRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        paymentRule.setCount(0.5); // 50% 错误率
        paymentRule.setTimeWindow(30); // 30 秒熔断
        paymentRule.setMinRequestAmount(10);
        rules.add(paymentRule);
        
        DegradeRuleManager.loadRules(rules);
    }
}

// 降级处理
@Service
public class PaymentService {

    @SentinelResource(
        value = "paymentService",
        fallback = "paymentFallback",
        blockHandler = "paymentBlockHandler"
    )
    public PaymentResult processPayment(PaymentRequest request) {
        return doPayment(request);
    }

    public PaymentResult paymentFallback(PaymentRequest request, Throwable e) {
        log.warn("支付服务降级: {}", e.getMessage());
        // 降级到余额支付
        if (canUseBalance(request.getUserId(), request.getAmount())) {
            return processBalancePayment(request);
        }
        throw new BusinessException(ErrorCode.PAYMENT_CHANNEL_ERROR, "支付服务暂不可用，请稍后重试");
    }

    public PaymentResult paymentBlockHandler(PaymentRequest request, BlockException e) {
        log.warn("支付服务熔断");
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "支付服务繁忙，请稍后重试");
    }
}
```

### 2.3 手动降级开关

```java
@RestController
@RequestMapping("/admin/degrade")
@PreAuthorize("hasRole('ADMIN')")
public class DegradeController {

    @Autowired
    private DegradeManager degradeManager;

    @PostMapping("/enable/{feature}")
    public Result<?> enableDegrade(@PathVariable String feature) {
        degradeManager.enableDegrade(feature);
        log.warn("手动开启降级: {}", feature);
        return Result.success();
    }

    @PostMapping("/disable/{feature}")
    public Result<?> disableDegrade(@PathVariable String feature) {
        degradeManager.disableDegrade(feature);
        log.info("手动关闭降级: {}", feature);
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Map<String, Boolean>> getDegradeStatus() {
        return Result.success(degradeManager.getStatus());
    }
}

@Component
public class DegradeManager {
    
    private final Map<String, AtomicBoolean> degradeStatus = new ConcurrentHashMap<>();

    public void enableDegrade(String feature) {
        degradeStatus.computeIfAbsent(feature, k -> new AtomicBoolean()).set(true);
    }

    public void disableDegrade(String feature) {
        AtomicBoolean status = degradeStatus.get(feature);
        if (status != null) {
            status.set(false);
        }
    }

    public boolean isDegraded(String feature) {
        AtomicBoolean status = degradeStatus.get(feature);
        return status != null && status.get();
    }
}
```

---

## 3. 故障转移预案

### 3.1 数据库故障转移

```bash
#!/bin/bash
# PostgreSQL 主从切换脚本

set -e

STANDBY_HOST="pg-standby"
NEW_PRIMARY_HOST="${STANDBY_HOST}"

echo "=== PostgreSQL 故障转移开始 ==="
echo "$(date): 当前主库不可用，准备切换到 ${NEW_PRIMARY_HOST}"

# 1. 提升从库为主库
ssh postgres@${STANDBY_HOST} "pg_ctl promote -D /var/lib/postgresql/data"
echo "$(date): 从库已提升为主库"

# 2. 更新 DNS 或负载均衡
kubectl patch service evcs-postgres -n evcs -p \
  '{"spec":{"selector":{"app":"postgres-standby"}}}'
echo "$(date): 服务已切换到新主库"

# 3. 通知应用重连
kubectl rollout restart deployment -n evcs
echo "$(date): 应用服务已重启"

# 4. 验证
sleep 30
psql -h ${NEW_PRIMARY_HOST} -U postgres -c "SELECT 1"
echo "$(date): 新主库连接正常"

echo "=== 故障转移完成 ==="
```

### 3.2 Redis 故障转移

```bash
#!/bin/bash
# Redis Sentinel 故障转移

# 检查 Sentinel 状态
redis-cli -h sentinel -p 26379 sentinel master evcs-master

# 手动触发故障转移
redis-cli -h sentinel -p 26379 sentinel failover evcs-master

# 查看新主库
redis-cli -h sentinel -p 26379 sentinel get-master-addr-by-name evcs-master
```

### 3.3 服务故障转移

```bash
#!/bin/bash
# 服务级故障转移

SERVICE=$1
BACKUP_REPLICAS=${2:-2}

echo "服务 ${SERVICE} 故障转移开始"

# 1. 扩容备用副本
kubectl scale deployment ${SERVICE} -n evcs --replicas=${BACKUP_REPLICAS}

# 2. 等待新副本就绪
kubectl rollout status deployment/${SERVICE} -n evcs --timeout=120s

# 3. 移除故障 Pod
kubectl delete pod -l app=${SERVICE} -n evcs --field-selector=status.phase!=Running

echo "服务 ${SERVICE} 故障转移完成"
```

---

## 4. 数据恢复预案

### 4.1 误删数据恢复

```sql
-- 场景：误删订单数据

-- 1. 立即停止写入
ALTER TABLE charging_order SET (autovacuum_enabled = false);

-- 2. 从备份恢复
-- 参见 BACKUP-RECOVERY-GUIDE.md

-- 3. 使用临时表对比
CREATE TABLE charging_order_backup AS 
SELECT * FROM restore_schema.charging_order 
WHERE id IN (SELECT id FROM charging_order WHERE deleted = true);

-- 4. 恢复数据
UPDATE charging_order SET deleted = false 
WHERE id IN (SELECT id FROM charging_order_backup);

-- 5. 重新启用自动清理
ALTER TABLE charging_order SET (autovacuum_enabled = true);
```

### 4.2 数据不一致修复

```sql
-- 订单与支付单不一致

-- 1. 查找不一致数据
SELECT o.id, o.order_no, o.status, p.status as payment_status
FROM charging_order o
LEFT JOIN payment_order p ON o.order_no = p.order_no
WHERE o.status = 'PAID' AND (p.status IS NULL OR p.status != 'SUCCESS');

-- 2. 记录异常数据
INSERT INTO data_repair_log (table_name, record_id, issue, repair_sql, created_at)
SELECT 'charging_order', id, 'payment_mismatch', 
       'UPDATE charging_order SET status = ... WHERE id = ' || id,
       now()
FROM charging_order
WHERE ...;

-- 3. 修复数据（需人工确认）
-- UPDATE charging_order SET status = 'PENDING_PAYMENT' WHERE id = ?;
```

---

## 5. 应急响应流程

### 5.1 L1 紧急响应

```
┌─────────────────────────────────────────────────────────────┐
│                    L1 紧急响应流程                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  发现问题 ──▶ 初步评估 ──▶ 升级通知 ──▶ 应急响应           │
│     │           │           │           │                   │
│     │           │           │           ▼                   │
│     │           │           │      ┌─────────┐              │
│     │           │           │      │启动应急组│              │
│     │           │           │      └────┬────┘              │
│     │           │           │           │                   │
│     ▼           ▼           ▼           ▼                   │
│  5分钟内     10分钟内    15分钟内   持续处理               │
│                                                             │
│  并行动作：                                                 │
│  1. 发布故障公告                                            │
│  2. 启动降级措施                                            │
│  3. 定位根因                                                │
│  4. 准备回滚方案                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 应急响应检查表

```markdown
## L1 紧急响应检查表

### 第一阶段 (0-5分钟)
- [ ] 确认故障范围和影响
- [ ] 通知应急总指挥
- [ ] 启动应急群组
- [ ] 发布初步公告

### 第二阶段 (5-15分钟)
- [ ] 收集故障日志和监控数据
- [ ] 初步定位问题
- [ ] 启动降级措施
- [ ] 准备回滚方案

### 第三阶段 (15-30分钟)
- [ ] 执行修复或回滚
- [ ] 验证服务恢复
- [ ] 更新用户公告
- [ ] 持续监控

### 收尾阶段
- [ ] 确认服务完全恢复
- [ ] 撰写故障报告
- [ ] 安排复盘会议
- [ ] 制定改进措施
```

---

## 6. 回滚预案

### 6.1 应用回滚

```bash
#!/bin/bash
# 应用回滚脚本

SERVICE=$1
REVISION=${2:-0}  # 0 表示回滚到上一版本

if [ -z "$SERVICE" ]; then
    echo "Usage: $0 <service-name> [revision]"
    exit 1
fi

echo "=== 开始回滚 ${SERVICE} ==="

if [ "$REVISION" -eq 0 ]; then
    # 回滚到上一版本
    kubectl rollout undo deployment/${SERVICE} -n evcs
else
    # 回滚到指定版本
    kubectl rollout undo deployment/${SERVICE} -n evcs --to-revision=${REVISION}
fi

# 等待回滚完成
kubectl rollout status deployment/${SERVICE} -n evcs --timeout=180s

# 验证
kubectl get pods -l app=${SERVICE} -n evcs

echo "=== 回滚完成 ==="
```

### 6.2 数据库回滚

```sql
-- DDL 回滚（需提前准备回滚脚本）

-- 例：回滚添加列操作
ALTER TABLE charging_order DROP COLUMN IF EXISTS new_column;

-- 例：回滚索引创建
DROP INDEX IF EXISTS idx_new_index;

-- 例：回滚表结构修改
ALTER TABLE charging_order ALTER COLUMN amount TYPE DECIMAL(10,2);
```

### 6.3 配置回滚

```bash
#!/bin/bash
# 配置回滚脚本

CONFIG_NAME=$1
REVISION=${2:-0}

# 查看历史版本
kubectl rollout history configmap/${CONFIG_NAME} -n evcs

# 回滚到指定版本（需要从 Git 恢复）
cd /path/to/config-repo
git checkout HEAD~1 -- ${CONFIG_NAME}.yaml
kubectl apply -f ${CONFIG_NAME}.yaml -n evcs

# 重启依赖服务
kubectl rollout restart deployment -l config=${CONFIG_NAME} -n evcs
```

---

## 7. 限流预案

### 7.1 限流规则

```java
// Sentinel 限流配置
@Configuration
public class RateLimitConfig {

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // API 网关限流
        FlowRule gatewayRule = new FlowRule();
        gatewayRule.setResource("gateway");
        gatewayRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        gatewayRule.setCount(1000); // 1000 QPS
        rules.add(gatewayRule);
        
        // 订单创建限流
        FlowRule orderRule = new FlowRule();
        orderRule.setResource("createOrder");
        orderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        orderRule.setCount(100); // 100 QPS
        rules.add(orderRule);
        
        FlowRuleManager.loadRules(rules);
    }
}
```

### 7.2 手动限流

```bash
# 使用 Nginx 限流
kubectl edit configmap nginx-config -n evcs

# 添加限流配置
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

server {
    location /api/ {
        limit_req zone=api burst=20 nodelay;
        ...
    }
}

# 应用配置
kubectl rollout restart deployment nginx -n evcs
```

---

## 8. 通知模板

### 8.1 故障通知

```markdown
【EVCS 故障通知】

故障等级：L1 紧急
故障时间：2026-01-13 10:30:00
影响范围：全部用户无法正常充电

故障现象：
- 订单服务响应超时
- 充电启动失败

当前状态：正在处理中
预计恢复：30分钟内

应急措施：
- 已启动备用服务
- 技术团队紧急排查

请保持关注，后续进展将及时通知。
```

### 8.2 恢复通知

```markdown
【EVCS 恢复通知】

故障等级：L1 紧急
故障时间：2026-01-13 10:30:00 ~ 11:00:00
影响时长：30分钟

故障原因：数据库连接池耗尽
处理措施：扩容连接池、优化慢查询

当前状态：已完全恢复
后续措施：
- 持续监控 24 小时
- 安排故障复盘

给您带来的不便深表歉意。
```

---

## 9. 演练计划

### 9.1 演练类型

| 类型 | 频率 | 范围 | 时长 |
|------|------|------|------|
| 桌面演练 | 每月 | 运维团队 | 2小时 |
| 技术演练 | 每季度 | 技术团队 | 4小时 |
| 全面演练 | 每年 | 全公司 | 1天 |

### 9.2 演练检查表

```markdown
## 应急演练检查表

### 准备阶段
- [ ] 确定演练场景
- [ ] 通知参与人员
- [ ] 准备演练环境
- [ ] 备份生产数据

### 执行阶段
- [ ] 模拟故障注入
- [ ] 触发告警机制
- [ ] 执行应急流程
- [ ] 记录响应时间

### 验收阶段
- [ ] 验证恢复效果
- [ ] 检查数据一致性
- [ ] 评估响应效率

### 总结阶段
- [ ] 编写演练报告
- [ ] 识别改进点
- [ ] 更新应急预案
```

---

## 10. 相关文档

- [故障排查手册](TROUBLESHOOTING-GUIDE.md)
- [备份恢复操作手册](BACKUP-RECOVERY-GUIDE.md)
- [监控告警配置指南](MONITORING-ALERTING-GUIDE.md)
- [系统架构风险审计报告](../architecture/RISK-AUDIT-REPORT.md)

---

## 11. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本 |
