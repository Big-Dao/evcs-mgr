# 充电站管理平台 (EVCS Manager)

一个基于Spring Boot和微服务架构的电动汽车充电站管理平台，支持多租户、多协议充电桩管理。

## 📌 项目状态

**当前阶段**: ✅ P3 完成，进入 P4 质量提升阶段  
**最近更新**: 2025-10-20  
**下一步计划**: 📋 [12周行动计划](NEXT-STEPS-QUICK-GUIDE.md) | [详细计划](docs/NEXT-STEP-PLAN.md)

### 最新完成（2025-10-20）
- ✅ **Docker 部署完成**: 13个服务全部容器化，健康检查通过
  - 业务服务: gateway, auth, tenant, station, order, payment, protocol, monitoring
  - 基础设施: config-server, eureka, postgresql, redis, rabbitmq
- ✅ **测试基础设施完善**: H2测试数据库配置，PostgreSQL兼容模式
- ✅ **工作流规范建立**: "小改动需要先测试再提交" 流程确立
- ⏳ **测试覆盖率提升中**: 当前基线 ~30%，目标 80%

### 已完成里程碑
- ✅ **P0 & P1**: 核心架构与基础功能（多租户、计费、订单）
- ✅ **P2 阶段**: 所有核心功能已完成
  - 协议调试接口 ✅
  - 支付渠道集成（支付宝 + 微信）✅
  - 协议事件流对接（OCPP + 云快充 + RabbitMQ）✅
  - 分布式计费计划缓存广播 ✅
  - 前端管理界面（Vue 3 + Element Plus）✅
- ✅ **P3 阶段**: 生产就绪准备完成（8个里程碑全部达成）
  - M1: 环境与安全加固 ✅
  - M2: 协议对接完成 ✅
  - M3: 支付集成完成 ✅
  - M4: 性能优化完成 ✅
  - M5: 前端原型完成 ✅
  - M6: 代码质量提升 ✅
  - M7: 监控体系建设 ✅
  - M8: 文档体系完善 ✅

### 当前任务（P4 Week 1）
- 🧪 **优先级 P0**: 提升测试覆盖率
  - 补充 evcs-tenant 模块测试（0% → 50%）
  - 补充 evcs-protocol 模块测试（13% → 50%）
  - 补充 evcs-integration 集成测试
  - 目标: 本周达到 50% 覆盖率

### 下一步计划（P4 - 12周计划）
- 🧪 **Week 1-4**: 质量提升（测试覆盖率80%+、性能测试）
- 🎨 **Week 5-8**: 前端开发（管理后台 + 用户端小程序）
- 🔌 **Week 9-10**: 协议与集成（OCPP完善 + 支付扩展）
- 🚀 **Week 11-12**: 运维优化与生产上线

**立即查看**: [📋 快速指南](NEXT-STEPS-QUICK-GUIDE.md) | [📖 详细计划](docs/NEXT-STEP-PLAN.md) | [路线图](docs/ROADMAP.md) | [进度追踪](docs/PROGRESS.md)

---

## 🚀 项目特色

- **多租户架构**：支持分层租户管理，上级可管理下级，数据完全隔离
- **四层数据隔离**：数据库层、SQL层、服务层、API层全方位数据隔离
- **微服务设计**：采用Spring Cloud微服务架构，服务解耦，高可扩展
- **多协议支持**：支持OCPP协议和云快充协议
- **多支付集成**：集成支付宝、微信支付、网联支付
- **实时监控**：完善的监控系统和故障诊断机制
- **第三方对接**：提供标准API支持第三方平台对接
- **自动SQL过滤**：基于MyBatis Plus的租户数据自动过滤
- **声明式权限**：使用AOP注解实现方法级数据权限控制

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

### 权限类型
- **ALL**: 查看所有数据（超级管理员）
- **SELF**: 只能查看当前租户数据
- **CHILDREN**: 可以查看当前租户及其子租户数据

## 🏗️ 技术栈

- **后端框架**：Spring Boot 3.2.2 + Spring Cloud 2023.0.0
- **开发语言**：Java 21
- **数据库**：PostgreSQL 15 + Redis 7
- **构建工具**：Gradle 8.5
- **消息队列**：RabbitMQ
- **容器化**：Docker + Docker Compose
- **API文档**：Knife4j (Swagger)
- **安全认证**：Spring Security + JWT

## 📊 系统规模

- **充电站数量**：支持最多500个充电站
- **充电桩数量**：每站最多5000个充电桩
- **并发能力**：高并发处理能力，支持大规模部署
- **数据隔离**：完善的多租户数据隔离机制

## 🏛️ 系统架构

### 微服务模块

```
evcs-mgr
├── evcs-common          # 公共组件模块
├── evcs-gateway         # API网关服务
├── evcs-auth            # 认证授权服务
├── evcs-tenant          # 租户管理服务
├── evcs-station         # 充电站管理服务
├── evcs-order           # 订单管理服务
├── evcs-payment         # 支付集成服务
├── evcs-protocol        # 协议处理服务
├── evcs-monitoring      # 监控告警服务
├── evcs-integration     # 第三方集成服务
└── evcs-config          # 配置中心服务
```

### 数据库设计

采用PostgreSQL数据库，支持多租户数据隔离：

- **系统管理**：租户、用户、角色、权限管理
- **充电站管理**：充电站、充电桩、设备状态管理
- **订单管理**：充电订单、支付记录、交易流水
- **监控数据**：设备监控、告警记录、统计分析

## 🚀 快速开始

### 环境要求

- Java 21+
- Docker & Docker Compose
- Gradle 8.5+

### 本地开发

1. **克隆项目**
```bash
git clone <repository-url>
cd evcs-mgr
```

2. **启动基础服务**
```bash
docker-compose up -d postgres redis rabbitmq
```

3. **构建项目**
```bash
./gradlew build
```

4. **运行服务**
```bash
# 启动认证服务
./gradlew :evcs-auth:bootRun

# 启动网关服务
./gradlew :evcs-gateway:bootRun
```

### Docker部署

1. **完整环境启动**
```bash
docker-compose up -d
```

2. **访问服务**
- API网关：http://localhost:8080
- API文档：http://localhost:8080/doc.html
- 数据库：localhost:5432
- Redis：localhost:6379
- RabbitMQ管理界面：http://localhost:15672

### 测试环境部署

快速部署测试环境进行人工测试：

```bash
# 启动测试环境（包含所有服务）
./scripts/start-test.sh

# 运行健康检查
./scripts/health-check.sh

# 运行冒烟测试
./scripts/smoke-test.sh

# 停止测试环境
./scripts/stop-test.sh
```

**测试环境特点**：
- ✅ 一键部署，包含完整服务栈
- ✅ 自动化健康检查和冒烟测试
- ✅ 独立的测试数据库和配置
- ✅ 完善的日志和监控
- ✅ 适合CI/CD集成

**访问地址**：
- 租户服务：http://localhost:8081
- 充电站服务：http://localhost:8082
- 数据库管理（Adminer）：http://localhost:8090
- RabbitMQ管理：http://localhost:15672

**快速开始**: [测试环境5分钟快速部署](TEST-ENVIRONMENT-QUICKSTART.md)  
**详细文档**: [测试环境完整部署指南](docs/TEST-ENVIRONMENT-GUIDE.md)

## 🔧 配置说明

### 数据库配置

数据库默认配置：
- 主机：localhost:5432
- 数据库：evcs_db
- 用户名：evcs_user
- 密码：evcs_password

### 多租户配置

系统支持分层多租户管理：
1. **平台方**：系统最高级别，可管理所有租户
2. **运营商**：管理自己的充电站和下级合作伙伴
3. **合作伙伴**：管理自己的充电站

### 认证配置

JWT Token配置：
- 密钥：可通过环境变量 `JWT_SECRET` 配置
- 过期时间：默认2小时，可通过 `JWT_EXPIRE` 配置

## 📚 API文档

项目集成Knife4j文档，启动后访问：
- 本地：http://localhost:8080/doc.html
- 生产：http://your-domain/doc.html

## 🔐 安全特性

- **认证授权**：基于JWT的无状态认证，带路径遍历防护
- **权限控制**：细粒度的角色权限管理
- **数据隔离**：多租户数据完全隔离，通过ThreadLocal确保线程安全
- **接口安全**：统一的API安全校验
- **安全测试**：31+ comprehensive security tests covering:
  - JWT path traversal prevention
  - Tenant context isolation (validated with 10,000 concurrent operations)
  - Exception handling and error responses
  - Thread-safe context management

### Security Test Results
- ✅ Zero tenant data leaks in 10,000 concurrent operations
- ✅ Path normalization prevents whitelist bypass attacks
- ✅ Proper exception handling prevents null context operations
- See [Week 1 Security Hardening Report](docs/WEEK1-SECURITY-HARDENING.md) for details

## 📈 监控告警

- **设备监控**：实时监控充电桩状态
- **业务监控**：订单、支付、用户行为监控
- **系统监控**：服务健康状态、性能指标
- **告警机制**：多种告警方式和告警策略

## 🔌 协议支持

### OCPP协议
- 支持OCPP 1.6版本
- WebSocket长连接通信
- 标准充电流程管理

### 云快充协议
- 支持国标云快充协议
- HTTP RESTful API
- 兼容主流充电桩厂商

## 💳 支付集成

- **支付宝**：支持扫码支付、APP支付
- **微信支付**：支持扫码支付、小程序支付
- **网联支付**：支持银行卡支付

## 🤝 第三方对接

提供标准RESTful API支持：
- 订单数据同步
- 设备状态推送
- 用户信息对接
- 支付结果通知

## 📝 开发指南

### 代码规范
- 遵循阿里巴巴Java开发手册
- 统一的代码格式化配置
- 完善的注释和文档

### 测试规范

**当前测试状态**：
- ✅ **131个测试用例** (从20个提升555%)
- ✅ **92个通过** (70.2%通过率)
- ⚠️ **39个待修复** (配置和依赖问题)
- 📈 **测试覆盖率**: ~35% (目标80%)

**测试框架**：
- **基础设施**: 完整的测试基类和工具类
- **Service层**: BaseServiceTest - 自动租户上下文管理
- **Controller层**: BaseControllerTest - MockMvc支持
- **多租户**: BaseTenantIsolationTest - 租户隔离验证
- **集成测试**: BaseIntegrationTest - 完整上下文测试

**测试文档**：
- 📖 [测试覆盖率报告](TEST-COVERAGE-REPORT.md)
- 📖 [测试改进总结](docs/TESTING-IMPROVEMENTS.md)  
- 📖 [测试指南](docs/TESTING-GUIDE.md)
- 📖 [测试框架说明](TEST-FRAMEWORK-SUMMARY.md)

### 运行测试
```bash
# 运行所有测试
./gradlew test --continue

# 运行指定模块测试
./gradlew :evcs-station:test

# 生成测试覆盖率报告
./gradlew test jacocoTestReport --continue
# 报告位置: {module}/build/reports/jacoco/test/html/index.html

# 查看测试HTML报告
# 报告位置: {module}/build/reports/tests/test/index.html
```

**模块测试状态**：
| 模块 | 测试数 | 通过 | 状态 |
|------|--------|------|------|
| evcs-common | 5 | 5 | ✅ |
| evcs-auth | 12 | 12 | ✅ |
| evcs-gateway | 1 | 1 | ✅ |
| evcs-protocol | 2 | 2 | ✅ |
| evcs-tenant | 1 | 1 | ✅ |
| evcs-station | 26 | 16 | ⚠️ |
| evcs-order | 10 | 6 | ⚠️ |
| evcs-payment | 12 | 0 | ❌ |
| evcs-integration | 18 | 5 | ⚠️ |

## 🛠️ 部署指南

### 生产环境部署
1. 数据库配置
2. Redis集群配置
3. 消息队列配置
4. 负载均衡配置
5. 监控告警配置

### 扩容指南
- 水平扩展微服务实例
- 数据库读写分离
- 缓存集群部署
- CDN静态资源加速

## 📞 技术支持

如有问题，请通过以下方式联系：
- 提交Issue
- 发送邮件
- 技术交流群

## 📄 许可证

本项目采用MIT许可证，详情请参阅 [LICENSE](LICENSE) 文件。

## 📚 项目文档

### 规划与进度
- **下一步计划（快速指南）**：[NEXT-STEPS-QUICK-GUIDE.md](NEXT-STEPS-QUICK-GUIDE.md) ⭐️ 🆕
- **下一步计划（详细版）**：[docs/NEXT-STEP-PLAN.md](docs/NEXT-STEP-PLAN.md) 🆕
- 路线图与计划：[docs/ROADMAP.md](docs/ROADMAP.md)
- 进度与里程碑：[docs/PROGRESS.md](docs/PROGRESS.md)
- 变更日志：[docs/CHANGELOG.md](docs/CHANGELOG.md)

### 技术文档
- 产品需求（PRD）：[docs/PRODUCT-REQUIREMENTS.md](docs/PRODUCT-REQUIREMENTS.md)
- 技术方案：[docs/TECHNICAL-DESIGN.md](docs/TECHNICAL-DESIGN.md)
- 多租户隔离详解：[README-TENANT-ISOLATION.md](README-TENANT-ISOLATION.md)
- API文档：[docs/API-DOCUMENTATION.md](docs/API-DOCUMENTATION.md)
- **开发者指南**：[docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md) ⭐️
- **测试指南**：[docs/TESTING-GUIDE.md](docs/TESTING-GUIDE.md) ⭐️

### 运维文档
- 部署指南：[docs/DEPLOYMENT-GUIDE.md](docs/DEPLOYMENT-GUIDE.md)
- 运维手册：[docs/OPERATIONS-MANUAL.md](docs/OPERATIONS-MANUAL.md)
- 测试环境快速部署：[TEST-ENVIRONMENT-QUICKSTART.md](TEST-ENVIRONMENT-QUICKSTART.md)
