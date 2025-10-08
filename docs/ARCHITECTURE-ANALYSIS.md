# EVCS Manager 架构分析文档

> 充电站管理平台（Electric Vehicle Charging Station Manager）完整架构分析
> 
> **版本**: v1.0.0  
> **更新日期**: 2024年  
> **作者**: 架构团队

---

## 目录

1. [系统概述](#1-系统概述)
2. [业务背景与目标](#2-业务背景与目标)
3. [核心架构设计](#3-核心架构设计)
4. [多租户数据隔离架构（四层设计）](#4-多租户数据隔离架构四层设计)
5. [微服务架构与模块设计](#5-微服务架构与模块设计)
6. [核心技术组件](#6-核心技术组件)
7. [数据模型设计](#7-数据模型设计)
8. [业务流程与数据流](#8-业务流程与数据流)
9. [技术栈分析](#9-技术栈分析)
10. [安全架构](#10-安全架构)
11. [性能与可扩展性](#11-性能与可扩展性)
12. [可观测性与监控](#12-可观测性与监控)
13. [部署架构](#13-部署架构)
14. [架构优势与特点](#14-架构优势与特点)
15. [技术债务与改进方向](#15-技术债务与改进方向)

---

## 1. 系统概述

EVCS Manager 是一个基于 **Spring Boot 3.2** 和 **Java 21** 构建的现代化电动汽车充电站管理平台。该系统采用**微服务架构**和**多租户设计**，支持大规模充电站和充电桩的运营管理。


### 1.1 系统定位

- **目标用户**：充电运营商、平台方、第三方合作伙伴
- **核心价值**：提供安全、高效、可扩展的充电站运营管理能力
- **技术特色**：四层多租户隔离、声明式权限控制、多协议支持

### 1.2 系统规模

| 指标 | 设计容量 |
|------|---------|
| 充电站数量 | 最多 500 个 |
| 单站充电桩数 | 最多 5000 个 |
| 并发能力 | 高并发处理 |
| 租户层级 | 支持多层级租户管理 |

### 1.3 关键特性

✅ **多租户架构** - 分层租户管理，上级可管理下级，数据完全隔离  
✅ **四层数据隔离** - 数据库层、SQL层、服务层、API层全方位数据隔离  
✅ **微服务设计** - 服务解耦，高可扩展  
✅ **多协议支持** - OCPP 1.6、云快充协议  
✅ **分时电价计费** - 灵活的分时段计费方案（TOU）  
✅ **实时监控** - 完善的监控系统和故障诊断  
✅ **第三方对接** - 标准 RESTful API  

---

## 2. 业务背景与目标

### 2.1 业务场景

充电站运营商需要管理大量的充电站和充电桩，涉及：

1. **多主体运营** - 平台方、运营商、合作伙伴等多方参与
2. **复杂计费规则** - 分时电价、服务费、不同充电桩差异化定价
3. **设备协议多样** - OCPP、云快充等多种通信协议
4. **支付渠道集成** - 支付宝、微信、网联等多种支付方式
5. **实时监控需求** - 设备状态、充电会话、订单流水实时监控
6. **数据安全隔离** - 不同运营商数据严格隔离

### 2.2 核心目标

| 目标维度 | 具体要求 |
|---------|---------|
| **数据隔离** | 多租户数据完全隔离，防止跨租户数据泄露 |
| **可扩展性** | 支持水平扩展，适应业务增长 |
| **高可用** | 核心接口可用性 99.9% |
| **灵活计费** | 支持复杂的分时电价和计费规则 |
| **协议兼容** | 兼容主流充电桩通信协议 |
| **可观测性** | 完整的日志、指标、告警体系 |

---

## 3. 核心架构设计

### 3.1 总体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      客户端层                                  │
│  (Web管理后台 / 移动App / 第三方系统 / 充电桩设备)              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   API网关层 (Gateway)                         │
│  - 路由转发  - 负载均衡  - 认证鉴权  - 限流熔断               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     微服务层                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │  认证服务 │  │  租户服务 │  │  站点服务 │  │  订单服务 │    │
│  │   Auth   │  │  Tenant  │  │ Station  │  │  Order   │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │  支付服务 │  │  协议服务 │  │  监控服务 │  │  集成服务 │    │
│  │ Payment  │  │ Protocol │  │Monitoring│  │Integration│   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   公共组件层 (Common)                          │
│  - 多租户框架  - 数据权限  - 统一异常  - 通用工具             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   基础设施层                                   │
│  PostgreSQL  │  Redis  │  RabbitMQ  │  配置中心              │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 架构分层说明

#### 3.2.1 客户端层
- **Web 管理后台**：运营管理人员使用
- **移动 App**：充电用户使用
- **第三方系统**：通过 API 集成
- **充电桩设备**：通过协议服务连接

#### 3.2.2 网关层 (evcs-gateway)
- **功能**：统一入口、路由转发
- **职责**：认证鉴权、限流熔断、负载均衡
- **技术**：Spring Cloud Gateway

#### 3.2.3 微服务层
由 8 个核心微服务组成，各司其职

#### 3.2.4 公共组件层 (evcs-common)
提供所有微服务共享的基础能力

#### 3.2.5 基础设施层
数据库、缓存、消息队列等基础服务

---

## 4. 多租户数据隔离架构（四层设计）

这是 EVCS Manager 最核心的架构设计，也是系统的技术亮点。

### 4.1 四层隔离架构概览

系统采用**四层租户隔离机制**，从数据库到 API 层层防护，确保不同租户数据完全隔离：

```
┌──────────────────────────────────────────────────────────────┐
│  第4层：API层隔离 (API Layer)                                  │
│  - Spring Security + JWT 认证                                 │
│  - TenantInterceptor 提取租户上下文                            │
│  - TenantContext ThreadLocal 存储                             │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  第3层：服务层隔离 (Service Layer)                             │
│  - @DataScope 注解声明数据权限                                 │
│  - DataScopeAspect AOP 切面拦截                               │
│  - 支持 TENANT、TENANT_HIERARCHY、USER 等权限类型              │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  第2层：SQL层隔离 (SQL Layer)                                  │
│  - MyBatis Plus TenantLineHandler                            │
│  - 自动在所有 SQL 添加 WHERE tenant_id = ?                     │
│  - INSERT 语句自动注入 tenant_id                               │
└──────────────────────────────────────────────────────────────┘
                            ↓
┌──────────────────────────────────────────────────────────────┐
│  第1层：数据库层隔离 (Database Layer)                          │
│  - PostgreSQL 行级数据隔离                                     │
│  - 所有业务表包含 tenant_id 字段                                │
│  - 外键约束确保数据一致性                                       │
└──────────────────────────────────────────────────────────────┘
```


### 4.2 第一层：数据库层隔离

#### 设计原则
所有业务表都包含 `tenant_id` 字段作为租户隔离字段。

#### 租户层级关系示例
```
平台方 (tenant_id=1)
  ├─ 运营商A (tenant_id=10, parent_id=1, ancestors='0,1')
  │   ├─ 合作伙伴A1 (tenant_id=101, parent_id=10, ancestors='0,1,10')
  │   └─ 合作伙伴A2 (tenant_id=102, parent_id=10, ancestors='0,1,10')
  └─ 运营商B (tenant_id=20, parent_id=1, ancestors='0,1')
      └─ 合作伙伴B1 (tenant_id=201, parent_id=20, ancestors='0,1,20')
```

### 4.3 第二层：SQL 层隔离

**核心组件**：`CustomTenantLineHandler`

自动 SQL 改写示例：

**原始 SQL**：
```sql
SELECT * FROM charging_station WHERE status = 1;
```

**自动改写后**：
```sql
SELECT * FROM charging_station 
WHERE status = 1 AND tenant_id = 10;
```

### 4.4 第三层：服务层隔离

**核心注解**：`@DataScope`

权限类型：
- `TENANT` - 仅当前租户
- `TENANT_HIERARCHY` - 当前租户及子租户
- `USER` - 仅当前用户
- `ROLE` - 基于角色
- `ALL` - 所有数据（仅系统管理员）

### 4.5 第四层：API 层隔离

**核心组件**：
- `TenantContext` - ThreadLocal 存储租户上下文
- `TenantInterceptor` - 从 JWT 提取租户信息并设置上下文

### 4.6 权限控制完整流程

```
HTTP请求 (带JWT Token)
    ↓
TenantInterceptor 拦截器
    ├─ 解析 JWT
    ├─ 提取租户ID、用户ID、租户类型、祖先路径
    └─ 设置 TenantContext
    ↓
Spring Security 认证鉴权
    ├─ 验证 Token 有效性
    └─ 检查接口权限
    ↓
Controller 方法 (@DataScope 注解)
    ↓
DataScopeAspect AOP切面
    ├─ 检查数据权限类型
    ├─ 从方法参数提取目标租户ID
    └─ 验证权限
    ↓
Service 方法执行
    ↓
MyBatis Plus 查询
    └─ CustomTenantLineHandler
        └─ 自动添加 WHERE tenant_id = ?
    ↓
数据库查询（行级隔离）
    ↓
返回响应
    ↓
TenantContext.clear() 清理上下文
```

---

## 5. 微服务架构与模块设计

### 5.1 微服务模块总览

```
evcs-mgr/
├── evcs-common/          # 公共组件模块 ⭐
├── evcs-gateway/         # API网关服务
├── evcs-auth/            # 认证授权服务
├── evcs-tenant/          # 租户管理服务
├── evcs-station/         # 充电站管理服务 ✅ 已实现
├── evcs-order/           # 订单管理服务 ✅ 已实现
├── evcs-payment/         # 支付集成服务 🚧 规划中
├── evcs-protocol/        # 协议处理服务 ✅ 已实现
├── evcs-monitoring/      # 监控告警服务 🚧 规划中
├── evcs-integration/     # 第三方集成服务 🚧 规划中
└── evcs-config/          # 配置中心服务
```

### 5.2 核心微服务详解

#### 5.2.1 evcs-common (公共组件) ⭐

**职责**：为所有微服务提供共享的基础能力

**核心组件**：

| 组件分类 | 组件名称 | 说明 |
|---------|---------|------|
| **租户框架** | TenantContext | ThreadLocal 存储租户上下文 |
|  | TenantInterceptor | HTTP 拦截器，提取租户信息 |
|  | CustomTenantLineHandler | MyBatis Plus 租户处理器 |
| **数据权限** | @DataScope 注解 | 声明式数据权限标记 |
|  | DataScopeAspect | AOP 切面，权限检查 |
| **统一响应** | Result | 统一响应封装 |
|  | ResultCode | 响应码枚举 |
|  | PageResult | 分页响应 |
| **统一异常** | BusinessException | 业务异常 |
|  | GlobalExceptionHandler | 全局异常处理器 |
| **基础实体** | BaseEntity | 包含审计字段的基类 |
| **工具类** | JwtUtil | JWT 工具类 |
| **配置类** | MybatisPlusConfig | MyBatis Plus 配置 |
|  | WebConfig | Web 配置 |

#### 5.2.2 evcs-station (充电站管理) ✅

**职责**：充电站和充电桩的管理

**核心功能**：
- 充电站 CRUD
- 充电桩 CRUD
- 充电桩状态管理
- 实时数据更新
- 地理位置查询
- 离线检测

**关键API**：
```
POST   /station              - 创建充电站
GET    /station/{id}         - 查询充电站
GET    /station/list         - 充电站列表
GET    /station/nearby       - 附近充电站（基于经纬度）
PUT    /charger/{id}/status  - 更新充电桩状态
PUT    /charger/{id}/data    - 更新实时数据
```

**数据模型**：
- `ChargingStation` - 充电站实体
- `Charger` - 充电桩实体

#### 5.2.3 evcs-order (订单管理) ✅

**职责**：充电订单和计费管理

**核心功能**：
- 充电订单创建（幂等）
- 充电订单完成（幂等、触发计费）
- **分时电价计费（TOU）** ⭐
- 计费计划管理
- 订单查询和分页
- 对账巡检

**分时电价设计亮点**：
- 每天最多 **96 个时段**（15分钟粒度）
- 每个站点最多 **16 组计费方案**
- 支持充电桩绑定特定计费方案
- 支持时段跨午夜
- 自动按时间段分摊电量并计算费用

**关键API**：
```
POST   /orders/start         - 开始充电（幂等）
POST   /orders/stop          - 停止充电（幂等，触发计费）
GET    /orders/page          - 订单分页查询
POST   /billing-plan         - 创建计费方案
POST   /reconcile/run-daily  - 每日对账巡检
```

**计费模型**：
- `BillingPlan` - 计费计划
- `BillingPlanSegment` - 计费时段（最多96段）
- `ChargingOrder` - 充电订单

#### 5.2.4 evcs-protocol (协议处理) ✅

**职责**：与充电桩设备通信

**支持协议**：
- **OCPP 1.6** - WebSocket 长连接
- **云快充协议** - HTTP RESTful API

**核心接口**：
- `IOCPPProtocolService` - OCPP 协议服务
- `ICloudChargeProtocolService` - 云快充协议服务
- `ProtocolEventListener` - 协议事件监听器

**事件驱动流程**：
```
充电桩设备
    ↓ (WebSocket/HTTP)
协议服务 (evcs-protocol)
    ↓ (事件回调)
订单服务 (evcs-order)
    └─ 创建/更新订单
```

#### 5.2.5 evcs-tenant (租户管理)

**职责**：租户的创建、查询、更新、删除

**核心功能**：
- 租户 CRUD
- 租户层级管理
- 租户树查询
- 租户配额管理

#### 5.2.6 evcs-gateway (API网关)

**职责**：统一入口、路由转发

**核心功能**：
- 路由配置
- 全局过滤器（认证、限流、日志）
- 负载均衡
- 跨域处理
- 熔断降级

#### 5.2.7 evcs-auth (认证授权)

**职责**：用户认证、Token 管理

**核心功能**：
- 用户登录/登出
- JWT Token 生成和验证
- 权限管理

### 5.3 微服务间通信

**通信方式**：
- **同步调用**：HTTP/REST（通过 Feign 或 RestTemplate）
- **异步消息**：RabbitMQ（事件驱动）

**示例场景**：
```
协议服务 (Protocol) 
    ↓ (HTTP 同步调用)
订单服务 (Order) 
    ↓ (MQ 异步消息)
支付服务 (Payment)
```

---

## 6. 核心技术组件

### 6.1 多租户框架

#### TenantContext (租户上下文)
```java
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> TENANT_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_ANCESTORS = new ThreadLocal<>();
    
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    public static Long getTenantId() {
        return TENANT_ID.get();
    }
    
    public static boolean hasAccessToTenant(Long targetTenantId) {
        if (isSystemAdmin()) return true;
        Long currentTenantId = getTenantId();
        if (currentTenantId.equals(targetTenantId)) return true;
        
        // 检查祖先路径
        String ancestors = getTenantAncestors();
        return ancestors != null && ancestors.contains("," + currentTenantId + ",");
    }
    
    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
        TENANT_TYPE.remove();
        TENANT_ANCESTORS.remove();
    }
}
```

**关键特性**：
- 使用 `ThreadLocal` 确保线程安全
- 支持租户层级权限检查
- 请求结束后自动清理，防止内存泄漏

#### CustomTenantLineHandler (SQL 过滤器)
```java
public class CustomTenantLineHandler implements TenantLineHandler {
    
    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("租户上下文中未找到租户ID");
            return new LongValue(-1);  // 返回不存在的ID，防止泄露数据
        }
        return new LongValue(tenantId);
    }
    
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
    
    @Override
    public boolean ignoreTable(String tableName) {
        // 系统表不进行租户隔离
        return IGNORE_TABLES.contains(tableName);
    }
}
```

**工作原理**：
- MyBatis Plus 在执行 SQL 前拦截
- 自动在 WHERE 子句添加 `tenant_id = ?`
- INSERT 语句自动注入 `tenant_id` 字段

### 6.2 数据权限框架

#### @DataScope 注解
```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    DataScopeType value() default DataScopeType.TENANT;
    String description() default "";
    
    enum DataScopeType {
        TENANT,            // 当前租户
        TENANT_HIERARCHY,  // 当前租户及子租户
        USER,              // 当前用户
        ALL                // 所有数据（系统管理员）
    }
}
```

#### DataScopeAspect (AOP 切面)
```java
@Aspect
@Component
public class DataScopeAspect {
    
    @Before("@annotation(com.evcs.common.annotation.DataScope)")
    public void checkDataScope(JoinPoint joinPoint) {
        DataScope dataScope = getDataScope(joinPoint);
        Long currentTenantId = TenantContext.getTenantId();
        
        switch (dataScope.value()) {
            case TENANT:
                checkTenantPermission(joinPoint, currentTenantId);
                break;
            case TENANT_HIERARCHY:
                checkTenantHierarchyPermission(joinPoint, currentTenantId);
                break;
            case ALL:
                if (!TenantContext.isSystemAdmin()) {
                    throw new BusinessException("权限不足");
                }
                break;
        }
    }
}
```

### 6.3 分时电价计费引擎

#### 计费算法核心逻辑
```
输入：
- 开始时间：2024-01-01 22:00
- 结束时间：2024-01-02 02:00
- 总电量：40 kWh
- 计费方案：包含 96 个时段

处理流程：
1. 将充电时段按自然日切分
   - 2024-01-01 22:00 ~ 23:59  (2小时)
   - 2024-01-02 00:00 ~ 02:00  (2小时)

2. 与每日分时段求交集
   - 22:00~23:00 峰时段：电价 1.2元/kWh，服务费 0.8元/kWh
   - 23:00~06:00 谷时段：电价 0.4元/kWh，服务费 0.6元/kWh

3. 按时间占比分摊电量
   - 峰时段（1小时）：10 kWh × (1.2 + 0.8) = 20元
   - 谷时段（3小时）：30 kWh × (0.4 + 0.6) = 30元
   - 总计：50元
```

**技术特点**：
- 最多 96 段/天（15分钟粒度）
- 支持跨午夜时段
- 时段不重叠校验
- 每站最多 16 组方案
- 充电桩可绑定特定方案

---

## 7. 数据模型设计

### 7.1 核心实体关系图

```
sys_tenant (租户)
    ├─ 1:N → sys_user (用户)
    ├─ 1:N → charging_station (充电站)
    └─ 1:N → billing_plan (计费方案)

charging_station (充电站)
    ├─ 1:N → charger (充电桩)
    └─ 1:N → billing_plan (站点计费方案)

charger (充电桩)
    ├─ N:1 → charging_station (所属充电站)
    ├─ N:1 → billing_plan (绑定的计费方案)
    └─ 1:N → charging_order (充电订单)

billing_plan (计费方案)
    └─ 1:N → billing_plan_segment (时段明细，最多96段)

charging_order (充电订单)
    ├─ N:1 → charger (充电桩)
    ├─ N:1 → billing_plan (使用的计费方案)
    └─ N:1 → sys_user (充电用户)
```

### 7.2 关键表结构

#### sys_tenant (租户表)
```sql
CREATE TABLE sys_tenant (
    id BIGINT PRIMARY KEY,
    tenant_code VARCHAR(50) UNIQUE NOT NULL,
    tenant_name VARCHAR(100) NOT NULL,
    parent_id BIGINT REFERENCES sys_tenant(id),
    ancestors TEXT,  -- 祖先路径：0,1,10
    tenant_type INTEGER,  -- 1-平台方，2-运营商，3-合作伙伴
    status INTEGER DEFAULT 1,
    max_users INTEGER DEFAULT 100,
    max_stations INTEGER DEFAULT 50,
    max_chargers INTEGER DEFAULT 1000,
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

#### charging_station (充电站表)
```sql
CREATE TABLE charging_station (
    station_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,  -- 租户隔离字段
    station_code VARCHAR(64) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    station_type INTEGER,  -- 1-公共，2-专用，3-个人
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    address TEXT NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(11,7),
    total_chargers INTEGER DEFAULT 0,
    available_chargers INTEGER DEFAULT 0,
    payment_methods INTEGER[] DEFAULT ARRAY[1,2,3],  -- PostgreSQL 数组
    facilities JSONB DEFAULT '{}',  -- JSONB 存储设施信息
    status INTEGER DEFAULT 1,
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT uk_station_code_tenant UNIQUE(station_code, tenant_id)
);
```

#### charger (充电桩表)
```sql
CREATE TABLE charger (
    charger_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    charger_code VARCHAR(64) NOT NULL,
    charger_name VARCHAR(100) NOT NULL,
    charger_type INTEGER,  -- 1-直流快充，2-交流慢充
    rated_power DECIMAL(8,2),  -- 额定功率 kW
    gun_count INTEGER DEFAULT 1,
    supported_protocols JSONB,  -- 协议支持：{"ocpp": "1.6"}
    status INTEGER,  -- 0-离线，1-空闲，2-充电中，3-故障
    last_heartbeat TIMESTAMP,
    current_session_id VARCHAR(64),
    billing_plan_id BIGINT,  -- 绑定的计费方案
    -- 实时数据
    charged_energy DECIMAL(8,2),
    charged_duration INTEGER,
    current_power DECIMAL(8,2),
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    CONSTRAINT uk_charger_code_tenant UNIQUE(charger_code, tenant_id)
);
```

#### billing_plan (计费方案表)
```sql
CREATE TABLE billing_plan (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT,  -- 所属站点
    name VARCHAR(100) NOT NULL,
    code VARCHAR(64),
    status INTEGER DEFAULT 1,
    is_default INTEGER DEFAULT 0,  -- 是否为站点默认方案
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

#### billing_plan_segment (计费时段表)
```sql
CREATE TABLE billing_plan_segment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    plan_id BIGINT NOT NULL REFERENCES billing_plan(id) ON DELETE CASCADE,
    segment_index INTEGER,  -- 时段序号 0-95
    start_time VARCHAR(8) NOT NULL,  -- HH:mm 格式
    end_time VARCHAR(8) NOT NULL,
    energy_price DECIMAL(10,4) DEFAULT 0,  -- 电价 元/kWh
    service_fee DECIMAL(10,4) DEFAULT 0,   -- 服务费 元/kWh
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### charging_order (充电订单表)
```sql
CREATE TABLE charging_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT,
    charger_id BIGINT,
    session_id VARCHAR(64) NOT NULL,  -- 充电会话ID（幂等键）
    user_id BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    energy DECIMAL(12,4),  -- 充电电量 kWh
    duration BIGINT,  -- 充电时长 分钟
    amount DECIMAL(12,4),  -- 总金额 元
    billing_plan_id BIGINT,  -- 使用的计费方案
    payment_trade_id VARCHAR(100),
    paid_time TIMESTAMP,
    status INTEGER DEFAULT 0,  -- 0-创建，1-完成，10-待支付，11-已支付
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);
```

### 7.3 索引策略

**租户隔离索引**：
```sql
CREATE INDEX idx_station_tenant ON charging_station(tenant_id, status);
CREATE INDEX idx_charger_tenant ON charger(tenant_id, status);
CREATE INDEX idx_order_tenant ON charging_order(tenant_id, status);
```

**业务查询索引**：
```sql
-- 充电桩按站点查询
CREATE INDEX idx_charger_station ON charger(station_id, status);

-- 订单按会话查询（幂等性）
CREATE INDEX idx_order_session_tenant ON charging_order(session_id, tenant_id);

-- 地理位置查询
CREATE INDEX idx_station_location ON charging_station(latitude, longitude);
```

---

## 8. 业务流程与数据流

### 8.1 充电订单完整流程

```
1. 用户发起充电请求
    ↓
2. App/扫码 → API网关 → 订单服务
    ├─ 校验用户权限
    ├─ 检查充电桩状态
    └─ 创建充电订单（幂等）
    ↓
3. 订单服务 → 协议服务
    └─ 发送启动充电指令（OCPP/云快充）
    ↓
4. 协议服务 → 充电桩设备
    └─ WebSocket/HTTP 通信
    ↓
5. 充电桩开始充电
    ├─ 定时上报心跳和实时数据
    └─ 更新充电桩状态
    ↓
6. 用户停止充电
    ↓
7. 充电桩 → 协议服务 → 订单服务
    ├─ 上报充电结束事件
    └─ 包含：电量、时长、结束时间
    ↓
8. 订单服务完成订单（幂等）
    ├─ 查询计费方案
    ├─ 调用分时电价计费引擎
    ├─ 计算总金额
    └─ 更新订单状态为"待支付"
    ↓
9. 订单服务 → 支付服务
    ├─ 创建支付订单
    └─ 返回支付信息
    ↓
10. 用户完成支付
    ├─ 支付宝/微信/网联
    └─ 支付回调（幂等处理）
    ↓
11. 订单状态更新为"已支付"
```

### 8.2 分时电价计费流程

```
1. 订单结束事件触发
    ├─ 充电开始时间：2024-01-01 22:00
    ├─ 充电结束时间：2024-01-02 02:00
    └─ 总电量：40 kWh
    ↓
2. 查询计费方案
    ├─ 优先：充电桩绑定的方案
    ├─ 其次：站点默认方案
    └─ 回落：系统基础费率
    ↓
3. 获取计费时段（96段）
    例如：
    - 00:00-08:00 谷时段：电价0.4元，服务费0.6元
    - 08:00-22:00 平时段：电价0.8元，服务费0.7元
    - 22:00-24:00 峰时段：电价1.2元，服务费0.8元
    ↓
4. 按自然日切分充电时段
    ├─ 2024-01-01 22:00~23:59 (2小时)
    └─ 2024-01-02 00:00~02:00 (2小时)
    ↓
5. 计算每个时段的电量和费用
    ├─ 峰时段（22:00-23:59, 2小时）
    │   └─ 电量：40 × 2/4 = 20 kWh
    │   └─ 费用：20 × (1.2 + 0.8) = 40元
    └─ 谷时段（00:00-02:00, 2小时）
        └─ 电量：40 × 2/4 = 20 kWh
        └─ 费用：20 × (0.4 + 0.6) = 20元
    ↓
6. 汇总计算
    └─ 总费用：40 + 20 = 60元
    ↓
7. 更新订单金额
```

### 8.3 多租户数据访问流程

```
场景：运营商A查询自己的充电站列表

1. 用户登录
    ├─ 输入账号密码
    └─ Auth服务验证
    ↓
2. 生成JWT Token
    包含信息：
    ├─ tenantId: 10 (运营商A)
    ├─ userId: 100
    ├─ tenantType: 2 (运营商)
    └─ ancestors: '0,1,10'
    ↓
3. 请求充电站列表
    GET /station/list
    Header: Authorization: Bearer <token>
    ↓
4. TenantInterceptor 拦截
    ├─ 解析JWT Token
    ├─ 提取租户信息
    └─ 设置TenantContext
        ├─ TenantId = 10
        ├─ UserId = 100
        ├─ TenantType = 2
        └─ Ancestors = '0,1,10'
    ↓
5. Controller 方法
    @DataScope(value = DataScopeType.TENANT_HIERARCHY)
    public Result<List<Station>> list() { ... }
    ↓
6. DataScopeAspect 切面检查
    ├─ 检查权限类型：TENANT_HIERARCHY
    ├─ 当前租户ID：10
    └─ 权限通过✅
    ↓
7. Service 方法执行
    stationMapper.selectList(null)
    ↓
8. MyBatis Plus 拦截
    CustomTenantLineHandler:
    ├─ 从TenantContext获取：tenantId = 10
    └─ SQL改写：
        SELECT * FROM charging_station
        WHERE deleted = 0 AND tenant_id = 10
    ↓
9. 数据库查询
    └─ 返回租户10的所有充电站
    ↓
10. 返回响应
    ↓
11. afterCompletion
    └─ TenantContext.clear()
```

---

## 9. 技术栈分析

### 9.1 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 21 | 开发语言（LTS版本） |
| **Spring Boot** | 3.2.2 | 应用框架 |
| **Spring Cloud** | 2023.0.0 | 微服务框架 |
| **Spring Security** | 6.x | 安全认证框架 |
| **MyBatis Plus** | 3.5.6 | ORM框架（增强） |
| **PostgreSQL** | 15+ | 关系型数据库 |
| **Redis** | 7+ | 缓存、分布式锁 |
| **RabbitMQ** | 3.8+ | 消息队列 |
| **JWT** | 4.4.0 | Token认证 |
| **Knife4j** | 4.4.0 | API文档（Swagger增强） |
| **Hutool** | 5.8.25 | Java工具类库 |
| **Fastjson2** | 2.0.45 | JSON处理 |
| **Lombok** | - | 简化代码 |

### 9.2 构建工具

| 工具 | 版本 | 说明 |
|------|------|------|
| **Gradle** | 8.5+ | 构建工具 |
| **Docker** | - | 容器化 |
| **Docker Compose** | - | 容器编排 |

### 9.3 监控与可观测性

| 工具 | 用途 |
|------|------|
| **Spring Boot Actuator** | 健康检查、指标暴露 |
| **Micrometer** | 指标收集 |
| **Prometheus** | 指标存储和查询 |
| **Grafana** | 可视化仪表盘（规划） |

### 9.4 技术选型说明

#### 为什么选择 Java 21？
- **虚拟线程（Virtual Threads）**：提升并发性能
- **模式匹配增强**：代码更简洁
- **记录类（Record）**：适合 DTO
- **LTS 版本**：长期支持

#### 为什么选择 PostgreSQL？
- **JSONB 类型**：存储灵活配置（facilities、protocols）
- **数组类型**：存储支付方式、枪头类型
- **地理位置支持**：基于经纬度的附近查询
- **事务支持**：强一致性
- **触发器**：自动更新时间戳、统计信息

#### 为什么选择 MyBatis Plus？
- **多租户插件**：开箱即用的租户隔离
- **代码生成器**：提升开发效率
- **分页插件**：简化分页查询
- **乐观锁**：支持版本号

#### 为什么选择 Spring Cloud Gateway？
- **异步非阻塞**：基于 WebFlux
- **性能优秀**：适合高并发场景
- **过滤器丰富**：易扩展

---

## 10. 安全架构

### 10.1 认证与授权

#### 认证流程
```
1. 用户登录
    ↓
2. 验证用户名密码（BCrypt加密）
    ↓
3. 生成 JWT Token
    包含Claims：
    ├─ sub: userId
    ├─ tenantId: 租户ID
    ├─ tenantType: 租户类型
    ├─ ancestors: 祖先路径
    ├─ roles: 角色列表
    └─ exp: 过期时间（2小时）
    ↓
4. 返回 Token 给客户端
    ↓
5. 后续请求携带 Token
    Header: Authorization: Bearer <token>
    ↓
6. Gateway 验证 Token 合法性
    ├─ 签名验证
    ├─ 过期时间检查
    └─ Token 黑名单检查（Redis）
    ↓
7. TenantInterceptor 提取租户信息
    └─ 设置 TenantContext
```

#### 授权控制
```
API层：@PreAuthorize("hasPermission('station:create')")
    ↓
服务层：@DataScope(value = DataScopeType.TENANT)
    ↓
SQL层：自动添加 WHERE tenant_id = ?
    ↓
数据库层：行级隔离
```

### 10.2 数据安全

#### 敏感数据加密
- **密码**：BCrypt 加密存储
- **手机号**：部分脱敏展示（138****8888）
- **身份证号**：加密存储，脱敏展示

#### SQL注入防护
- **MyBatis 参数化查询**：防止 SQL 注入
- **租户ID 自动注入**：防止跨租户访问

#### XSS防护
- **输入校验**：使用 Hibernate Validator
- **输出转义**：HTML 特殊字符转义

### 10.3 接口安全

#### 限流策略
```
基于 Redis + Lua 脚本实现：
- 用户级别限流：100 req/min
- IP 级别限流：1000 req/min
- 接口级别限流：根据业务设置
```

#### 防重放攻击
- **JWT 过期时间**：2 小时
- **Nonce 机制**：关键操作需要一次性随机数

#### 幂等性设计
- **充电订单创建**：session_id 作为幂等键
- **支付回调**：trade_id 作为幂等键
- **订单完成**：session_id + end_time 校验

---

## 11. 性能与可扩展性

### 11.1 性能优化策略

#### 数据库优化
```
1. 索引优化
   ├─ 租户隔离索引：idx_xxx_tenant
   ├─ 业务查询索引：idx_xxx_status
   └─ 复合索引：idx_xxx_tenant_status

2. 查询优化
   ├─ 分页查询：使用 LIMIT/OFFSET
   ├─ 避免 SELECT *
   └─ 使用覆盖索引

3. 连接池配置
   ├─ HikariCP（Spring Boot 默认）
   └─ maxPoolSize: 20
```

#### 缓存策略
```
1. Redis 缓存
   ├─ 租户信息缓存（TTL: 1小时）
   ├─ 计费方案缓存（TTL: 30分钟）
   ├─ 充电桩状态缓存（TTL: 5分钟）
   └─ 用户Token黑名单

2. 本地缓存
   ├─ 字典数据（@Cacheable）
   └─ 配置信息（@Cacheable）

3. 缓存更新策略
   ├─ 主动更新：数据变更时删除缓存
   └─ 被动更新：TTL 到期自动失效
```

#### 异步处理
```
1. RabbitMQ 消息队列
   ├─ 充电事件异步处理
   ├─ 支付回调异步处理
   └─ 订单对账异步任务

2. @Async 异步方法
   ├─ 发送通知
   └─ 生成报表
```

### 11.2 可扩展性设计

#### 水平扩展
```
1. 微服务无状态设计
   ├─ 所有状态存储在 Redis/数据库
   └─ 支持多实例部署

2. 数据库分库分表（规划）
   ├─ 按租户分库：tenant_id % N
   ├─ 按时间分表：charging_order_2024_01
   └─ 使用 ShardingSphere

3. 缓存集群
   └─ Redis Cluster 或 Redis Sentinel
```

#### 垂直扩展
```
1. 服务拆分
   ├─ 按业务领域：订单、支付、协议
   └─ 按职责：读写分离服务

2. 计算资源
   ├─ 增加 CPU/内存
   └─ 使用更强大的服务器
```

### 11.3 性能指标

| 指标 | 目标值 |
|------|--------|
| **接口响应时间** | P95 < 200ms |
| **API可用性** | 99.9% |
| **并发TPS** | > 1000 |
| **数据库连接池** | 最大20个连接 |
| **缓存命中率** | > 90% |

---

## 12. 可观测性与监控

### 12.1 日志体系

#### 日志分类
```
1. 业务日志
   ├─ 订单创建/完成
   ├─ 充电开始/停止
   └─ 支付成功/失败

2. 系统日志
   ├─ 服务启动/停止
   ├─ 异常堆栈
   └─ 性能瓶颈

3. 审计日志
   ├─ 用户登录/登出
   ├─ 数据修改记录
   └─ 权限变更
```

#### 日志规范
```java
// 包含租户信息和用户信息
log.info("[租户:{}] [用户:{}] 创建充电订单 - sessionId={}, chargerId={}",
    TenantContext.getTenantId(),
    TenantContext.getUserId(),
    sessionId,
    chargerId
);
```

### 12.2 指标监控

#### 暴露的指标
```
1. JVM 指标
   ├─ 堆内存使用率
   ├─ GC 次数和时间
   └─ 线程数

2. 业务指标
   ├─ 充电订单数（按状态）
   ├─ 充电桩在线率
   ├─ 充电桩状态分布
   └─ 订单平均金额

3. 接口指标
   ├─ QPS（请求数/秒）
   ├─ 响应时间（P50、P95、P99）
   ├─ 错误率
   └─ 慢查询统计

4. 中间件指标
   ├─ 数据库连接池状态
   ├─ Redis 命中率
   └─ MQ 消息堆积
```

#### Prometheus 集成
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 12.3 告警机制

#### 告警规则（规划）
```
1. 系统告警
   ├─ CPU 使用率 > 80%
   ├─ 内存使用率 > 85%
   ├─ 磁盘使用率 > 90%
   └─ 接口可用性 < 99%

2. 业务告警
   ├─ 充电桩离线率 > 10%
   ├─ 订单支付失败率 > 5%
   ├─ 对账不一致数量 > 0
   └─ 协议通信超时 > 5次/分钟

3. 安全告警
   ├─ 登录失败次数 > 5次/分钟
   ├─ 非法Token访问
   └─ SQL异常
```

---

## 13. 部署架构

### 13.1 本地开发环境

```
docker-compose.local.yml

PostgreSQL (5432)
Redis (6379)
RabbitMQ (5672, 15672)
    ↓
Spring Boot 应用（8080+）
    ├─ evcs-gateway (8080)
    ├─ evcs-auth (8081)
    ├─ evcs-tenant (8082)
    ├─ evcs-station (8083)
    ├─ evcs-order (8084)
    └─ evcs-protocol (8085)
```

### 13.2 生产环境（规划）

```
                    [负载均衡 - Nginx/ALB]
                            ↓
        ┌───────────────────────────────────┐
        │   Kubernetes / Docker Swarm      │
        ├───────────────────────────────────┤
        │  evcs-gateway (3 replicas)       │
        │  evcs-auth (2 replicas)          │
        │  evcs-tenant (2 replicas)        │
        │  evcs-station (3 replicas)       │
        │  evcs-order (3 replicas)         │
        │  evcs-protocol (2 replicas)      │
        └───────────────────────────────────┘
                            ↓
        ┌───────────────────────────────────┐
        │   PostgreSQL 主从集群             │
        │   Redis Cluster / Sentinel       │
        │   RabbitMQ 集群                  │
        └───────────────────────────────────┘
                            ↓
        ┌───────────────────────────────────┐
        │   Prometheus + Grafana           │
        │   ELK / Loki (日志)              │
        └───────────────────────────────────┘
```

### 13.3 CI/CD 流程

```
代码提交 (Git)
    ↓
代码审查 (Pull Request)
    ↓
自动化测试 (JUnit + Integration Tests)
    ↓
代码质量检查 (SonarQube)
    ↓
构建镜像 (Gradle + Docker)
    ↓
推送镜像仓库 (Docker Registry / 阿里云)
    ↓
部署到测试环境
    ↓
冒烟测试
    ↓
部署到生产环境 (蓝绿/金丝雀)
    ↓
监控和告警
```

---

## 14. 架构优势与特点

### 14.1 核心优势

#### 1. 四层租户隔离（⭐⭐⭐⭐⭐）
- **数据库层**：行级隔离，外键约束
- **SQL层**：自动过滤，防止泄漏
- **服务层**：AOP切面，声明式权限
- **API层**：上下文管理，线程安全

**优势**：
- 多层防护，安全可靠
- 自动化程度高，开发友好
- 支持层级租户，灵活性强

#### 2. 声明式数据权限
```java
@DataScope(value = DataScopeType.TENANT_HIERARCHY)
public List<Station> getStations() { ... }
```

**优势**：
- 代码简洁，易于维护
- 权限逻辑集中管理
- 减少重复代码

#### 3. 分时电价计费（⭐⭐⭐⭐）
- 最多 96 段/天（15分钟粒度）
- 支持跨午夜时段
- 充电桩级别绑定
- 自动按时间段分摊电量

**优势**：
- 灵活的计费策略
- 精确的费用计算
- 支持复杂业务场景

#### 4. 微服务架构
- 服务解耦，职责清晰
- 独立部署，快速迭代
- 水平扩展，高可用

#### 5. 多协议支持
- OCPP 1.6（国际标准）
- 云快充（国内标准）
- 事件驱动，易扩展

#### 6. 幂等性设计
- 订单创建幂等（session_id）
- 订单完成幂等（session_id + end_time）
- 支付回调幂等（trade_id）

### 14.2 技术亮点

#### 1. PostgreSQL 特性运用
- **JSONB**：灵活存储设施信息、协议配置
- **数组类型**：存储支付方式、枪头类型
- **触发器**：自动更新时间戳、统计信息
- **地理位置**：基于经纬度的附近查询

#### 2. MyBatis Plus 多租户插件
- **开箱即用**：无需手动编写租户过滤逻辑
- **自动注入**：INSERT 语句自动添加 tenant_id
- **灵活配置**：支持忽略表列表

#### 3. ThreadLocal 上下文管理
- **线程安全**：每个请求独立上下文
- **自动清理**：请求结束后释放资源
- **易于使用**：全局访问租户信息

#### 4. AOP 切面编程
- **非侵入式**：业务代码无需关心权限
- **统一拦截**：集中处理权限检查
- **易于扩展**：新增权限类型简单

---

## 15. 技术债务与改进方向

### 15.1 当前限制

#### 1. 部分服务未实现
- ❌ evcs-payment（支付服务）- 规划中
- ❌ evcs-monitoring（监控服务）- 规划中
- ❌ evcs-integration（集成服务）- 规划中
- ⚠️ evcs-protocol - 仅有基础实现

#### 2. 协议服务待完善
- OCPP 协议仅有框架，未实现完整流程
- 云快充协议待对接真实设备
- 缺少设备连接管理（连接池、心跳检测）

#### 3. 缓存机制待增强
- 计费方案缓存仅为本地缓存
- 缺少多实例间的缓存同步（Redis Pub/Sub）
- 缓存更新策略需优化

### 15.2 待优化项

#### 性能优化
```
1. 数据库
   ├─ 读写分离（主从复制）
   ├─ 分库分表（按租户/时间）
   └─ 慢查询优化

2. 缓存
   ├─ Redis 集群部署
   ├─ 缓存预热策略
   └─ 缓存击穿/穿透防护

3. 消息队列
   ├─ 消息幂等性增强
   ├─ 死信队列处理
   └─ 消息延迟队列
```

#### 功能增强
```
1. 支付集成
   ├─ 支付宝/微信/网联对接
   ├─ 退款流程完善
   └─ 对账单拉取

2. 监控告警
   ├─ Grafana 仪表盘
   ├─ 告警规则配置
   └─ 钉钉/邮件通知

3. 第三方对接
   ├─ 开放平台API
   ├─ Webhook 推送
   └─ API文档自动生成
```

#### 安全增强
```
1. 接口限流
   ├─ 分布式限流（Redis）
   ├─ 熔断降级（Sentinel）
   └─ 黑名单机制

2. 数据加密
   ├─ 敏感字段加密（AES）
   ├─ 传输加密（HTTPS）
   └─ 数据库TDE

3. 审计日志
   ├─ 操作审计记录
   ├─ 数据变更追踪
   └─ 合规性报告
```

### 15.3 架构演进方向

#### 短期（3个月）
- ✅ 完善支付服务对接
- ✅ 增强协议服务（真实设备测试）
- ✅ 完善监控和告警
- ✅ 前端管理界面开发

#### 中期（6个月）
- ✅ 数据库读写分离
- ✅ Redis 集群部署
- ✅ 分布式事务支持（Seata）
- ✅ 性能压测和优化

#### 长期（12个月）
- ✅ 数据库分库分表
- ✅ 服务网格（Service Mesh）
- ✅ 边缘计算（充电桩侧）
- ✅ 大数据分析平台

---

## 16. 总结

### 16.1 架构特点总结

EVCS Manager 是一个**技术先进、架构清晰、安全可靠**的充电站管理平台：

1. **四层多租户隔离**：业界领先的租户隔离方案
2. **微服务架构**：高内聚、低耦合、易扩展
3. **分时电价计费**：灵活强大的计费引擎
4. **多协议支持**：兼容国内外主流协议
5. **现代化技术栈**：Java 21、Spring Boot 3.2、PostgreSQL 15

### 16.2 适用场景

- ✅ 中大型充电运营商
- ✅ SaaS 多租户平台
- ✅ 需要复杂计费规则的场景
- ✅ 需要多协议支持的场景
- ✅ 需要严格数据隔离的场景

### 16.3 核心价值

| 价值维度 | 说明 |
|---------|------|
| **数据安全** | 四层隔离确保数据不泄露 |
| **灵活计费** | 支持复杂的分时电价规则 |
| **高可扩展** | 微服务架构支持水平扩展 |
| **易于开发** | 声明式注解，代码简洁 |
| **运维友好** | 完善的监控和日志体系 |

---

## 附录

### A. 术语表

| 术语 | 说明 |
|------|------|
| **EVCS** | Electric Vehicle Charging Station（电动汽车充电站） |
| **TOU** | Time of Use（分时电价） |
| **OCPP** | Open Charge Point Protocol（开放充电桩协议） |
| **JWT** | JSON Web Token |
| **AOP** | Aspect Oriented Programming（面向切面编程） |
| **SaaS** | Software as a Service（软件即服务） |

### B. 参考文档

- [PRODUCT-REQUIREMENTS.md](./PRODUCT-REQUIREMENTS.md) - 产品需求文档
- [TECHNICAL-DESIGN.md](./TECHNICAL-DESIGN.md) - 技术方案设计
- [ROADMAP.md](./ROADMAP.md) - 路线图与计划
- [PROGRESS.md](./PROGRESS.md) - 进度与里程碑
- [README-TENANT-ISOLATION.md](../README-TENANT-ISOLATION.md) - 多租户隔离详解

### C. 联系方式

如有技术问题或建议，请通过以下方式联系：
- 提交 Issue
- 发送邮件
- 技术交流群

---

**文档结束**

> 本文档完整分析了 EVCS Manager 的架构设计，涵盖业务背景、技术架构、数据模型、业务流程、安全设计等各个方面。
> 
> **最后更新**: 2024年
