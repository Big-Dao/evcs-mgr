# EVCS Manager 服务详细参考

> **版本**: v1.2 | **最后更新**: 2026-01-12 | **维护者**: 运维负责人 | **状态**: 已发布

## 1. 权威来源

1.1. 服务主标识与端口

- 权威来源：[docs/architecture/SERVICE-IDENTIFIERS-AND-PORTS.md](../architecture/SERVICE-IDENTIFIERS-AND-PORTS.md)
- 本文定位：服务功能说明与运维侧检查要点；不重复维护服务名映射表与端口表

1.2. 本地编排

- Docker Compose 权威来源：[docker-compose.yml](../../docker-compose.yml)

---

## 2. 服务概览

本文档说明 EVCS Manager 平台中各服务的功能与运维要点。

## 3. 核心微服务架构

### 3.1 公共服务

#### 3.1.1 evcs-common (公共组件库)
- **功能**: 提供所有服务共享的公共组件
- **主要内容**:
  - JWT 认证工具
  - Redis 缓存配置和工具类
  - MyBatis Plus配置
  - 多租户数据隔离组件
  - 通用工具类和常量
- **部署方式**: 作为依赖库被其他服务引用
- **重要性**: 核心基础

### 3.2 网关

#### 3.2.1 evcs-gateway (API网关) - 端口 8080
- **功能**: 统一API入口，路由转发，安全防护
- **核心特性**:
  - JWT认证验证
  - 服务路由和负载均衡
  - 限流和熔断保护
  - CORS跨域处理
  - 请求日志和监控
- **配置文件**: `config-repo/evcs-gateway-local.yml`
- **健康检查**: http://localhost:8080/actuator/health

### 3.3 认证授权

#### 3.3.1 evcs-auth (认证授权服务) - 端口 8081
- **功能**: 用户认证、权限管理、JWT签发
- **核心功能**:
  - 用户登录/登出
  - JWT Token 生成和验签
  - 角色权限管理
  - 多租户认证
- **登录入口**: `POST /api/auth/login`
  - 请求体：`identifier`（手机号或邮箱）、`password`
  - 认证成功后，服务端根据 `identifier` 解析所属租户，并在 JWT 中写入 `tenantId`；前端无需手工传递租户信息。
- **下游请求头**: 所有业务请求必须携带 `Authorization: Bearer <token>`、`X-Tenant-Id`、`X-User-Id`
- **API路径**: `/api/auth/**`
- **数据表**: `sys_user`, `sys_role`, `sys_user_role`, `sys_permission`
- **健康检查**: http://localhost:8081/actuator/health

### 3.4 配置管理

#### 3.4.1 evcs-config (配置中心) - 端口 8888
- **功能**: 集中配置管理，支持动态配置更新
- **配置存储**: Git仓库 (`config-repo/`)
- **核心配置文件**:
  - `evcs-gateway-local.yml` - 网关配置
  - `evcs-auth-local.yml` - 认证配置
  - `evcs-tenant-local.yml` - 租户配置
  - 其他服务配置...
- **管理界面**: http://localhost:8888
- **健康检查**: http://localhost:8888/actuator/health

#### 3.4.2 evcs-eureka (服务注册中心) - 端口 8761
- **功能**: 微服务注册与发现
- **管理界面**: http://localhost:8761
- **注册服务**: 所有业务微服务
- **健康检查**: http://localhost:8761/ （返回 Eureka 仪表板）

### 3.5 业务服务

#### 3.5.1 evcs-tenant (租户管理服务) - 端口 8086
- **功能**: 多租户管理，层级组织架构
- **核心功能**:
  - 租户创建和管理
  - 层级权限控制
  - 数据隔离策略
  - 租户配置管理
- **API路径**: `/api/tenant/**`
- **数据隔离**: 四层数据隔离机制
- **健康检查**: http://localhost:8086/actuator/health

#### 3.5.2 evcs-station (充电站管理服务) - 端口 8082
- **功能**: 充电站和充电桩管理
- **核心功能**:
  - 充电站信息管理
  - 充电桩状态监控
  - WebSocket实时通信
  - 设备远程控制
- **API路径**: `/api/station/**`
- **协议支持**: OCPP, 云快充
- **健康检查**: http://localhost:8082/actuator/health

#### 3.5.3 evcs-order (订单管理服务) - 端口 8083
- **功能**: 充电订单和计费管理
- **核心功能**:
  - 订单生命周期管理
  - 计费方案配置
  - 时间分段计费
  - 订单统计分析
- **订单状态**: `0` 已创建、`1` 已完成、`2` 已取消、`10` 待支付、`11` 已支付、`12` 退款中、`13` 已退款
- **API路径**: `/api/order/**`
- **鉴权要求**: 与网关一致，需要 JWT 与租户上下文请求头
- **数据表**: `charging_order`, `billing_plan`, `billing_plan_segment`
- **集成服务**: 支付服务, 站点服务
- **健康检查**: http://localhost:8083/actuator/health

#### 3.5.4 evcs-payment (支付服务) - 端口 8084
- **功能**: 支付集成和财务管理
- **核心功能**:
  - 支付宝/微信支付集成
  - 支付状态跟踪
  - 退款处理
  - 对账系统
  - 幂等性保护
- **回调地址**:

  | 类型 | 支付回调 | 退款回调 |
  | ---- | -------- | -------- |
  | 支付宝 | `/api/payment/callback/alipay` | `/api/payment/callback/alipay/refund` |
  | 微信   | `/api/payment/callback/wechat` | `/api/payment/callback/wechat/refund` |

- **配置要点**:
- 微信退款回调解密需配置 `evcs.payment.wechat.api-v2-key`（使用 `req_info` 进行 AES 解密与签名校验）
- 回调地址通过 API 网关暴露，生产环境需在渠道侧配置公网可达域名
- 网关需保留 `Wechatpay-Timestamp/Nonce/Signature/Serial` 请求头并透传至支付服务，以便回调验签与解密
- 微信渠道真实接入需显式启用 `evcs.payment.wechat.enabled=true`，并补齐以下属性（若缺少则自动退回模拟模式）
  - `evcs.payment.wechat.app-id`（JSAPI/小程序）与 `evcs.payment.wechat.mchid`
  - `evcs.payment.wechat.merchant-serial-number`、`evcs.payment.wechat.api-v3-key`
  - `evcs.payment.wechat.private-key` 或 `evcs.payment.wechat.private-key-path`（PKCS8 PEM）
  - 回调地址 `evcs.payment.wechat.notify-url` / 退款回调 `evcs.payment.wechat.refund-notify-url`
- 前端/调用方在发起微信支付时需按支付方式补齐 `wechatOptions`:
  - JSAPI：`appId` 与 `openId` 必填
  - Native：推荐提供 `payerClientIp`，用于风控记录
  - 可选透传 `attach`、`goodsTag`，会在回调中原样返回
- **API路径**: `/api/payment/**`
- **消息队列**: RabbitMQ异步通知
- **健康检查**: http://localhost:8084/actuator/health

#### 3.5.5 evcs-protocol (协议处理服务) - 端口 8085
- **功能**: 充电桩协议对接
- **支持的协议**:
  - OCPP 1.6协议
  - 云快充协议
  - 自定义协议扩展
- **通信方式**: WebSocket, HTTP
- **API路径**: `/api/protocol/**`
- **健康检查**: http://localhost:8085/actuator/health

#### 3.5.6 evcs-integration (第三方集成服务，预留模块)
- **功能**: 外部系统集成接口（预留）
- **计划集成**:
  - 地图服务API
  - 支付渠道扩展
  - 第三方充电平台
  - 数据统计分析
- **状态**: 当前仅保留模块与依赖配置，未提供可运行的 Spring Boot 启动入口，未在 `docker-compose.yml` 中启用

### 3.6 监控运维

#### 3.6.1 evcs-monitoring (监控服务) - 端口 8087
- **功能**: 系统监控和指标收集
- **监控内容**:
  - 应用性能指标
  - 业务指标统计
  - 系统资源监控
  - 健康检查聚合
- **集成工具**: Prometheus, Grafana
- **API路径**: `/api/monitoring/**`
- **健康检查**: http://localhost:8087/actuator/health

### 3.7 前端界面

#### 3.7.1 evcs-admin (前端管理界面) - 端口 80/3000
- **技术栈**: Vue 3 + Element Plus
- **功能模块**:
  - 用户和权限管理
  - 租户和层级管理
  - 充电站监控
  - 订单和支付管理
  - 系统配置
- **开发端口**: 3000（开发模式）
- **生产端口**: 80 (Docker部署)
- **部署文档**: [evcs-admin/DEPLOYMENT.md](../deployment/DEPLOYMENT-GUIDE.md)
- **健康检查**: http://localhost:3000/ （前端构建可使用自定义心跳接口）

#### 3.7.2 evcs-mobile (C 端移动端)
- **技术栈**: uni-app + Vue 3
- **运行方式**: 通过 `evcs-mobile/package.json` 中的 `dev:*` 脚本启动（小程序/App/H5）
- **部署方式**: 不纳入 `docker-compose.yml`，按目标平台独立构建与发布

## 4. 基础设施服务

### 4.1 数据存储
- **PostgreSQL** (端口 5432) - 主数据库
- **Redis** (端口 6379) - 缓存和会话存储
- **健康检查**:
  - PostgreSQL: `pg_isready -U postgres`
  - Redis: `redis-cli ping`

### 4.2 消息队列
- **RabbitMQ** (端口 5672/15672) - 异步消息处理
- **管理界面**: http://localhost:15672 (guest/guest)
- **健康检查**: http://localhost:15672/api/healthchecks/node

## 5. 服务间通信

### 5.1 服务发现
- **注册中心**: Eureka (8761)
- **服务注册**: 所有微服务启动时自动注册
- **健康检查**: 定期服务健康状态检查

### 5.2 配置管理
- **配置中心**: Spring Cloud Config (8888)
- **配置存储**: Git仓库 (`config-repo/`)
- **动态更新**: 支持配置热更新

### 5.3 消息通信
- **同步调用**: HTTP RESTful API
- **异步通信**: RabbitMQ消息队列
- **实时通信**: WebSocket (协议服务)

## 6. 开发和部署

### 6.1 本地开发
1. 启动基础设施: PostgreSQL, Redis, RabbitMQ
2. 启动核心服务: Eureka, Config
3. 启动业务服务: Auth, Tenant, Station
4. 启动前端: `evcs-admin`（Vite）/ `evcs-mobile`（uni-app）

### 6.2 Docker部署
- **编排文件**: `docker-compose.yml`
- **一键部署**: `docker compose up -d`
- **服务监控**: Docker 健康检查
- **日志管理**: 集中日志收集

### 6.3 端口使用总结
| 类型 | 端口范围 | 说明 |
|------|----------|------|
| 基础设施 | 5432, 6379, 5672, 15672 | 数据库、缓存、消息队列 |
| 微服务核心 | 8761, 8888, 8080 | 注册中心、配置中心、网关 |
| 业务服务 | 8081-8086 | 认证、业务、协议服务 |
| 监控服务 | 8087 | 系统监控 |
| 前端界面 | 80, 3000 | 管理界面 |

## 7. 演示数据与测试账号

- **管理员账号**: `admin@tenant1` / `password`（初始化数据，详见 `sql/init.sql`）
- **演示数据脚本**: `sql/demo-order-data.sql`
  - macOS/Linux: `cat sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr`
  - Windows (PowerShell): `Get-Content sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr`
  - 内容: 默认计费方案 + 5 条订单样本，覆盖创建/待支付、已支付、退款中等状态
- **前端访问**: 登录后访问 `http://localhost:3000/orders` 可查看真实订单数据（若未导入则回退到模拟数据）

## 8. 故障排查

### 8.1 常见问题
1. **服务启动失败**: 检查端口占用和依赖服务
2. **配置不生效**: 检查 Config Server 连接
3. **服务发现失败**: 检查 Eureka 注册状态
4. **数据库连接失败**: 检查数据库服务与网络

### 8.2 健康检查
每个服务都提供Spring Boot Actuator健康检查端点：
- 路径: `/actuator/health`
- 方法: GET
- 返回: 服务健康状态JSON

### 8.3 日志查看
```bash
# 查看特定服务日志
docker-compose logs [service-name]

# 实时跟踪日志
docker-compose logs -f [service-name]
```

---

**维护**: 定期更新服务状态和配置信息
**反馈**: 发现问题请提交Issue或PR
