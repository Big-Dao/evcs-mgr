# EVCS 备份恢复操作手册

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: DBA/运维团队  
> **状态**: 已发布

---

## 1. 概述

本手册定义 EVCS 充电站管理系统的数据备份策略和灾难恢复流程。

### 1.1 备份目标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **RPO** (Recovery Point Objective) | < 5 分钟 | 最大可接受数据丢失量 |
| **RTO** (Recovery Time Objective) | < 30 分钟 | 最大可接受恢复时间 |
| **备份保留期** | 30 天 | 全量备份保留时间 |
| **归档保留期** | 7 年 | 财务数据归档时间 |

### 1.2 备份范围

| 数据类型 | 备份方式 | 频率 | 保留期 |
|----------|----------|------|--------|
| PostgreSQL 数据库 | 物理 + WAL | 每日 + 持续 | 30 天 |
| Redis 数据 | RDB + AOF | 每小时 | 7 天 |
| 配置文件 | Git 版本控制 | 每次变更 | 永久 |
| 应用日志 | 归档压缩 | 每日 | 30 天 |
| 充电曲线数据 | 对象存储 | 每日 | 7 年 |

---

## 2. PostgreSQL 备份

### 2.1 备份策略

```
┌─────────────────────────────────────────────────────────────┐
│                    PostgreSQL 备份架构                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐    ┌──────────┐    ┌──────────────────────┐  │
│  │  主库    │───▶│ WAL归档  │───▶│  对象存储 (S3/MinIO) │  │
│  │ Primary  │    │ 持续归档  │    │  保留 7 天           │  │
│  └──────────┘    └──────────┘    └──────────────────────┘  │
│       │                                                     │
│       │ 每日凌晨 2:00                                       │
│       ▼                                                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  pg_basebackup 物理全量备份                           │  │
│  │  保留 30 天                                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 WAL 归档配置

```ini
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'test ! -f /archive/%f && cp %p /archive/%f'
archive_timeout = 60

# 或使用对象存储
archive_command = 'aws s3 cp %p s3://evcs-backup/wal/%f --endpoint-url http://minio:9000'
```

### 2.3 物理备份脚本

```bash
#!/bin/bash
# /opt/scripts/pg_backup.sh

set -e

# 配置
BACKUP_DIR="/backup/pg"
S3_BUCKET="s3://evcs-backup/pg"
RETENTION_DAYS=30
DATE=$(date +%Y-%m-%d)
BACKUP_PATH="${BACKUP_DIR}/${DATE}"

# 创建备份目录
mkdir -p "${BACKUP_PATH}"

# 执行物理备份
echo "Starting pg_basebackup at $(date)"
pg_basebackup -h localhost -U postgres -D "${BACKUP_PATH}" \
    -Ft -z -P \
    --checkpoint=fast \
    --wal-method=stream

# 上传到对象存储
echo "Uploading to S3..."
aws s3 sync "${BACKUP_PATH}" "${S3_BUCKET}/${DATE}/" \
    --endpoint-url http://minio:9000

# 清理旧备份
echo "Cleaning old backups..."
find "${BACKUP_DIR}" -type d -mtime +${RETENTION_DAYS} -exec rm -rf {} \;

# 记录备份信息
echo "Backup completed at $(date)" >> "${BACKUP_DIR}/backup.log"
du -sh "${BACKUP_PATH}" >> "${BACKUP_DIR}/backup.log"
```

### 2.4 逻辑备份（关键表）

```bash
#!/bin/bash
# /opt/scripts/pg_dump_critical.sh

set -e

BACKUP_DIR="/backup/pg/logical"
DATE=$(date +%Y-%m-%d_%H%M)

mkdir -p "${BACKUP_DIR}"

# 备份关键表
CRITICAL_TABLES="sys_tenant sys_user charging_order payment_order"

for table in ${CRITICAL_TABLES}; do
    echo "Dumping ${table}..."
    pg_dump -h localhost -U postgres -d evcs_mgr \
        -t ${table} \
        --format=custom \
        --file="${BACKUP_DIR}/${table}_${DATE}.dump"
done

# 压缩
tar -czf "${BACKUP_DIR}/critical_${DATE}.tar.gz" -C "${BACKUP_DIR}" *.dump
rm -f "${BACKUP_DIR}"/*.dump

echo "Critical tables backup completed"
```

### 2.5 定时任务配置

```cron
# /etc/cron.d/evcs-backup

# PostgreSQL 物理备份 - 每日凌晨 2:00
0 2 * * * postgres /opt/scripts/pg_backup.sh >> /var/log/pg_backup.log 2>&1

# 关键表逻辑备份 - 每 6 小时
0 */6 * * * postgres /opt/scripts/pg_dump_critical.sh >> /var/log/pg_dump.log 2>&1

# WAL 清理 - 每日凌晨 3:00
0 3 * * * postgres find /archive -mtime +7 -delete

# 备份验证 - 每周日凌晨 4:00
0 4 * * 0 postgres /opt/scripts/verify_backup.sh >> /var/log/backup_verify.log 2>&1
```

---

## 3. 数据恢复

### 3.1 恢复类型选择

| 场景 | 恢复类型 | 预计时间 |
|------|----------|----------|
| 误删少量数据 | 逻辑恢复（单表） | 5-10 分钟 |
| 数据库损坏 | 物理恢复 | 10-30 分钟 |
| 需要回滚到特定时间点 | PITR 恢复 | 15-45 分钟 |
| 完全灾难恢复 | 全量恢复 | 30-60 分钟 |

### 3.2 PITR 恢复流程

**场景**: 误操作删除数据，需要恢复到特定时间点

```bash
#!/bin/bash
# PITR 恢复脚本

# 1. 停止当前数据库
sudo systemctl stop postgresql

# 2. 备份当前数据（保险起见）
mv /var/lib/postgresql/17/main /var/lib/postgresql/17/main.broken

# 3. 恢复最近的全量备份
LATEST_BACKUP=$(ls -t /backup/pg | head -1)
tar -xzf "/backup/pg/${LATEST_BACKUP}/base.tar.gz" -C /var/lib/postgresql/17/main

# 4. 配置恢复参数
cat > /var/lib/postgresql/17/main/recovery.signal << EOF
EOF

cat > /var/lib/postgresql/17/main/postgresql.auto.conf << EOF
restore_command = 'cp /archive/%f %p'
recovery_target_time = '2026-01-13 10:30:00'
recovery_target_action = 'promote'
EOF

# 5. 修复权限
chown -R postgres:postgres /var/lib/postgresql/17/main

# 6. 启动数据库
sudo systemctl start postgresql

# 7. 等待恢复完成
while ! pg_isready; do
    echo "Waiting for recovery..."
    sleep 5
done

echo "Recovery completed"
```

### 3.3 单表恢复

```bash
# 从逻辑备份恢复单表

# 1. 找到备份文件
BACKUP_FILE="/backup/pg/logical/charging_order_2026-01-13_0600.dump"

# 2. 恢复到临时表
pg_restore -h localhost -U postgres -d evcs_mgr \
    --table=charging_order \
    --data-only \
    --disable-triggers \
    "${BACKUP_FILE}"

# 或者恢复到新表进行比对
pg_restore -h localhost -U postgres -d evcs_mgr \
    --table=charging_order \
    --create \
    --schema=restore_temp \
    "${BACKUP_FILE}"
```

### 3.4 全量恢复

```bash
#!/bin/bash
# 全量恢复脚本

set -e

BACKUP_DATE=$1  # 格式: 2026-01-13

if [ -z "$BACKUP_DATE" ]; then
    echo "Usage: $0 <backup-date>"
    exit 1
fi

# 1. 停止所有服务
kubectl scale deployment --all -n evcs --replicas=0

# 2. 停止数据库
kubectl exec -it evcs-postgres -n evcs -- pg_ctl stop -D /var/lib/postgresql/data

# 3. 清空数据目录
kubectl exec -it evcs-postgres -n evcs -- rm -rf /var/lib/postgresql/data/*

# 4. 下载备份
kubectl exec -it evcs-postgres -n evcs -- \
    aws s3 sync s3://evcs-backup/pg/${BACKUP_DATE}/ /backup/ \
    --endpoint-url http://minio:9000

# 5. 解压恢复
kubectl exec -it evcs-postgres -n evcs -- \
    tar -xzf /backup/base.tar.gz -C /var/lib/postgresql/data

# 6. 启动数据库
kubectl exec -it evcs-postgres -n evcs -- pg_ctl start -D /var/lib/postgresql/data

# 7. 验证恢复
kubectl exec -it evcs-postgres -n evcs -- psql -U postgres -c "SELECT count(*) FROM sys_tenant"

# 8. 重启服务
kubectl scale deployment --all -n evcs --replicas=1

echo "Full recovery completed"
```

---

## 4. Redis 备份

### 4.1 备份配置

```conf
# redis.conf
save 900 1
save 300 10
save 60 10000

appendonly yes
appendfsync everysec
```

### 4.2 手动备份

```bash
# 触发 RDB 快照
redis-cli BGSAVE

# 等待完成
while [ $(redis-cli LASTSAVE) == $(cat /tmp/lastsave) ]; do
    sleep 1
done

# 复制备份文件
cp /data/dump.rdb /backup/redis/dump_$(date +%Y%m%d%H%M).rdb
```

### 4.3 恢复流程

```bash
# 1. 停止 Redis
redis-cli SHUTDOWN NOSAVE

# 2. 恢复 RDB 文件
cp /backup/redis/dump_20260113.rdb /data/dump.rdb

# 3. 启动 Redis
redis-server /etc/redis/redis.conf
```

---

## 5. 备份验证

### 5.1 自动验证脚本

```bash
#!/bin/bash
# /opt/scripts/verify_backup.sh

set -e

VERIFY_DIR="/tmp/backup_verify"
LATEST_BACKUP=$(ls -t /backup/pg | head -1)

echo "Verifying backup: ${LATEST_BACKUP}"

# 1. 创建验证目录
rm -rf "${VERIFY_DIR}"
mkdir -p "${VERIFY_DIR}/data"

# 2. 解压备份
tar -xzf "/backup/pg/${LATEST_BACKUP}/base.tar.gz" -C "${VERIFY_DIR}/data"

# 3. 启动验证实例
docker run -d --name pg_verify \
    -v ${VERIFY_DIR}/data:/var/lib/postgresql/data \
    -p 5433:5432 \
    postgres:17-alpine

sleep 30

# 4. 验证数据
TENANT_COUNT=$(docker exec pg_verify psql -U postgres -d evcs_mgr -t -c "SELECT count(*) FROM sys_tenant")
ORDER_COUNT=$(docker exec pg_verify psql -U postgres -d evcs_mgr -t -c "SELECT count(*) FROM charging_order")

echo "Tenant count: ${TENANT_COUNT}"
echo "Order count: ${ORDER_COUNT}"

if [ ${TENANT_COUNT} -gt 0 ]; then
    echo "Backup verification PASSED"
else
    echo "Backup verification FAILED"
    exit 1
fi

# 5. 清理
docker stop pg_verify
docker rm pg_verify
rm -rf "${VERIFY_DIR}"
```

### 5.2 验证清单

| 验证项 | 频率 | 方法 |
|--------|------|------|
| 备份文件完整性 | 每日 | 校验和验证 |
| 数据可恢复性 | 每周 | 恢复到测试环境 |
| 恢复时间测试 | 每月 | 模拟恢复演练 |
| 跨区域恢复 | 每季度 | 异地恢复测试 |

---

## 6. 灾难恢复演练

### 6.1 演练计划

| 演练类型 | 频率 | 参与人员 |
|----------|------|----------|
| 桌面演练 | 每月 | 运维团队 |
| 技术演练 | 每季度 | 运维 + 开发 |
| 全面演练 | 每年 | 全公司 |

### 6.2 演练步骤

```markdown
## 灾难恢复演练检查表

### 准备阶段
- [ ] 通知相关人员
- [ ] 准备测试环境
- [ ] 确认备份文件可用

### 执行阶段
- [ ] 模拟数据库故障
- [ ] 执行恢复流程
- [ ] 记录恢复时间
- [ ] 验证数据完整性

### 验收阶段
- [ ] 核心功能测试
- [ ] 数据一致性校验
- [ ] 业务流程验证

### 总结阶段
- [ ] 记录问题和改进点
- [ ] 更新恢复文档
- [ ] 培训相关人员
```

---

## 7. 监控告警

### 7.1 备份监控

```yaml
# Prometheus 告警规则
groups:
  - name: backup
    rules:
      - alert: BackupNotCompleted
        expr: time() - backup_last_success_timestamp > 86400 * 1.5
        for: 1h
        labels:
          severity: critical
        annotations:
          summary: "备份超过 36 小时未成功"

      - alert: BackupSizeDrop
        expr: backup_size_bytes < backup_size_bytes offset 1d * 0.5
        for: 1h
        labels:
          severity: warning
        annotations:
          summary: "备份大小异常下降"
```

### 7.2 备份状态上报

```bash
# 备份完成后上报指标
curl -X POST http://pushgateway:9091/metrics/job/pg_backup \
    --data-binary @- << EOF
backup_last_success_timestamp $(date +%s)
backup_size_bytes $(du -sb /backup/pg/$(date +%Y-%m-%d) | cut -f1)
backup_duration_seconds ${DURATION}
EOF
```

---

## 8. 应急联系人

| 角色 | 姓名 | 电话 | 职责 |
|------|------|------|------|
| DBA 主管 | - | - | 数据库恢复决策 |
| 运维主管 | - | - | 整体协调 |
| 开发主管 | - | - | 业务验证 |
| 安全主管 | - | - | 安全审计 |

---

## 9. 相关文档

- [监控告警配置指南](MONITORING-ALERTING-GUIDE.md)
- [故障排查手册](TROUBLESHOOTING-GUIDE.md)
- [海量数据处理方案 RFC](../architecture/DATA-PARTITIONING-RFC.md)

---

## 10. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本 |
