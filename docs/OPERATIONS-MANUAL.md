# EVCS Manager 运维手册

## 概述

本文档提供 EVCS Manager 系统的运维指南，包括监控、日志管理、故障排查、备份恢复和性能优化等内容。

**版本**: v1.0.0  
**更新日期**: 2025-10-12

---

## 目录

- [系统监控](#系统监控)
- [日志管理](#日志管理)
- [故障排查](#故障排查)
- [备份与恢复](#备份与恢复)
- [性能优化](#性能优化)
- [安全运维](#安全运维)
- [应急响应](#应急响应)

---

## 系统监控

### Prometheus监控指标

#### 访问监控端点

```bash
# 查看所有可用指标
curl http://localhost:8080/actuator/prometheus
```

#### 核心指标说明

| 指标名称 | 说明 | 告警阈值 |
|---------|------|---------|
| `jvm_memory_used_bytes` | JVM内存使用量 | > 80% |
| `http_server_requests_seconds` | HTTP请求响应时间 | P99 > 1s |
| `system_cpu_usage` | CPU使用率 | > 80% |
| `jdbc_connections_active` | 活跃数据库连接数 | > 80% pool size |
| `evcs_order_created_total` | 订单创建总数 | 监控趋势 |
| `evcs_charger_online_rate` | 充电桩在线率 | < 95% |

### Grafana Dashboard

#### 1. 系统总览Dashboard

访问: `http://localhost:3000/d/system-overview`

**关键面板**:
- 服务健康状态
- QPS (每秒请求数)
- 响应时间 (P50/P95/P99)
- 错误率
- JVM内存使用
- CPU使用率

#### 2. 业务监控Dashboard

访问: `http://localhost:3000/d/business-monitoring`

**关键面板**:
- 订单创建趋势
- 订单成功率
- 充电桩在线率
- 充电桩状态分布
- 营收统计
- 实时充电桩数

### 告警规则

#### 配置告警

告警规则位于 `monitoring/grafana/alerts/alert-rules.yml`:

```yaml
groups:
  - name: system_alerts
    interval: 1m
    rules:
      # 服务宕机告警
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务 {{ $labels.job }} 宕机"
          description: "服务已宕机超过1分钟"

      # 高错误率告警
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "高错误率告警"
          description: "5xx错误率超过5%"

      # 高响应时间告警
      - alert: HighResponseTime
        expr: histogram_quantile(0.99, http_server_requests_seconds_bucket) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "响应时间过长"
          description: "P99响应时间超过1秒"
```

#### 告警通知渠道

配置在 `monitoring/grafana/provisioning/alerting/contact-points.yml`:

```yaml
apiVersion: 1
contactPoints:
  - name: Email
    receivers:
      - uid: email-1
        type: email
        settings:
          addresses: ops@evcs-mgr.com
          
  - name: Slack
    receivers:
      - uid: slack-1
        type: slack
        settings:
          url: https://hooks.slack.com/services/xxx
          
  - name: PagerDuty
    receivers:
      - uid: pagerduty-1
        type: pagerduty
        settings:
          integrationKey: your-key
```

---

## 日志管理

### 日志架构

```
应用服务 → Logstash → Elasticsearch → Kibana
```

### 日志级别

| 级别 | 使用场景 | 生产环境 |
|------|---------|---------|
| ERROR | 严重错误，需要立即处理 | ✅ |
| WARN | 警告信息，需要关注 | ✅ |
| INFO | 重要业务流程 | ✅ |
| DEBUG | 调试信息 | ❌ |
| TRACE | 详细跟踪信息 | ❌ |

### 日志查询

#### 1. Kibana查询

访问: `http://localhost:5601`

**常用查询语法**:
```
# 查询特定服务的日志
service:"evcs-gateway"

# 查询错误日志
level:"ERROR"

# 查询特定租户的日志
tenantId:1

# 查询包含关键词的日志
message:*充电桩*

# 组合查询
service:"evcs-order" AND level:"ERROR" AND tenantId:1

# 时间范围查询
@timestamp:[now-1h TO now]
```

#### 2. 命令行查询

```bash
# 查看实时日志
docker-compose logs -f evcs-gateway

# 查看最近100行日志
docker-compose logs --tail=100 evcs-gateway

# 查看特定时间段的日志
docker-compose logs --since="2025-10-12T10:00:00" --until="2025-10-12T11:00:00" evcs-gateway
```

### 日志归档

#### 配置日志滚动

`logback-spring.xml`:

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/logs/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <!-- 每天滚动一次 -->
        <fileNamePattern>/logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
        <!-- 保留30天 -->
        <maxHistory>30</maxHistory>
        <!-- 总大小不超过10GB -->
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

#### 清理旧日志

```bash
# 手动清理30天前的日志
find /logs -name "*.log" -mtime +30 -delete

# 配置定时任务（每天凌晨2点执行）
0 2 * * * find /logs -name "*.log" -mtime +30 -delete
```

---

## 故障排查

### 常见故障场景

#### 1. 服务无响应

**症状**: HTTP请求超时，无返回结果

**排查步骤**:

```bash
# 1. 检查服务进程
ps aux | grep java

# 2. 检查端口监听
netstat -nltp | grep 8080

# 3. 检查CPU和内存
top
free -h

# 4. 查看最近的错误日志
docker-compose logs --tail=100 evcs-gateway | grep ERROR

# 5. 检查数据库连接
docker-compose exec postgres pg_isready

# 6. 检查线程dump
jstack <pid> > thread-dump.txt
```

**常见原因**:
- 数据库连接池耗尽
- 死锁
- 内存溢出
- 网络问题

#### 2. 数据库连接失败

**症状**: `Connection refused` 或 `Too many connections`

**排查步骤**:

```bash
# 1. 检查数据库是否运行
docker-compose ps postgres

# 2. 查看当前连接数
docker-compose exec postgres psql -U evcs_user -d evcs_db -c "SELECT count(*) FROM pg_stat_activity;"

# 3. 查看最大连接数
docker-compose exec postgres psql -U evcs_user -d evcs_db -c "SHOW max_connections;"

# 4. 查看慢查询
docker-compose exec postgres psql -U evcs_user -d evcs_db -c "SELECT pid, now() - query_start as duration, query FROM pg_stat_activity WHERE state = 'active' ORDER BY duration DESC LIMIT 10;"

# 5. 终止长时间运行的查询
docker-compose exec postgres psql -U evcs_user -d evcs_db -c "SELECT pg_terminate_backend(<pid>);"
```

**解决方案**:
- 增加max_connections配置
- 优化慢查询
- 调整连接池配置
- 检查连接泄漏

#### 3. 内存溢出 (OOM)

**症状**: 服务突然停止，日志显示 `OutOfMemoryError`

**排查步骤**:

```bash
# 1. 查看heap dump（如果配置了自动dump）
ls -lh /logs/*.hprof

# 2. 分析heap dump
# 使用MAT (Memory Analyzer Tool)
# https://www.eclipse.org/mat/

# 3. 查看GC日志
# 在JVM参数中添加: -Xloggc:/logs/gc.log -XX:+PrintGCDetails

# 4. 查看内存使用趋势
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**解决方案**:
- 增加堆内存: `-Xmx2g`
- 分析内存泄漏
- 优化对象创建
- 使用对象池

#### 4. 充电桩离线

**症状**: 大量充电桩显示离线状态

**排查步骤**:

```bash
# 1. 检查协议服务
curl http://localhost:8085/actuator/health

# 2. 查看协议服务日志
docker-compose logs --tail=100 evcs-protocol

# 3. 检查网络连通性
ping <charger-ip>
telnet <charger-ip> <port>

# 4. 查看最近的心跳记录
# 在数据库中查询
SELECT charger_id, last_heartbeat FROM chargers 
WHERE last_heartbeat < NOW() - INTERVAL '5 minutes'
ORDER BY last_heartbeat DESC LIMIT 20;

# 5. 检查RabbitMQ消息积压
docker-compose exec rabbitmq rabbitmqctl list_queues
```

**解决方案**:
- 检查网络连接
- 重启协议服务
- 检查充电桩硬件状态
- 清理消息积压

### 故障诊断工具

#### 1. JVM诊断工具

```bash
# 查看JVM进程
jps -l

# 查看JVM参数
jinfo <pid>

# 查看堆内存分配
jmap -heap <pid>

# 导出heap dump
jmap -dump:format=b,file=heap.hprof <pid>

# 查看线程栈
jstack <pid>

# 实时监控
jstat -gc <pid> 1000
```

#### 2. 性能分析工具

```bash
# CPU性能分析
perf top

# 网络连接分析
ss -s
netstat -ant | awk '{print $6}' | sort | uniq -c | sort -n

# 磁盘IO分析
iostat -x 1

# 查看系统资源
vmstat 1
```

---

## 备份与恢复

### 数据库备份

#### 1. 定时备份脚本

创建 `/opt/scripts/backup-db.sh`:

```bash
#!/bin/bash

# 配置
BACKUP_DIR="/backup/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/evcs_db_$DATE.sql"
DB_USER="evcs_user"
DB_NAME="evcs_db"
RETENTION_DAYS=30

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
docker-compose exec -T postgres pg_dump -U $DB_USER $DB_NAME > $BACKUP_FILE

# 压缩备份文件
gzip $BACKUP_FILE

# 删除旧备份（保留30天）
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

# 记录日志
echo "$(date): Database backup completed: $BACKUP_FILE.gz" >> /var/log/backup.log
```

设置定时任务：

```bash
# 每天凌晨3点备份
0 3 * * * /opt/scripts/backup-db.sh
```

#### 2. 恢复数据库

```bash
# 解压备份文件
gunzip evcs_db_20251012_030000.sql.gz

# 恢复数据库
docker-compose exec -T postgres psql -U evcs_user -d evcs_db < evcs_db_20251012_030000.sql
```

### 配置文件备份

```bash
# 备份所有配置文件
tar -czf config-backup-$(date +%Y%m%d).tar.gz \
  docker-compose.yml \
  .env \
  */src/main/resources/application*.yml

# 上传到远程存储（示例：S3）
aws s3 cp config-backup-*.tar.gz s3://evcs-backups/config/
```

---

## 性能优化

### 数据库优化

#### 1. 创建索引

```sql
-- 为常用查询字段创建索引
CREATE INDEX idx_stations_tenant_id ON stations(tenant_id);
CREATE INDEX idx_stations_status ON stations(status);
CREATE INDEX idx_chargers_station_id ON chargers(station_id);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_create_time ON orders(create_time DESC);

-- 复合索引
CREATE INDEX idx_orders_tenant_status ON orders(tenant_id, status);
```

#### 2. 查询优化

```sql
-- 使用EXPLAIN分析查询
EXPLAIN ANALYZE 
SELECT * FROM stations 
WHERE tenant_id = 1 AND status = 1;

-- 定期更新统计信息
VACUUM ANALYZE stations;
```

#### 3. 连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 应用优化

#### 1. 缓存配置

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
```

#### 2. 线程池配置

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    accept-count: 100
    max-connections: 8192
```

### 监控性能指标

```bash
# 查看QPS
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements[] | select(.statistic=="COUNT")'

# 查看响应时间
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements[] | select(.statistic=="TOTAL_TIME")'

# 查看数据库连接池使用情况
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

---

## 安全运维

### 1. 访问控制

```bash
# 使用防火墙限制访问
ufw allow from 10.0.0.0/8 to any port 8080
ufw deny 8080

# 使用fail2ban防止暴力破解
apt-get install fail2ban
```

### 2. 密钥轮换

```bash
# 定期更换JWT密钥
# 1. 生成新密钥
openssl rand -base64 32

# 2. 更新配置
# 3. 滚动重启服务
```

### 3. 安全审计

```bash
# 查看登录记录
grep "login" /logs/application.log | tail -100

# 查看失败的认证尝试
grep "authentication failed" /logs/application.log

# 检查异常的API调用
grep "403\|401" /logs/application.log | wc -l
```

---

## 应急响应

### 紧急情况处理流程

#### 1. 服务宕机

```bash
# 1. 快速重启服务
docker-compose restart evcs-gateway

# 2. 如果无效，重新部署
docker-compose down evcs-gateway
docker-compose up -d evcs-gateway

# 3. 切换到备用节点（如果有）
# 在负载均衡器中移除故障节点
```

#### 2. 数据库故障

```bash
# 1. 切换到只读模式
# 修改配置，禁止写操作

# 2. 从备库提升为主库
# 根据数据库复制方案执行

# 3. 恢复服务
docker-compose restart evcs-gateway
```

#### 3. 安全事件

```bash
# 1. 立即阻止可疑IP
ufw deny from <suspicious-ip>

# 2. 撤销受影响的Token
# 在数据库中标记Token为无效

# 3. 通知用户修改密码
# 发送邮件通知

# 4. 收集日志证据
tar -czf security-incident-$(date +%Y%m%d).tar.gz /logs/
```

### 值班联系方式

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 一线值班 | 张三 | 138-xxxx-xxxx | zhangsan@evcs-mgr.com |
| 二线值班 | 李四 | 139-xxxx-xxxx | lisi@evcs-mgr.com |
| DBA | 王五 | 136-xxxx-xxxx | wangwu@evcs-mgr.com |
| 安全负责人 | 赵六 | 137-xxxx-xxxx | zhaoliu@evcs-mgr.com |

---

## 更新日志

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0.0 | 2025-10-12 | 初始版本 |

---

## 支持与联系

**运维支持**: ops@evcs-mgr.com  
**紧急联系**: +86-400-xxx-xxxx (24/7)  
**技术支持**: tech-support@evcs-mgr.com
