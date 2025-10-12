# 充电站管理平台 (EVCS Manager)

一个基于Spring Boot和微服务架构的电动汽车充电站管理平台，支持多租户、多协议充电桩管理。

## 📌 项目状态

**当前阶段**: ✅ P2 & P3 全部完成  
**最近更新**: 2025-10-12

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

### 下一步计划（P4）
- 📱 用户移动端APP（iOS + Android）
- 📲 微信小程序 + 支付宝小程序
- 📊 BI报表与数据大屏
- ☁️ Kubernetes编排与服务网格
- 🧪 单元测试覆盖率提升至80%+

详见：[项目路线图](docs/ROADMAP.md) | [进度追踪](docs/PROGRESS.md)

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
- 单元测试覆盖率 > 80%
- 集成测试覆盖核心业务流程
- 性能测试确保系统稳定性
- 完善的测试框架支持，详见 [测试指南](docs/TESTING-GUIDE.md)

### 运行测试
```bash
# 运行所有测试
./gradlew test

# 运行指定模块测试
./gradlew :evcs-station:test

# 生成测试覆盖率报告
./gradlew test jacocoTestReport
# 报告位置: build/reports/jacoco/test/html/index.html
```

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
- 产品需求（PRD）：[docs/PRODUCT-REQUIREMENTS.md](docs/PRODUCT-REQUIREMENTS.md)
- 技术方案：[docs/TECHNICAL-DESIGN.md](docs/TECHNICAL-DESIGN.md)
- 路线图与计划：[docs/ROADMAP.md](docs/ROADMAP.md)
- 进度与里程碑：[docs/PROGRESS.md](docs/PROGRESS.md)
- 变更日志：[docs/CHANGELOG.md](docs/CHANGELOG.md)
- **测试指南**：[docs/TESTING-GUIDE.md](docs/TESTING-GUIDE.md) ⭐️
- 多租户隔离详解：[README-TENANT-ISOLATION.md](README-TENANT-ISOLATION.md)
