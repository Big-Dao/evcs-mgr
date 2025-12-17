# 充电站管理平台 (EVCS Manager)

一个基于Spring Boot和微服务架构的电动汽车充电站管理平台，支持多租户、多协议充电桩管理。

> 🚀 **[快速启动指南](docs/deployment/QUICK-START.md)** ⭐ | 📚 **[统一部署指南](docs/deployment/DEPLOYMENT-GUIDE.md)** | **[AI编程助手规范](docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md)** | **[快速文档指南](docs/overview/QUICK-DOCUMENTATION-GUIDE.md)** | **[服务参考](docs/operations/SERVICES-REFERENCE.md)** | **[故障排除](docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md)**

## 🎯 项目状态

![Tests](https://img.shields.io/badge/tests-100%20passing-green)
![Coverage](https://img.shields.io/badge/coverage-~85%25-brightgreen)
![Build](https://img.shields.io/badge/build-stable-brightgreen)
![JDK](https://img.shields.io/badge/JDK-21-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

**当前阶段**: 🟢 生产就绪 - 资源优化版本
**最近更新**: 2025-12-14
**整体完成度**: 约90% - 核心功能完整 + 资源优化

**系统规模**: 281个Java文件 | 12个微服务 | 23个控制器 | 39个服务类 | 37个测试文件

### 最新完成（2025-12-14）✅

- ✅ **工具链升级**: Gradle 8.11.1, Node.js 20, Spring Boot 3.2.12
- ✅ **构建优化**: 提升Gradle构建内存至4GB，优化国内镜像配置
- ✅ **前端修复**: 修正前端依赖版本，确保构建稳定性

### 历史完成（2025-11-08）✅

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

- **后端框架**：Spring Boot 3.2.12 + Spring Cloud 2023.0.6
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

```text
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

部署文档建议从统一索引进入，避免在多个入口重复维护同一套命令：

- 文档总索引：[docs/DOCUMENTATION-INDEX.md](docs/DOCUMENTATION-INDEX.md)
- 本地最少步骤启动：[docs/deployment/QUICK-START.md](docs/deployment/QUICK-START.md)
- 统一部署指南（本地/测试/生产）：[docs/deployment/DEPLOYMENT-GUIDE.md](docs/deployment/DEPLOYMENT-GUIDE.md)

最小配置启动示例（更多组合见快速启动指南）：

```bash
docker compose -f docker-compose.minimal.yml up -d
```

快速验证：

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

### 本地开发

**开发环境搭建**: 请参考 [开发者指南](docs/development/DEVELOPER-GUIDE.md)

## 📋 开发规划

📚 **规划文档**:

- 🎯 **[下一步行动计划](docs/overview/NEXT-PLAN.md)** ⭐ - 最新规划（2025-12-07）
- 📋 **[TODO项跟踪清单](docs/overview/TODO-ITEMS-TRACKING.md)** - 持续更新
- ⚡ **[规划快速参考](docs/overview/PLANNING-QUICK-REFERENCE.md)** - 快速查看
- 📚 **[发展路线图](docs/overview/NEXT-STEP-ACTION-PLAN.md)** - 历史规划（Week 4-8）
- 📖 **[产品需求](docs/architecture/requirements.md)** - 产品需求文档

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

- 📖 [统一测试指南](docs/testing/UNIFIED-TESTING-GUIDE.md)
- 📖 [集成测试指南](docs/testing/INTEGRATION-TEST-GUIDE.md)

### 运行测试

```bash
# 运行所有测试
./gradlew test --continue

# 生成测试覆盖率报告
./gradlew test jacocoTestReport --continue
```

## 🛠️ 部署指南

### 快速部署

- **Docker 部署**: [统一部署指南](docs/deployment/DEPLOYMENT-GUIDE.md) ⭐

### 生产环境部署

详见：[部署指南](docs/deployment/DEPLOYMENT-GUIDE.md)

1. 数据库配置与优化
2. Redis 集群配置
3. RabbitMQ 集群配置
4. 负载均衡配置（Nginx/HAProxy）
5. 监控告警配置（Prometheus + Grafana）

## 📚 项目文档

文档已收敛为“单一入口 + 场景分流”，完整导航以文档总索引为准：

- [docs/DOCUMENTATION-INDEX.md](docs/DOCUMENTATION-INDEX.md)

## 🤝 第三方对接

提供标准RESTful API支持：

- 订单数据同步
- 设备状态推送
- 用户信息对接
- 支付结果通知

## 📄 许可证

许可证信息以仓库声明为准（当前徽标显示 MIT）。
