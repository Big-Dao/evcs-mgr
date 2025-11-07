# 技术方案设计

> **最后更新**: 2025-11-01
> **维护者**: EVCS Architecture Team
> **状态**: 开发中 - 核心架构已完成，部分业务功能待实现

## 1. 架构总览
- **技术栈**: Spring Boot 3.2、Java 21、Gradle 8.5
- **微服务模块**: auth/gateway/common/station/order/payment/protocol/tenant/monitoring
- **多租户四层隔离**: 
  1. **DB 行级**: PostgreSQL `tenant_id` 字段
  2. **SQL 层**: MyBatis Plus TenantLineInnerInterceptor 自动注入 `WHERE tenant_id = ?`
  3. **服务层**: `@DataScope` 注解 + DataScopeAspect 切面
  4. **API 层**: Gateway JWT 验证 + Header 传递租户上下文

### 1.1 认证与授权架构

#### 认证流程

```
用户请求 → Gateway (JwtAuthGlobalFilter) → 后端服务 (TenantInterceptor) → 业务逻辑
         ↓ JWT 验证                      ↓ Header 读取
         ↓ 提取 tenantId/userId          ↓ 设置 TenantContext
         ↓ 添加 X-Tenant-Id/X-User-Id
```

**1. API Gateway (evcs-gateway)**:
- **JwtAuthGlobalFilter**: 验证 Authorization: Bearer {token}
- 提取 JWT Claims: `tenantId`, `userId`, `username`
- 添加到请求头:
  - `X-Tenant-Id`: 租户 ID
  - `X-User-Id`: 用户 ID
- 路由到后端服务

**2. 后端服务层**:
- **TenantInterceptor**: 
  - 从请求头读取 `X-Tenant-Id`/`X-User-Id`
  - 设置 `TenantContext` (ThreadLocal)
  - 优先级: Header → JWT → Request Param
- **安全配置**:
  - 所有服务（除 evcs-auth）排除 `SecurityAutoConfiguration`
  - 使用 `spring-security-core` 支持 `@PreAuthorize` 注解
  - 不启用 Spring Security 过滤器链

**3. 数据层隔离**:
- **MyBatis Plus**:
  - `TenantLineInnerInterceptor`: 自动注入 `WHERE tenant_id = ?`
  - `CustomTenantLineHandler`: 定义忽略表（IGNORE_TABLES）
  - `CustomMetaObjectHandler`: 自动填充 `tenant_id`, `created_by`, `updated_by`
- **基础实体**: 所有实体继承 `BaseEntity`（含 tenant_id、审计字段、软删除）

#### 配置管理架构

详见 [Spring Cloud Config 规范](SPRING-CLOUD-CONFIG-CONVENTIONS.md)

**配置层次**:
```
application-{profile}.yml (全局共享)
  ↓ 覆盖
{service-name}-{profile}.yml (服务特定)
  ↓ 覆盖
application.yml (Classpath 默认)
```

**全局配置** (`config-repo/application-local.yml`):
- JWT 配置: `jwt.secret`, `jwt.expire`
- Eureka Client 配置
- Actuator & Monitoring 配置
- 日志配置

**服务配置** (`config-repo/{service}-local.yml`):
- 数据库连接
- Redis 配置
- RabbitMQ 配置
- 服务特定功能开关

## 2. 分时电价（TOU）设计
### 2.1 数据模型
- BillingPlan（计费计划）：站点内最多 16 个，含生效日期区间、优先级
- BillingPlanSegment（计划分段）：每日最多 96 段，字段：start_time、end_time、electricity_price、service_price
- Charger 关联：charger.plan_id 首选；否则站点默认计划；否则回落 legacy 费率

### 2.2 选择与优先级
1) 计费调用显式传入 planId 则优先
2) 否则按充电桩绑定的 planId
3) 否则站点默认计划（按日期区间、优先级筛选 1 条）
4) 否则回落旧费率配置

### 2.3 校验与边界
- 分段最多 96，分钟粒度
- 段内价格 >= 0；时间段可跨午夜
- 不重叠校验；可选“是否覆盖全天”校验
- 站点下计划数上限 16

### 2.4 计费算法（概要）
- 输入：开始/结束时间、总电量 kWh、计划分段集合
- 将充电时段在自然日维度切分，与每日分段做时间交集
- 按时间占比或按能量分布计价（当前实现按时长比例在段内分摊能量，计算 price = energy_in_segment*(elec+svc)）
- 避免负值，边界能量归零

### 2.5 多租户与权限
- Controller 方法使用 @DataScope（TENANT 或 HIERARCHY）
- MyBatis Plus 自动注入 WHERE tenant_id = ?
- API 层通过 JWT 提取租户上下文

## 3. 订单闭环与对账
- 协议事件入口：/orders/start、/orders/stop 幂等
- 结束事件触发计费（调用 TOU 计算）
- ReconciliationService：每日离线一致性校验，统计无效时间段/负金额/缺失结束时间

## 4. 可观测性
- evcs-order 启用 actuator 与 micrometer-registry-prometheus，开放 /actuator/prometheus
- 后续在 Grafana 侧配置仪表盘

## 5. 可扩展点
- 计划缓存广播：Redis Topic（当前留空实现，用本地缓存，后续切换广播）
- 协议接入：OCPP/云快充事件对齐，使用消息队列做削峰
- 支付通道：签名校验、退款、对账单拉取

