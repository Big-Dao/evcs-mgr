# EVCS Manager 服务详细参考

> **更新日期**: 2025-11-06 | **状态**: 完整

## 📋 服务概览

本文档详细说明EVCS Manager平台中所有微服务的功能、端口和配置信息。

## 🏗️ 核心微服务架构

### 🔧 公共服务层

#### evcs-common (公共组件库)
- **功能**: 提供所有服务共享的公共组件
- **主要内容**:
  - JWT认证工具类
  - Redis缓存配置和工具
  - MyBatis Plus配置
  - 多租户数据隔离组件
  - 通用工具类和常量
- **部署方式**: 作为依赖库被其他服务引用
- **重要性**: ⭐⭐⭐⭐⭐ (核心基础)

### 🚪 网关层

#### evcs-gateway (API网关) - 端口 8080
- **功能**: 统一API入口，路由转发，安全防护
- **核心特性**:
  - JWT认证验证
  - 服务路由和负载均衡
  - 限流和熔断保护
  - CORS跨域处理
  - 请求日志和监控
- **配置文件**: `config-repo/evcs-gateway-local.yml`
- **健康检查**: http://localhost:8080/actuator/health

#### evcs-gateway-backup (网关备份)
- **功能**: 网关服务的备用实例
- **状态**: 备用配置，用于高可用场景
- **部署方式**: 按需启用

### 🔐 认证授权层

#### evcs-auth (认证授权服务) - 端口 8081
- **功能**: 用户认证、权限管理、JWT签发
- **核心功能**:
  - 用户登录/登出
  - JWT Token生成和验证
  - 角色权限管理
  - 多租户认证
- **登录入口**: `POST /api/auth/login`（Body需包含 `username`、`password`、`tenantId`）
- **下游请求头**: 所有业务请求必须携带 `Authorization: Bearer <token>`、`X-Tenant-Id`、`X-User-Id`
- **API路径**: `/api/auth/**`
- **数据表**: `sys_user`, `sys_role`, `sys_user_role`, `sys_permission`
- **健康检查**: http://localhost:8081/actuator/health

### ⚙️ 配置管理层

#### evcs-config (配置中心) - 端口 8888
- **功能**: 集中配置管理，动态配置更新
- **配置存储**: Git仓库 (`config-repo/`)
- **核心配置文件**:
  - `evcs-gateway-local.yml` - 网关配置
  - `evcs-auth-local.yml` - 认证配置
  - `evcs-tenant-local.yml` - 租户配置
  - 其他服务配置...
- **管理界面**: http://localhost:8888
- **健康检查**: http://localhost:8888/actuator/health

#### evcs-eureka (服务注册中心) - 端口 8761
- **功能**: 微服务注册与发现
- **管理界面**: http://localhost:8761
- **注册服务**: 所有业务微服务
- **健康检查**: http://localhost:8761/ （返回Eureka仪表板）

### 🏢 业务服务层

#### evcs-tenant (租户管理服务) - 端口 8086
- **功能**: 多租户管理，层级组织架构
- **核心功能**:
  - 租户创建和管理
  - 层级权限控制
  - 数据隔离策略
  - 租户配置管理
- **API路径**: `/api/tenant/**`
- **数据隔离**: 四层数据隔离机制
- **健康检查**: http://localhost:8086/actuator/health

#### evcs-station (充电站管理服务) - 端口 8082
- **功能**: 充电站和充电桩管理
- **核心功能**:
  - 充电站信息管理
  - 充电桩状态监控
  - WebSocket实时通信
  - 设备远程控制
- **API路径**: `/api/station/**`
- **协议支持**: OCPP, 云快充
- **健康检查**: http://localhost:8082/actuator/health

#### evcs-order (订单管理服务) - 端口 8083
- **功能**: 充电订单和计费管理
- **核心功能**:
  - 订单生命周期管理
  - 计费方案配置
  - 时间分段计费
  - 订单统计分析
- **订单状态**: `0` 已创建、`1` 已完成、`2` 已取消、`10` 待支付、`11` 已支付、`12` 退款中、`13` 已退款
- **API路径**: `/api/order/**`
- **鉴权要求**: 与网关一致，需要JWT与租户上下文请求头
- **数据表**: `charging_order`, `billing_plan`, `billing_plan_segment`
- **集成服务**: 支付服务, 站点服务
- **健康检查**: http://localhost:8083/actuator/health

#### evcs-payment (支付服务) - 端口 8084
- **功能**: 支付集成和财务管理
- **核心功能**:
  - 支付宝/微信支付集成
  - 支付状态跟踪
  - 退款处理
  - 对账系统
  - 幂等性保护
- **API路径**: `/api/payment/**`
- **消息队列**: RabbitMQ异步通知
- **健康检查**: http://localhost:8084/actuator/health

#### evcs-protocol (协议处理服务) - 端口 8085
- **功能**: 充电桩协议对接
- **支持的协议**:
  - OCPP 1.6协议
  - 云快充协议
  - 自定义协议扩展
- **通信方式**: WebSocket, HTTP
- **API路径**: `/api/protocol/**`
- **健康检查**: http://localhost:8085/actuator/health

#### evcs-integration (第三方集成服务) - 开发中
- **功能**: 外部系统集成接口
- **计划集成**:
  - 地图服务API
  - 支付渠道扩展
  - 第三方充电平台
  - 数据统计分析
- **状态**: 开发中，按需启用

### 📊 监控运维层

#### evcs-monitoring (监控服务) - 端口 8087
- **功能**: 系统监控和指标收集
- **监控内容**:
  - 应用性能指标
  - 业务指标统计
  - 系统资源监控
  - 健康检查聚合
- **集成工具**: Prometheus, Grafana
- **API路径**: `/api/monitoring/**`
- **健康检查**: http://localhost:8087/actuator/health

### 🖥️ 前端界面

#### evcs-admin (前端管理界面) - 端口 80/3000
- **技术栈**: Vue 3 + Element Plus
- **功能模块**:
  - 用户和权限管理
  - 租户和层级管理
  - 充电站监控
  - 订单和支付管理
  - 系统配置
- **开发端口**: 3000 (开发模式)
- **生产端口**: 80 (Docker部署)
- **部署文档**: [evcs-admin/DEPLOYMENT.md](evcs-admin/DEPLOYMENT.md)
- **健康检查**: http://localhost:3000/ （前端构建可使用自定义心跳接口）

## 🗄️ 基础设施服务

### 数据存储
- **PostgreSQL** (端口 5432) - 主数据库
- **Redis** (端口 6379) - 缓存和会话存储
- **健康检查**:
  - PostgreSQL: `pg_isready -U postgres`
  - Redis: `redis-cli ping`

### 消息队列
- **RabbitMQ** (端口 5672/15672) - 异步消息处理
- **管理界面**: http://localhost:15672 (guest/guest)
- **健康检查**: http://localhost:15672/api/healthchecks/node

## 📡 服务间通信

### 服务发现
- **注册中心**: Eureka (8761)
- **服务注册**: 所有微服务启动时自动注册
- **健康检查**: 定期服务健康状态检查

### 配置管理
- **配置中心**: Spring Cloud Config (8888)
- **配置存储**: Git仓库 (`config-repo/`)
- **动态更新**: 支持配置热更新

### 消息通信
- **同步调用**: HTTP RESTful API
- **异步通信**: RabbitMQ消息队列
- **实时通信**: WebSocket (协议服务)

## 🔧 开发和部署

### 本地开发
1. 启动基础设施: PostgreSQL, Redis, RabbitMQ
2. 启动核心服务: Eureka, Config
3. 启动业务服务: Auth, Tenant, Station等
4. 启动前端: Vue开发服务器

### Docker部署
- **编排文件**: `docker-compose.local-images.yml`
- **一键部署**: `docker-compose up -d`
- **服务监控**: Docker健康检查
- **日志管理**: 集中日志收集

### 端口使用总结
| 类型 | 端口范围 | 说明 |
|------|----------|------|
| 基础设施 | 5432, 6379, 5672, 15672 | 数据库、缓存、消息队列 |
| 微服务核心 | 8761, 8888, 8080 | 注册中心、配置中心、网关 |
| 业务服务 | 8081-8086 | 认证、业务、协议服务 |
| 监控服务 | 9090 | 系统监控 |
| 前端界面 | 80, 3000 | 管理界面 |

## 📦 演示数据与测试账号

- **平台租户**: `PLATFORM-001`（tenant_id = `1001`）
- **管理员账号**: `admin.east` / `password`
- **演示数据脚本**: `sql/demo-order-data.sql`
  - macOS/Linux: `cat sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr`
  - Windows (PowerShell): `Get-Content sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr`
  - 内容: 默认计费方案及 5 条订单样本，覆盖创建/待支付/已支付/退款中等状态
- **前端访问**: 登录后 `http://localhost:3000/orders` 可查看真实订单数据（若未导入则回退到模拟数据）

## 🚨 故障排查

### 常见问题
1. **服务启动失败**: 检查端口占用和依赖服务
2. **配置不生效**: 检查Config Server连接
3. **服务发现失败**: 检查Eureka注册状态
4. **数据库连接失败**: 检查数据库服务和网络

### 健康检查
每个服务都提供Spring Boot Actuator健康检查端点：
- 路径: `/actuator/health`
- 方法: GET
- 返回: 服务健康状态JSON

### 日志查看
```bash
# 查看特定服务日志
docker-compose logs [service-name]

# 实时跟踪日志
docker-compose logs -f [service-name]
```

---

**维护**: 定期更新服务状态和配置信息
**反馈**: 发现问题请提交Issue或PR
