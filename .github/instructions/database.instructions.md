---
applyTo: "**/*.sql"
priority: high
---

# 数据库变更规范 (Database Migration)

> **最后更新**: 2025-12-18 | **维护者**: DBA/架构团队 | **状态**: 已发布

本规范适用于所有 Flyway 迁移脚本和 SQL 变更。

## 🚨 关键要求

### 1. 命名规范
**严格遵循 Flyway 命名格式**
- **版本迁移**: `V{Version}__{Description}.sql`
  - 版本号使用点分格式：`V1.0.1`
  - 描述使用下划线分隔：`__Create_station_table`
  - 示例：`V1.0.1__Create_station_table.sql`
- **可重复迁移**: `R__{Description}.sql` (用于视图、存储过程)

### 2. 脚本内容
**DDL 与 DML 分离**
- 尽量将表结构变更 (DDL) 与数据变更 (DML) 分在不同的脚本中。
- **禁止**在迁移脚本中包含测试数据或开发环境特定的数据。
- 所有表必须包含标准审计字段：`created_at`, `updated_at`, `created_by`, `updated_by`。

### 3. 安全与性能
**变更审查**
- **禁止**使用 `DROP TABLE` 或 `DROP COLUMN`（除非是专门的清理脚本且经过审批）。
- 为所有外键列和频繁查询列创建索引。
- 大表变更（>100万行）必须先在 Staging 环境测试执行时间。

---

## ✅ 示例

### 标准建表脚本

```sql
CREATE TABLE charging_station (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_station_tenant ON charging_station(tenant_id);
COMMENT ON TABLE charging_station IS '充电站基础信息表';
```
