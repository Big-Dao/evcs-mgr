# EVCS 故障排查手册

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: 运维团队  
> **状态**: 已发布

---

## 1. 概述

本手册提供 EVCS 充电站管理系统常见故障的诊断和处理流程，帮助运维人员快速定位和解决问题。

### 1.1 故障分级

| 级别 | 定义 | 影响范围 | 响应时间 |
|------|------|----------|----------|
| **P0** | 系统瘫痪 | 全部用户无法使用 | 5 分钟 |
| **P1** | 核心功能不可用 | 无法充电/支付 | 15 分钟 |
| **P2** | 功能降级 | 部分功能异常 | 30 分钟 |
| **P3** | 轻微问题 | 不影响核心业务 | 工作时间 |

### 1.2 故障排查通用流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  发现问题   │───▶│  初步诊断   │───▶│  定位根因   │
└─────────────┘    └─────────────┘    └─────────────┘
                                            │
                                            ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  记录总结   │◀───│  验证恢复   │◀───│  实施修复   │
└─────────────┘    └─────────────┘    └─────────────┘
```

---

## 2. 服务不可用

### 2.1 单个服务无法启动

**现象**: 某个微服务无法启动或频繁重启

**排查步骤**:

```bash
# 1. 查看服务状态
kubectl get pods -n evcs | grep <service-name>

# 2. 查看 Pod 详情
kubectl describe pod <pod-name> -n evcs

# 3. 查看启动日志
kubectl logs <pod-name> -n evcs --tail=200

# 4. 查看之前的日志（如果已重启）
kubectl logs <pod-name> -n evcs --previous
```

**常见原因及处理**:

| 原因 | 日志特征 | 处理方法 |
|------|----------|----------|
| 内存不足 | `OOMKilled` | 增加内存限制 |
| 配置错误 | `Failed to load configuration` | 检查 ConfigMap |
| 数据库连接失败 | `Connection refused` | 检查数据库状态 |
| Eureka 注册失败 | `Cannot execute request` | 检查 Eureka 服务 |
| 端口冲突 | `Address already in use` | 检查端口占用 |

### 2.2 多个服务同时不可用

**现象**: 多个服务同时出现问题

**排查步骤**:

```bash
# 1. 检查基础设施
kubectl get pods -n evcs | grep -E "postgres|redis|rabbitmq|eureka|config"

# 2. 检查网络
kubectl exec -it <any-pod> -n evcs -- ping evcs-postgres

# 3. 检查资源使用
kubectl top nodes
kubectl top pods -n evcs
```

**常见原因**:
- 数据库不可用
- Redis 不可用
- RabbitMQ 不可用
- Eureka 注册中心不可用
- 网络问题

---

## 3. 数据库问题

### 3.1 PostgreSQL 连接失败

**现象**: 服务报错 `Connection refused` 或 `too many connections`

**排查步骤**:

```bash
# 1. 检查 PostgreSQL Pod 状态
kubectl get pod -n evcs | grep postgres
kubectl logs <postgres-pod> -n evcs

# 2. 连接数据库检查
kubectl exec -it <postgres-pod> -n evcs -- psql -U postgres -d evcs_mgr

# 3. 查看连接数
SELECT count(*) FROM pg_stat_activity;
SELECT * FROM pg_stat_activity WHERE state = 'active';

# 4. 查看等待的查询
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE state != 'idle' 
ORDER BY duration DESC;
```

**处理方法**:

```sql
-- 终止长时间运行的查询
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE duration > interval '5 minutes' AND state = 'active';

-- 增加最大连接数（需重启）
ALTER SYSTEM SET max_connections = 300;
```

### 3.2 PostgreSQL 性能问题

**现象**: 查询响应慢

**排查步骤**:

```sql
-- 1. 查看慢查询
SELECT query, calls, total_time, mean_time, rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- 2. 查看表膨胀
SELECT schemaname, relname, n_live_tup, n_dead_tup,
       round(n_dead_tup::numeric / (n_live_tup + 1) * 100, 2) as dead_ratio
FROM pg_stat_user_tables
WHERE n_dead_tup > 10000
ORDER BY n_dead_tup DESC;

-- 3. 查看索引使用情况
SELECT schemaname, relname, indexrelname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;

-- 4. 查看锁等待
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

**处理方法**:

```sql
-- 手动 VACUUM
VACUUM ANALYZE <table_name>;

-- 重建索引
REINDEX INDEX CONCURRENTLY <index_name>;

-- 更新统计信息
ANALYZE;
```

---

## 4. Redis 问题

### 4.1 Redis 连接失败

**排查步骤**:

```bash
# 1. 检查 Redis Pod 状态
kubectl get pod -n evcs | grep redis
kubectl logs <redis-pod> -n evcs

# 2. 测试连接
kubectl exec -it <redis-pod> -n evcs -- redis-cli ping

# 3. 查看内存使用
kubectl exec -it <redis-pod> -n evcs -- redis-cli info memory
```

### 4.2 Redis 内存不足

**现象**: 日志显示 `OOM command not allowed`

**排查步骤**:

```bash
# 1. 查看内存使用
redis-cli info memory

# 2. 查看大 Key
redis-cli --bigkeys

# 3. 查看 Key 数量
redis-cli dbsize
```

**处理方法**:

```bash
# 清理过期 Key
redis-cli --scan --pattern "session:*" | xargs redis-cli del

# 增加内存限制（修改 K8s 配置）
kubectl edit deployment redis -n evcs
```

---

## 5. RabbitMQ 问题

### 5.1 消息积压

**现象**: 队列消息持续增长

**排查步骤**:

```bash
# 1. 查看队列状态
rabbitmqctl list_queues name messages consumers

# 2. 查看消费者
rabbitmqctl list_consumers

# 3. 查看连接
rabbitmqctl list_connections
```

**处理方法**:

```bash
# 清空队列（谨慎使用）
rabbitmqctl purge_queue <queue_name>

# 重启消费者服务
kubectl rollout restart deployment <consumer-service> -n evcs
```

### 5.2 消息丢失

**排查步骤**:

1. 检查生产者确认
2. 检查队列持久化配置
3. 检查消费者确认模式
4. 查看死信队列

```bash
# 查看死信队列
rabbitmqctl list_queues name messages | grep -E "dlx|dead"
```

---

## 6. 应用服务问题

### 6.1 JVM 内存问题

**现象**: 服务响应变慢或 OOM

**排查步骤**:

```bash
# 1. 查看 JVM 内存
curl http://<service>:8080/actuator/metrics/jvm.memory.used

# 2. 触发 Heap Dump（谨慎使用）
kubectl exec -it <pod> -n evcs -- jcmd 1 GC.heap_dump /tmp/heap.hprof

# 3. 查看 GC 日志
kubectl logs <pod> -n evcs | grep -i gc
```

**处理方法**:

1. 增加 JVM 堆内存
2. 分析 Heap Dump 定位内存泄漏
3. 检查代码是否有内存泄漏

### 6.2 HTTP 请求超时

**现象**: API 请求超时

**排查步骤**:

```bash
# 1. 查看接口延迟
curl http://<service>:8080/actuator/metrics/http.server.requests

# 2. 查看线程状态
curl http://<service>:8080/actuator/threaddump | grep -A 20 "BLOCKED"

# 3. 查看连接池状态
curl http://<service>:8080/actuator/metrics/hikaricp.connections.active
```

**常见原因**:
- 数据库查询慢
- 外部服务调用超时
- 线程池耗尽
- 连接池耗尽

### 6.3 服务调用失败

**现象**: 服务间调用失败

**排查步骤**:

```bash
# 1. 检查 Eureka 注册状态
curl http://eureka:8761/eureka/apps

# 2. 检查网络连通性
kubectl exec -it <pod> -n evcs -- curl http://<target-service>:8080/actuator/health

# 3. 查看 Feign 调用日志
kubectl logs <pod> -n evcs | grep -i feign
```

---

## 7. 业务问题

### 7.1 订单创建失败

**排查步骤**:

```bash
# 1. 查看 Order Service 日志
kubectl logs -l app=order-service -n evcs --tail=500 | grep -i error

# 2. 检查数据库订单表
SELECT * FROM charging_order WHERE create_time > now() - interval '1 hour' ORDER BY create_time DESC LIMIT 10;

# 3. 检查 RabbitMQ 消息
rabbitmqctl list_queues name messages | grep order
```

**常见原因**:
- 充电桩状态异常
- 用户余额不足
- 计费方案未配置
- 数据库写入失败

### 7.2 支付回调失败

**排查步骤**:

```bash
# 1. 查看 Payment Service 日志
kubectl logs -l app=payment-service -n evcs --tail=500 | grep -i callback

# 2. 检查支付订单状态
SELECT order_no, status, callback_time, callback_count FROM payment_order WHERE create_time > now() - interval '1 hour';

# 3. 检查死信队列
rabbitmqctl list_queues | grep -E "payment.*dlx"
```

**处理方法**:

1. 手动触发回调重试
2. 检查签名验证配置
3. 联系支付渠道确认状态

### 7.3 充电桩离线

**排查步骤**:

```bash
# 1. 查看 Protocol Service 日志
kubectl logs -l app=protocol-service -n evcs --tail=500 | grep <charger_code>

# 2. 检查心跳记录
SELECT charger_code, last_heartbeat, status FROM charger WHERE station_id = <station_id>;

# 3. 检查网络连接
kubectl logs -l app=protocol-service -n evcs | grep -i "connection\|disconnect"
```

**常见原因**:
- 充电桩网络故障
- 充电桩重启
- 协议版本不兼容
- 服务器负载过高

---

## 8. 日志分析

### 8.1 日志查看命令

```bash
# 实时查看日志
kubectl logs -f <pod-name> -n evcs

# 查看多个 Pod 日志
kubectl logs -l app=<service-name> -n evcs --tail=100

# 按时间范围查看
kubectl logs <pod-name> -n evcs --since=1h

# 导出日志
kubectl logs <pod-name> -n evcs > /tmp/service.log
```

### 8.2 日志关键字

| 关键字 | 含义 |
|--------|------|
| `ERROR` | 错误日志 |
| `Exception` | 异常堆栈 |
| `timeout` | 超时 |
| `refused` | 连接拒绝 |
| `OOM` | 内存溢出 |
| `deadlock` | 死锁 |
| `slow query` | 慢查询 |

### 8.3 日志分析示例

```bash
# 统计错误数量
kubectl logs <pod> -n evcs --since=1h | grep -c ERROR

# 查看最近的异常
kubectl logs <pod> -n evcs --since=1h | grep -A 10 Exception | head -50

# 统计接口调用次数
kubectl logs <pod> -n evcs --since=1h | grep "GET\|POST" | awk '{print $6}' | sort | uniq -c | sort -rn
```

---

## 9. 紧急恢复操作

### 9.1 服务快速重启

```bash
# 重启单个服务
kubectl rollout restart deployment <service-name> -n evcs

# 重启所有服务
kubectl rollout restart deployment -n evcs
```

### 9.2 数据库紧急操作

```sql
-- 终止所有活跃连接
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'evcs_mgr' AND pid <> pg_backend_pid();

-- 禁用慢查询
ALTER SYSTEM SET statement_timeout = '10s';
SELECT pg_reload_conf();
```

### 9.3 服务降级

```bash
# 缩减副本数
kubectl scale deployment <service-name> -n evcs --replicas=1

# 临时禁用非核心服务
kubectl scale deployment monitoring-service -n evcs --replicas=0
```

---

## 10. 故障报告模板

```markdown
## 故障报告

**故障编号**: EVCS-2026-0113-001
**故障时间**: 2026-01-13 10:30 ~ 11:00
**故障级别**: P1
**影响范围**: 订单服务不可用

### 故障现象
- 用户无法创建订单
- Order Service 频繁重启

### 根因分析
- 数据库连接池耗尽
- 慢查询导致连接积压

### 处理过程
1. 10:35 发现告警
2. 10:40 定位到数据库连接问题
3. 10:45 终止慢查询
4. 10:50 重启 Order Service
5. 11:00 服务恢复

### 改进措施
- [ ] 增加连接池大小
- [ ] 优化慢查询
- [ ] 添加连接池告警
```

---

## 11. 相关文档

- [监控告警配置指南](MONITORING-ALERTING-GUIDE.md)
- [备份恢复操作手册](BACKUP-RECOVERY-GUIDE.md)
- [系统架构风险审计报告](../architecture/RISK-AUDIT-REPORT.md)

---

## 12. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本 |
