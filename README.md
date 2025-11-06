# 充电站管理平台 (EVCS Manager)

一个基于Spring Boot和微服务架构的电动汽车充电站管理平台，支持多租户、多协议充电桩管理。

> 📚 **[快速导航指南](QUICK-DOCUMENTATION-GUIDE.md)** | [完整文档索引](DOCUMENTATION-INDEX.md) | [架构设计](docs/01-core/architecture.md) | [部署指南](docs/03-deployment/docker-deployment.md)

## 🎯 项目状态

![Tests](https://img.shields.io/badge/tests-100%20passing-green)
![Coverage](https://img.shields.io/badge/coverage-~85%25-brightgreen)
![Build](https://img.shields.io/badge/build-stable-brightgreen)
![JDK](https://img.shields.io/badge/JDK-21-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

**当前阶段**: 🟢 生产就绪 - 企业级监控与安全架构完成
**最近更新**: 2025-11-06
**整体完成度**: 约85% - 核心功能完整

**系统规模**: 281个Java文件 | 12个微服务 | 23个控制器 | 39个服务类 | 37个测试文件

### 最新完成（2025-11-06）✅
- ✅ **文档体系重构**: 彻底清理重复和冲突文档，建立清晰的文档架构
- ✅ **配置管理优化**: 统一服务名配置，解决nginx与docker-compose不一致问题
- ✅ **错误预防机制**: 建立Claude错误记忆库和检查流程

### 历史完成（2025-11-02）✅
- ✅ **企业级监控基础设施**: 完整的可观测性体系
- ✅ **JDK 21统一升级**: 现代化技术栈全面升级
- ✅ **API网关安全加固**: 企业级安全防护体系

## 🏗️ 技术架构

📚 **详细架构文档**: [技术架构设计](docs/01-core/architecture.md) | [数据模型](docs/01-core/data-model.md) | [API设计](docs/01-core/api-design.md)

### 技术栈概览
- **后端框架**：Spring Boot 3.2.2 + Spring Cloud 2023.0.0
- **开发语言**：Java 21 (现代LTS版本)
- **数据库**：PostgreSQL 15 + Redis 7
- **构建工具**：Gradle 8.11.1
- **消息队列**：RabbitMQ 3
- **容器化**：Docker + Docker Compose
- **安全认证**：Spring Security + JWT

### 微服务架构
```
evcs-mgr/
├── evcs-common          # 🔧 公共组件
├── evcs-gateway         # 🚪 API网关
├── evcs-auth            # 🔐 认证授权
├── evcs-tenant          # 🏢 租户管理
├── evcs-station         # ⚡ 充电站管理
├── evcs-order           # 📋 订单管理
├── evcs-payment         # 💳 支付服务
├── evcs-protocol        # 🔌 协议处理
├── evcs-monitoring      # 📊 监控服务
└── evcs-eureka          # 📡 服务发现
```

## 🚀 快速开始

### Docker 完整部署（推荐 ⭐）

**一键部署**:
```bash
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr
docker-compose up -d
```

**访问服务**:
- 🌐 **前端管理界面**: http://localhost:3000
- 🚪 **API网关**: http://localhost:8080
- 📊 **Grafana监控**: http://localhost:3001 (admin/admin123)

📚 **完整部署指南**: [Docker部署指南](docs/03-deployment/docker-deployment.md)

### 本地开发

**开发环境搭建**: 请参考 [开发环境搭建指南](docs/02-development/setup.md)（待创建）
**前端开发**: [前端部署指南](evcs-admin/DEPLOYMENT.md)

## 📋 开发规划

📚 **详细规划文档**: [发展路线图](docs/05-planning/roadmap.md) | [产品需求](docs/01-core/requirements.md)

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

**完整文档导航**：[📚 DOCUMENTATION-INDEX.md](DOCUMENTATION-INDEX.md) ⭐

### 快速开始
- **项目概述**: [README.md](README.md) ⭐
- **文档索引**: [DOCUMENTATION-INDEX.md](DOCUMENTATION-INDEX.md) ⭐
- **产品需求**: [产品需求文档](docs/01-core/requirements.md)
- **架构设计**: [技术架构设计](docs/01-core/architecture.md)

### 开发文档
- **编码规范**: [编码规范](docs/02-development/coding-standards.md)
- **开发者指南**: [开发者指南](docs/DEVELOPER-GUIDE.md) ⭐
- **测试框架**: [测试框架指南](docs/testing/TESTING-FRAMEWORK-GUIDE.md) ⭐

### 部署与运维
- **部署指南**: [Docker部署指南](docs/03-deployment/docker-deployment.md) ⭐
- **运维手册**: [运维手册](docs/04-operations/user-manual.md)（待创建）

## 🤝 第三方对接

提供标准RESTful API支持：
- 订单数据同步
- 设备状态推送
- 用户信息对接
- 支付结果通知

## 📄 许可证

本项目采用MIT许可证，详情请参阅 [LICENSE](LICENSE) 文件。