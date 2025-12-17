# EVCS 数据库设计规范

> **版本**: v1.2 | **最后更新**: 2025-12-17 | **维护者**: 数据架构/后端团队 | **状态**: 活跃
>
> 本文档定义 EVCS Manager 的数据库设计基线：命名、字段、索引、多租户与迁移方式。

## 🎯 概述

EVCS 采用 PostgreSQL + MyBatis Plus（多租户）为核心的数据存储方案。

文档入口建议从统一索引进入：

- 文档总索引（SSOT）：[docs/DOCUMENTATION-INDEX.md](../DOCUMENTATION-INDEX.md)
- 项目编码与架构规范（SSOT）：[docs/overview/PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md)

## 🏗️ 技术与目录约定

### 技术选型（基线）

- 主数据库：PostgreSQL（项目基线以 [docs/architecture/architecture.md](../architecture/architecture.md) 的技术栈为准）
- 缓存：Redis
- 连接池：HikariCP
- ORM：MyBatis Plus

### 仓库内的“事实来源”

以仓库现存 SQL 脚本与 DDL 为准（文档只描述规范，不替代实际 DDL）：

- 初始化与系统表：[sql/init.sql](../../sql/init.sql)
- 站点/充电桩表：[sql/charging_station_tables.sql](../../sql/charging_station_tables.sql)
- 订单/计费表：[sql/evcs_order_tables.sql](../../sql/evcs_order_tables.sql)
- 索引优化示例：[sql/performance-indexes.sql](../../sql/performance-indexes.sql)

## 📋 命名规范

### 表与字段命名

- 表名：`snake_case`，与现有表一致（如 `sys_user`、`charging_station`、`charging_order`）。
- 字段名：`snake_case`。
- 主键：优先使用 `id`；若领域内已有约定（如 `station_id`、`charger_id`），则同域保持一致。
- 外键列：使用被引用主键名（如 `station_id`、`charger_id`），避免混用 `id` 与 `xxx_id`。

### 索引与约束命名

- 普通索引：`idx_<table>_<col1>_<col2>`
- 唯一索引：`uk_<table>_<col1>_<col2>`（或 `uk_...`）
- 外键：`fk_<table>_<ref>`

## 🧱 通用字段规范（强制）

EVCS 的多数业务表遵循同一套审计/多租户/软删除字段（与现有 DDL 一致）：

```sql
CREATE TABLE example_table (
    id BIGSERIAL PRIMARY KEY,

    -- 多租户隔离（必须）
    tenant_id BIGINT NOT NULL,

    -- 审计字段（建议统一）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,

    -- 软删除与乐观锁
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0
);

-- 常见索引形态（按业务调整）
CREATE INDEX IF NOT EXISTS idx_example_tenant ON example_table(tenant_id);
```

### 软删除（deleted）

- 使用 `deleted INTEGER`，约定：`0=未删除`，`1=已删除`。
- 业务查询必须带 `deleted = 0`（应用层与索引设计共同保证）。

### 唯一约束（与软删除配套）

对“业务唯一键”（例如 `station_code`、`charger_code`）应使用 **部分唯一索引**，避免软删除后阻塞重建：

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uk_station_code_tenant
ON charging_station(station_code, tenant_id)
WHERE deleted = 0;
```

## 🔐 多租户设计

### 基线策略（默认）

- 所有业务表必须包含 `tenant_id`。
- 应用侧通过多租户拦截/上下文保证 “查询必带 tenant_id”。

### 外键策略（建议）

- 跨表引用尽量在同一领域内建立外键（例如 `charger.station_id → charging_station.station_id`）。
- 若引用租户表：根据实际字段选择 `sys_tenant.id` 或 `sys_tenant.tenant_id`，并确保被引用列具备唯一约束。

## 🧾 字段类型与约束

### 推荐类型

- 金额：`DECIMAL(10,2)` 或 `DECIMAL(12,4)`（按精度需求选择，保持同域一致）
- 电量/能耗：`DECIMAL(12,4)`（kWh）
- 状态：优先 `INTEGER`（与枚举/字典表对应），避免字符串状态散落
- JSON：PostgreSQL 推荐 `JSONB`
- 多值枚举：可使用数组（如 `INTEGER[]`），但需明确取值域

### 示例约束

```sql
amount DECIMAL(12,2) NOT NULL CHECK (amount >= 0);
rate DECIMAL(5,2) NOT NULL CHECK (rate >= 0 AND rate <= 100);
phone VARCHAR(20) CHECK (phone ~ '^[0-9]{11}$');
```

### 注释（PostgreSQL 语法）

PostgreSQL 不支持 MySQL 风格的列内联 `COMMENT '...'`，应使用：

```sql
COMMENT ON TABLE charging_station IS '充电站表';
COMMENT ON COLUMN charging_station.station_code IS '站点编码';
```

## 🚀 索引与性能基线

### 设计原则

1. 优先覆盖租户维度：多数查询以 `tenant_id` 开头设计复合索引。
2. 软删除字段参与索引：高频查询建议把 `deleted` 纳入索引或使用部分索引。
3. 写多表避免过度索引：每个索引都带来写入成本。

### 常见索引形态（示例）

```sql
-- 租户 + 状态
CREATE INDEX IF NOT EXISTS idx_charger_tenant_status
ON charger(tenant_id, status, deleted);

-- 业务唯一键（配合软删除）
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_tenant_username
ON sys_user(tenant_id, username)
WHERE deleted = 0;

-- 会话定位
CREATE INDEX IF NOT EXISTS idx_order_session_tenant
ON charging_order(session_id, tenant_id)
WHERE deleted = 0;
```

## ⏱️ update_time 自动维护

若选择数据库触发器自动维护 `update_time`，请采用与现有脚本一致的方式（参考 [sql/charging_station_tables.sql](../../sql/charging_station_tables.sql)）。

## 📦 变更与迁移规范

### 脚本落地位置

数据库变更脚本统一放在：

- [database/scripts/](../../database/scripts/)

命名建议：`YYYY-MM-DD-<action>-<short-desc>.sql`（示例可参考该目录现有文件）。

### 可重复执行（强制）

- 尽量使用 `IF NOT EXISTS` / `DO $$ ... $$;` 防御重复执行。
- 变更脚本必须可在“已存在部分对象”的情况下安全运行。

## 🧪 测试建议（与项目实践对齐）

- SQL 级别：优先使用真实 PostgreSQL（如集成测试环境或容器化环境）验证 DDL/索引。
- 应用级别：在服务测试中覆盖“tenant_id + deleted”过滤、唯一键、关键索引路径的查询。

## 📚 相关文档

- [项目编码与架构规范（SSOT）](../overview/PROJECT-CODING-STANDARDS.md)
- [API 设计规范](API-DESIGN-STANDARDS.md)
- [统一测试指南](../testing/UNIFIED-TESTING-GUIDE.md)
- [统一部署指南](../deployment/DEPLOYMENT-GUIDE.md)

---

本规范以“与仓库现存 DDL 一致”为第一原则：如规范与现状冲突，应以 SSOT 与实际 DDL 为准，并通过脚本迁移逐步收敛。
