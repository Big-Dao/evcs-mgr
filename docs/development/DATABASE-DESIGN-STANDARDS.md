# EVCS数据库设计规范

> **版本**: v1.1 | **最后更新**: 2025-11-10 | **维护者**: 数据架构师 | **状态**: 活跃
>
> 📋 **本文档定义EVCS项目数据库设计标准和规范**

## 🎯 概述

本文档为EVCS充电站管理系统建立统一的数据库设计标准，确保数据库结构的一致性、性能和可维护性。

## 🏗️ 数据库架构

### 技术选型
- **主数据库**: PostgreSQL 15
- **缓存数据库**: Redis 7
- **连接池**: HikariCP
- **ORM框架**: MyBatis Plus

### 微服务数据库划分
| 服务 | 数据库 | 用途 |
|------|--------|------|
| evcs-auth | evcs_auth | 用户认证、权限管理 |
| evcs-station | evcs_station | 充电站、充电桩管理 |
| evcs-order | evcs_order | 订单、充电记录 |
| evcs-payment | evcs_payment | 支付记录、财务 |
| evcs-tenant | evcs_tenant | 租户管理 |
| evcs-common | evcs_common | 公共数据（字典等） |

## 📋 表设计规范

### 命名规范
- **表名**: 小写字母 + 下划线，使用复数形式
- **字段名**: 小写字母 + 下划线
- **索引名**: `idx_表名_字段名`
- **外键名**: `fk_表名_字段名`
- **唯一约束**: `uk_表名_字段名`

### 基础字段规范
所有业务表都应包含以下基础字段：

```sql
CREATE TABLE example_table (
    -- 主键
    id BIGSERIAL PRIMARY KEY,

    -- 租户隔离字段（必须）
    tenant_id BIGINT NOT NULL,

    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version INTEGER NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- 业务字段
    -- ...

    -- 索引
    CONSTRAINT uk_example_tenant_id UNIQUE (id, tenant_id)
);
```

### 字段类型规范

#### 通用字段类型
| 数据类型 | 用途 | 示例 |
|----------|------|------|
| BIGSERIAL | 自增主键 | `id BIGSERIAL PRIMARY KEY` |
| BIGINT | 数值ID | `user_id BIGINT` |
| VARCHAR(n) | 短文本 | `name VARCHAR(100)` |
| TEXT | 长文本 | `description TEXT` |
| DECIMAL(p,s) | 金额 | `amount DECIMAL(12,2)` |
| TIMESTAMP | 时间戳 | `created_at TIMESTAMP` |
| BOOLEAN | 布尔值 | `active BOOLEAN` |
| JSON | JSON数据 | `metadata JSON` |

#### 特殊字段类型
```sql
-- 金额字段（精确到分）
amount DECIMAL(12,2) NOT NULL CHECK (amount >= 0)

-- 百分比字段
rate DECIMAL(5,2) NOT NULL CHECK (rate >= 0 AND rate <= 100)

-- 手机号码
phone VARCHAR(20) CHECK (phone ~ '^[0-9]{11}$')

-- 邮箱
email VARCHAR(255) CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

-- 状态字段
status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING'))
```

## 🔐 多租户设计

### 租户隔离策略
采用**行级安全（Row Level Security）**实现多租户数据隔离：

```sql
-- 启用行级安全
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- 创建租户隔离策略
CREATE POLICY tenant_isolation ON orders
    FOR ALL
    TO application_user
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

-- 在应用层设置租户上下文
SET app.current_tenant_id = '123';
```

### 租户相关索引
```sql
-- 租户相关查询的复合索引
CREATE INDEX idx_orders_tenant_status ON orders(tenant_id, status);
CREATE INDEX idx_orders_tenant_created ON orders(tenant_id, created_at DESC);
CREATE INDEX idx_charging_poles_tenant_station ON charging_poles(tenant_id, station_id);
```

## 📊 核心表设计

### 用户表 (users)
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    real_name VARCHAR(100),
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version INTEGER NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_users_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email)
);

-- 索引
CREATE INDEX idx_users_tenant_status ON users(tenant_id, status);
CREATE INDEX idx_users_tenant_phone ON users(tenant_id, phone);
```

### 充电站表 (stations)
```sql
CREATE TABLE stations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_no VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    address TEXT,
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    available_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE')),
    operator_info JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version INTEGER NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_stations_tenant_no UNIQUE (tenant_id, station_no)
);

-- 索引
CREATE INDEX idx_stations_tenant_status ON stations(tenant_id, status);
CREATE INDEX idx_stations_location ON stations(latitude, longitude) WHERE latitude IS NOT NULL;
```

### 充电桩表 (charging_poles)
```sql
CREATE TABLE charging_poles (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    pole_no VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    power_rate DECIMAL(8,2) NOT NULL CHECK (power_rate > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'OFFLINE')),
    protocol_type VARCHAR(20) NOT NULL DEFAULT 'OCPP'
        CHECK (protocol_type IN ('OCPP', 'GB', 'PRIVATE')),
    metadata JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version INTEGER NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_poles_tenant_station_no UNIQUE (tenant_id, station_id, pole_no),
    CONSTRAINT fk_poles_station FOREIGN KEY (station_id) REFERENCES stations(id)
);

-- 索引
CREATE INDEX idx_poles_tenant_station ON charging_poles(tenant_id, station_id);
CREATE INDEX idx_poles_tenant_status ON charging_poles(tenant_id, status);
```

### 订单表 (orders)
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_no VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    pole_id BIGINT,
    plan_type VARCHAR(20) NOT NULL DEFAULT 'TIME_BASED'
        CHECK (plan_type IN ('TIME_BASED', 'AMOUNT_BASED', 'AUTO')),
    planned_amount DECIMAL(12,2),
    planned_duration INTEGER, -- 分钟
    actual_amount DECIMAL(12,2),
    actual_duration INTEGER, -- 分钟
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CHARGING', 'COMPLETED', 'CANCELLED', 'FAILED')),
    total_fee DECIMAL(12,2) DEFAULT 0,
    service_fee DECIMAL(12,2) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version INTEGER NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_orders_tenant_no UNIQUE (tenant_id, order_no),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_station FOREIGN KEY (station_id) REFERENCES stations(id),
    CONSTRAINT fk_orders_pole FOREIGN KEY (pole_id) REFERENCES charging_poles(id)
);

-- 索引
CREATE INDEX idx_orders_tenant_user ON orders(tenant_id, user_id);
CREATE INDEX idx_orders_tenant_status ON orders(tenant_id, status);
CREATE INDEX idx_orders_tenant_created ON orders(tenant_id, created_at DESC);
CREATE INDEX idx_orders_tenant_station_status ON orders(tenant_id, station_id, status);
```

### 支付记录表 (payments)
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    payment_no VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(20) NOT NULL
        CHECK (payment_method IN ('ALIPAY', 'WECHAT', 'UNION_PAY', 'BALANCE')),
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED')),
    third_party_trade_no VARCHAR(100),
    third_party_response JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    version INTEGER NOT NULL DEFAULT 1,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_payments_tenant_no UNIQUE (tenant_id, payment_no),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- 索引
CREATE INDEX idx_payments_tenant_order ON payments(tenant_id, order_id);
CREATE INDEX idx_payments_tenant_status ON payments(tenant_id, status);
CREATE INDEX idx_payments_tenant_method ON payments(tenant_id, payment_method);
```

## 🚀 性能优化

### 索引设计原则
1. **为主键创建聚簇索引**
2. **为外键创建索引**
3. **为查询条件创建索引**
4. **为排序字段创建索引**
5. **避免过多索引影响写入性能**

### 复合索引设计
```sql
-- 订单查询优化
CREATE INDEX idx_orders_user_status_created
ON orders(tenant_id, user_id, status, created_at DESC);

-- 充电站可用性查询
CREATE INDEX idx_stations_status_capacity
ON stations(tenant_id, status, available_count)
WHERE status = 'ACTIVE';

-- 支付记录查询
CREATE INDEX idx_payments_order_status
ON payments(tenant_id, order_id, status);
```

### 分区表设计
```sql
-- 按时间分区订单表
CREATE TABLE orders_partitioned (
    LIKE orders INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- 创建月度分区
CREATE TABLE orders_2025_11 PARTITION OF orders_partitioned
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE orders_2025_12 PARTITION OF orders_partitioned
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
```

### 查询优化
```sql
-- 使用EXPLAIN ANALYZE分析查询
EXPLAIN ANALYZE
SELECT o.*, u.username, s.name as station_name
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN stations s ON o.station_id = s.id
WHERE o.tenant_id = 1
  AND o.status = 'COMPLETED'
  AND o.created_at >= '2025-11-01'
ORDER BY o.created_at DESC
LIMIT 20;

-- 避免N+1查询问题
-- ❌ 错误：N+1查询
SELECT * FROM orders WHERE tenant_id = 1;
-- 然后对每个订单执行：
SELECT * FROM users WHERE id = ?;

-- ✅ 正确：使用JOIN查询
SELECT o.*, u.username, s.name
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN stations s ON o.station_id = s.id
WHERE o.tenant_id = 1;
```

## 🔒 安全设计

### 数据加密
```sql
-- 敏感数据加密存储
CREATE TABLE user_sensitive_info (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    id_card_number TEXT, -- 加密存储
    bank_account TEXT,   -- 加密存储
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_sensitive_tenant_user UNIQUE (tenant_id, user_id)
);

-- 创建加密函数
CREATE OR REPLACE FUNCTION encrypt_sensitive_data(data TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN encode(encrypt(data::bytea, 'encryption_key', 'aes'), 'base64');
END;
$$ LANGUAGE plpgsql;
```

### 审计日志
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    record_id BIGINT NOT NULL,
    operation VARCHAR(20) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    old_values JSON,
    new_values JSON,
    changed_by VARCHAR(64),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建审计触发器
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_logs (tenant_id, table_name, record_id, operation, new_values, changed_by)
        VALUES (NEW.tenant_id, TG_TABLE_NAME, NEW.id, 'INSERT', row_to_json(NEW), NEW.updated_by);
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_logs (tenant_id, table_name, record_id, operation, old_values, new_values, changed_by)
        VALUES (NEW.tenant_id, TG_TABLE_NAME, NEW.id, 'UPDATE', row_to_json(OLD), row_to_json(NEW), NEW.updated_by);
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audit_logs (tenant_id, table_name, record_id, operation, old_values, changed_by)
        VALUES (OLD.tenant_id, TG_TABLE_NAME, OLD.id, 'DELETE', row_to_json(OLD), OLD.updated_by);
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;
```

## 📋 数据迁移规范

### 版本控制
```sql
-- 创建迁移版本表
CREATE TABLE schema_migrations (
    version VARCHAR(20) PRIMARY KEY,
    description TEXT,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checksum VARCHAR(64)
);

-- 迁移脚本命名规范
-- V20251107__001_create_users_table.sql
-- V20251107__002_add_indexes_to_users.sql
-- V20251108__001_create_orders_table.sql
```

### 迁移脚本示例
```sql
-- V20251107__001_create_orders_table.sql
-- 创建订单表
CREATE TABLE orders (
    -- 表结构定义
);

-- 添加索引
CREATE INDEX idx_orders_tenant_user ON orders(tenant_id, user_id);

-- 添加注释
COMMENT ON TABLE orders IS '充电订单表';
COMMENT ON COLUMN orders.status IS '订单状态：PENDING-待处理，CHARGING-充电中，COMPLETED-已完成，CANCELLED-已取消，FAILED-失败';

-- 记录迁移
INSERT INTO schema_migrations (version, description, checksum)
VALUES ('20251107.001', '创建订单表', 'abc123def456');
```

## 🧪 数据库测试

### 测试数据准备
```sql
-- 创建测试数据函数
CREATE OR REPLACE FUNCTION create_test_data(tenant_id_param BIGINT)
RETURNS VOID AS $$
DECLARE
    test_user_id BIGINT;
    test_station_id BIGINT;
BEGIN
    -- 创建测试用户
    INSERT INTO users (tenant_id, username, password, email)
    VALUES (tenant_id_param, 'test_user', 'password', 'test@example.com')
    RETURNING id INTO test_user_id;

    -- 创建测试充电站
    INSERT INTO stations (tenant_id, station_no, name, address, capacity)
    VALUES (tenant_id_param, 'TEST001', '测试充电站', '测试地址', 10)
    RETURNING id INTO test_station_id;

    -- 创建测试订单
    INSERT INTO orders (tenant_id, order_no, user_id, station_id, status, total_fee)
    VALUES (tenant_id_param, 'TEST-ORDER-001', test_user_id, test_station_id, 'COMPLETED', 100.00);
END;
$$ LANGUAGE plpgsql;
```

### 数据库单元测试
```java
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        Order order = Order.builder()
            .tenantId(1L)
            .orderNo("TEST-ORDER-001")
            .userId(1L)
            .stationId(1L)
            .status(OrderStatus.PENDING)
            .totalFee(new BigDecimal("100.00"))
            .build();

        // When
        Order savedOrder = orderRepository.save(order);

        // Then
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getCreatedAt()).isNotNull();
        assertThat(savedOrder.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldFindOrdersByTenantAndStatus() {
        // Given
        createTestOrders();

        // When
        List<Order> orders = orderRepository.findByTenantIdAndStatus(1L, OrderStatus.COMPLETED);

        // Then
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
```

## 📊 监控和维护

### 性能监控查询
```sql
-- 查看慢查询
SELECT query, calls, total_time, mean_time, rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- 查看索引使用情况
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- 查看表大小
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### 定期维护任务
```sql
-- 更新表统计信息
ANALYZE orders;

-- 重建索引
REINDEX INDEX CONCURRENTLY idx_orders_tenant_status;

-- 清理死元组
VACUUM orders;

-- 清理无用数据
DELETE FROM audit_logs WHERE changed_at < NOW() - INTERVAL '1 year';
```

## 📚 相关文档

- [项目编码标准](../PROJECT-CODING-STANDARDS.md)
- [API设计规范](API-DESIGN-STANDARDS.md)
- [统一测试指南](testing/UNIFIED-TESTING-GUIDE.md)
- [统一部署指南](../deployment/DEPLOYMENT-GUIDE.md)

---

**遵循本数据库设计规范可以确保EVCS项目数据库的性能、安全性和可维护性。**
