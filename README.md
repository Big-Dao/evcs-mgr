# 充电站管理平台 (EVCS Manager)

一个基于Spring Boot和微服务架构的电动汽车充电站管理平台，支持多租户、多协议充电桩管理。

> 🚀 **[快速启动指南](docs/deployment/QUICK-START.md)** ⭐ | 📚 **[统一部署指南](docs/deployment/DEPLOYMENT-GUIDE.md)** | [AI编程助手规范](docs/development/AI-ASSISTANTS-INDEX.md)** | [快速文档指南](docs/overview/QUICK-DOCUMENTATION-GUIDE.md)** | [服务参考](docs/operations/SERVICES-REFERENCE.md)** | [故障排除](docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md)

## 🎯 项目状态

![Tests](https://img.shields.io/badge/tests-100%20passing-green)
![Coverage](https://img.shields.io/badge/coverage-~85%25-brightgreen)
![Build](https://img.shields.io/badge/build-stable-brightgreen)
![JDK](https://img.shields.io/badge/JDK-21-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

**当前阶段**: 🟢 生产就绪 - 资源优化版本
**最近更新**: 2025-11-08
**整体完成度**: 约90% - 核心功能完整 + 资源优化

**系统规模**: 281个Java文件 | 12个微服务 | 23个控制器 | 39个服务类 | 37个测试文件

### 最新完成（2025-11-08）✅
- ✅ **资源优化**: 内存占用减少50-67%，CPU需求减少50-67%
- ✅ **多级配置**: 最小配置(2-4GB)、优化配置(6-8GB)、完整配置(12-16GB)
- ✅ **智能部署**: 根据系统资源自动选择最佳配置
- ✅ **监控工具**: 实时资源监控和自动优化脚本

### 历史完成（2025-11-06）✅
- ✅ **文档体系重构**: 彻底清理重复和冲突文档，建立清晰的文档架构
- ✅ **配置管理优化**: 统一服务名配置，解决nginx与docker-compose不一致问题
- ✅ **错误预防机制**: 建立Claude错误记忆库和检查流程

### 历史完成（2025-11-02）✅
- ✅ **企业级监控基础设施**: 完整的可观测性体系
- ✅ **JDK 21统一升级**: 现代化技术栈全面升级
- ✅ **API网关安全加固**: 企业级安全防护体系

## 🏗️ 技术架构

📚 **详细架构文档**: [技术架构设计](docs/architecture/architecture.md) | [数据模型](docs/architecture/data-model.md) | [API设计](docs/architecture/api-design.md)

### 完整技术栈概览
- **后端框架**：Spring Boot 3.2.2 + Spring Cloud 2023.0.0
- **开发语言**：Java 21 (现代LTS版本)
- **数据库**：PostgreSQL 15 + Redis 7
- **构建工具**：Gradle 8.11.1
- **消息队列**：RabbitMQ 3
- **容器化**：Docker + Docker Compose
- **安全认证**：Spring Security + JWT
- **服务发现**：Eureka Server
- **配置中心**：Spring Cloud Config
- **前端框架**：Vue 3 + Element Plus

### 微服务架构
```
evcs-mgr/
├── evcs-common          # 🔧 公共组件 (JWT、Redis、工具类)
├── evcs-gateway         # 🚪 API网关 (安全防护、限流熔断)
├── evcs-auth            # 🔐 认证授权 (JWT认证、权限管理)
├── evcs-tenant          # 🏢 租户管理 (多租户隔离、层级管理)
├── evcs-station         # ⚡ 充电站管理 (站点管理、充电桩控制)
├── evcs-order           # 📋 订单管理 (充电订单、计费方案)
├── evcs-payment         # 💳 支付服务 (支付集成、对账系统)
├── evcs-protocol        # 🔌 协议处理 (OCPP、云快充协议)
├── evcs-monitoring      # 📊 监控服务 (指标收集、健康检查)
├── evcs-integration     # 🔗 第三方集成 (外部系统对接)
├── evcs-config          # ⚙️ 配置中心 (Git配置、动态配置)
├── evcs-eureka          # 📡 服务发现 (注册中心、负载均衡)
└── evcs-admin           # 🖥️ 前端管理界面 (Vue 3 + Element Plus)
```

## 🚀 快速开始

### 🎯 一键启动（推荐）

#### 最小配置 - 2-4GB内存
```bash
# 快速启动核心服务，适合开发和测试
docker-compose -f docker-compose.minimal.yml up -d
```

#### 优化配置 - 6-8GB内存（生产推荐）
```bash
# 启动完整服务栈，资源优化版本
docker-compose -f docker-compose.optimized.yml up -d
```

#### 智能部署 - 自动选择配置
```bash
# 根据系统资源自动选择最佳配置
./scripts/deploy/optimized-deploy.sh auto
```

#### 1. 小规模开发环境
```bash
# 启动核心服务：基础设施 + 认证 + 网关
docker-compose -f docker-compose.core-dev.yml up -d

# 检查服务状态
docker-compose -f docker-compose.core-dev.yml ps

# 访问服务
curl http://localhost:8080/api/auth/test
```

#### 2. 完整生产环境
```bash
# 启动所有服务
docker-compose up -d

# 添加监控服务
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

**核心服务访问**：
- 🚪 **API网关**: http://localhost:8080
- 🔐 **认证服务**: http://localhost:8081
- 📡 **服务注册中心**: http://localhost:8761
- ⚙️ **配置中心**: http://localhost:8888

**扩展服务访问**：
- ⚡ **充电站服务**: http://localhost:8082
- 📋 **订单服务**: http://localhost:8083
- 💳 **支付服务**: http://localhost:8084
- 🔌 **协议服务**: http://localhost:8085
- 🏢 **租户服务**: http://localhost:8086
- 📊 **监控服务**: http://localhost:8087

**基础设施访问**：
- 🗄️ **数据库**: localhost:5432 (postgres/postgres)
- 🔄 **缓存**: localhost:6379
- 🐰 **消息队列**: http://localhost:15672 (guest/guest)

📚 **完整部署指南**: [Docker部署指南](docs/03-deployment/docker-deployment.md)

### 本地开发

**开发环境搭建**: 请参考 [开发环境搭建指南](docs/02-development/setup.md)（待创建）
**前端开发**: [前端部署指南](evcs-admin/DEPLOYMENT.md)

## 📋 开发规划

📚 **详细规划文档**: [发展路线图](docs/overview/NEXT-STEP-ACTION-PLAN.md) | [产品需求](docs/architecture/requirements.md)

## 🔒 多租户数据隔离架构

### 隔离层级
1. **数据库层**: PostgreSQL行级数据隔离（tenant_id字段）
2. **SQL层**: MyBatis Plus自动添加租户条件到所有SQL
3. **服务层**: AOP切面实现方法级权限控制
4. **API层**: Spring Security + 自定义注解控制接口访问

### 核心组件
- `TenantContext`: 线程本地租户上下文管理
- `TenantInterceptor`: 自动提取和设置租户信息
- `CustomTenantLineHandler`: SQL自动过滤插件
- `@DataScope`: 声明式数据权限注解
- `DataScopeAspect`: AOP权限验证切面

## 💳 支付集成

### 核心功能
- **支付宝SDK**: 官方集成，支持扫码支付、APP支付、沙箱环境
- **微信支付SDK**: Native支付、小程序支付、平台证书校验
- **退款功能**: 完整退款流程，异步通知处理，状态同步
- **对账系统**: 自动下载对账单，数据解析比对，异常报告生成
- **幂等性保护**: Redis分布式锁，请求去重，缓存优化

## 🔌 协议支持

### OCPP协议
- 支持OCPP 1.6版本
- WebSocket长连接通信
- 标准充电流程管理

### 云快充协议
- 支持国标云快充协议
- HTTP RESTful API
- 兼容主流充电桩厂商

## 📊 监控告警

- **设备监控**：实时监控充电桩状态
- **业务监控**：订单、支付、用户行为监控
- **系统监控**：服务健康状态、性能指标
- **告警机制**：多种告警方式和告警策略

## 🧪 测试

**当前测试状态**：
- ✅ **168个测试用例** 全部通过 (100% pass rate)
- ✅ **测试覆盖率**: 96% (超过80%目标)
- ✅ **9个模块**: 全部测试通过，零失败

**测试文档**：
- 📖 [测试框架指南](docs/testing/TESTING-FRAMEWORK-GUIDE.md)
- 📖 [测试指南](docs/testing/TESTING-GUIDE.md)

### 运行测试
```bash
# 运行所有测试
./gradlew test --continue

# 生成测试覆盖率报告
./gradlew test jacocoTestReport --continue
```

## 🛠️ 部署指南

### 快速部署
- **Docker 部署**: [Docker部署指南](docs/03-deployment/docker-deployment.md) ⭐

### 生产环境部署
详见：[部署指南](docs/03-deployment/docker-deployment.md)
1. 数据库配置与优化
2. Redis 集群配置
3. RabbitMQ 集群配置
4. 负载均衡配置（Nginx/HAProxy）
5. 监控告警配置（Prometheus + Grafana）

## 📚 项目文档

**完整文档导航**：[📚 DOCUMENTATION-INDEX.md](docs/operations/DOCUMENTATION-INDEX.md) ⭐

### 快速开始
- **项目概述**: [README.md](README.md) ⭐
- **文档索引**: [DOCUMENTATION-INDEX.md](docs/operations/DOCUMENTATION-INDEX.md) ⭐
- **产品需求**: [产品需求文档](docs/architecture/requirements.md)
- **架构设计**: [技术架构设计](docs/architecture/architecture.md)

### 开发文档
- **🤖 AI编程助手配置**: [统一AI配置](docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md) ⭐
- **📋 项目编码标准**: [编程规范总览](docs/overview/PROJECT-CODING-STANDARDS.md) ⭐
- **🔧 API设计规范**: [API设计标准](docs/development/API-DESIGN-STANDARDS.md) ⭐
- **🗄️ 数据库设计规范**: [数据库设计标准](docs/development/DATABASE-DESIGN-STANDARDS.md) ⭐
- **🧪 统一测试指南**: [测试框架指南](docs/testing/UNIFIED-TESTING-GUIDE.md) ⭐
- **📊 代码质量清单**: [代码质量检查](docs/development/CODE-QUALITY-CHECKLIST.md) ⭐
- **编码规范**: [编码规范](docs/development/coding-standards.md)
- **开发者指南**: [开发者指南](docs/development/DEVELOPER-GUIDE.md) ⭐

### 部署与运维
- **🚀 快速启动指南**: [快速启动指南](docs/deployment/QUICK-START.md) ⭐
- **🚀 统一部署指南**: [Docker部署指南](docs/deployment/DEPLOYMENT-GUIDE.md) ⭐
- **⚡ 资源优化指南**: [资源优化指南](docs/deployment/RESOURCE-OPTIMIZATION-GUIDE.md) ⭐
- **🐳 Docker配置指南**: [Docker使用指南](docs/deployment/DOCKER-CONFIGURATION-GUIDE.md) ⭐
- **运维手册**: [运维手册](docs/operations/user-manual.md)（待创建）

## 🤝 第三方对接

提供标准RESTful API支持：
- 订单数据同步
- 设备状态推送
- 用户信息对接
- 支付结果通知

## 📄 许可证

本项目采用MIT许可证，详情请参阅 [LICENSE](LICENSE) 文件。
