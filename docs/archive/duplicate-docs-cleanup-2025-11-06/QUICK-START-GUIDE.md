# EVCS Manager 快速开始指南

> **版本**: v2.0 | **更新日期**: 2025-11-02 | **预计阅读时间**: 15分钟

## 🎯 5分钟快速部署

如果你已经熟悉Docker和Spring Boot，这是最快的部署方式：

```bash
# 1. 克隆项目
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr

# 2. 一键部署（包含监控）
docker-compose up -d

# 3. 等待服务启动（约2-3分钟）
docker-compose ps

# 4. 验证部署
curl http://localhost:8080/actuator/health
```

**访问地址**:
- 🌐 **前端管理界面**: http://localhost:3000
- 🚪 **API网关**: http://localhost:8080
- 📊 **Grafana监控**: http://localhost:3001 (admin/admin123)
- 📈 **Prometheus**: http://localhost:9090

## 📋 系统要求

### 基础环境
- **Java**: JDK 21+ (必须)
- **Docker**: 20.10+ (推荐)
- **Docker Compose**: 2.0+ (推荐)
- **内存**: 最小4GB，推荐8GB+
- **磁盘**: 最小10GB可用空间

### 开发环境（可选）
- **IDE**: IntelliJ IDEA 2023+ 或 VS Code
- **Git**: 2.30+
- **Gradle**: 8.5+ (项目自带wrapper)

## 🚀 详细部署指南

### 方式一：Docker完整部署（推荐 ⭐）

#### 1. 环境准备
```bash
# 检查Docker版本
docker --version
docker-compose --version

# 检查端口占用
netstat -tulpn | grep -E ':(8080|3000|5432|6379|9090|3001)'
```

#### 2. 启动服务
```bash
# 启动所有服务（包含监控组件）
docker-compose up -d

# 或者分步启动
docker-compose up -d postgresql redis rabbitmq    # 基础设施
docker-compose up -d eureka config-server         # 配置中心
docker-compose up -d auth gateway                 # 核心服务
docker-compose up -d station order payment        # 业务服务
docker-compose up -d prometheus grafana           # 监控组件
```

#### 3. 验证部署
```bash
# 检查服务状态
docker-compose ps

# 查看服务日志
docker-compose logs -f gateway

# 健康检查
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

#### 4. 初始化数据
```bash
# 访问前端界面创建租户
open http://localhost:3000

# 或使用API创建测试租户
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "code": "test-tenant",
    "name": "测试租户",
    "contactEmail": "test@example.com"
  }'
```

### 方式二：本地开发部署

#### 1. 启动基础设施
```bash
# 仅启动数据库、Redis、RabbitMQ
docker-compose -f docker-compose.local.yml up -d

# 等待服务就绪
./scripts/wait-for-it.sh localhost:5432 --timeout=60
./scripts/wait-for-it.sh localhost:6379 --timeout=30
./scripts/wait-for-it.sh localhost:5672 --timeout=30
```

#### 2. 构建项目
```bash
# 清理并构建
./gradlew clean build -x test

# 运行测试（可选）
./gradlew test
```

#### 3. 启动服务
```bash
# 启动配置中心（可选）
./gradlew :evcs-config:bootRun &

# 启动注册中心
./gradlew :evcs-eureka:bootRun &

# 启动认证服务
./gradlew :evcs-auth:bootRun &

# 启动网关服务
./gradlew :evcs-gateway:bootRun &

# 启动业务服务（按需）
./gradlew :evcs-tenant:bootRun &
./gradlew :evcs-station:bootRun &
./gradlew :evcs-order:bootRun &
./gradlew :evcs-payment:bootRun &
```

#### 4. 验证本地部署
```bash
# 检查服务状态
curl http://localhost:8761/eureka/apps     # Eureka注册中心
curl http://localhost:8080/actuator/health # API网关
curl http://localhost:8081/actuator/health # 认证服务
```

## 🔧 开发环境配置

### IDE配置

#### IntelliJ IDEA
1. **打开项目**: File → Open → 选择项目根目录
2. **设置JDK**: File → Project Structure → Project SDK → 选择JDK 21
3. **配置Gradle**: File → Settings → Build Tools → Gradle
   - Use Gradle from: 'gradle-wrapper.properties'
   - JDK for Gradle: 选择JDK 21
4. **代码格式化**: 导入项目根目录的 `.editorconfig`

#### VS Code
1. **安装扩展**:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Docker
   - GitLens

2. **配置Java**: 按 `Ctrl+Shift+P` → "Java: Configure Java Runtime" → 选择JDK 21

### 数据库配置

#### PostgreSQL连接
```bash
# 使用Docker连接
docker exec -it evcs-mgr-postgresql-1 psql -U evcs_user -d evcs_db

# 或使用本地客户端
psql -h localhost -p 5432 -U evcs_user -d evcs_db
```

#### Redis连接
```bash
# 使用Docker连接
docker exec -it evcs-mgr-redis-1 redis-cli

# 或使用本地客户端
redis-cli -h localhost -p 6379
```

### 常用开发命令

#### 构建和测试
```bash
# 完整构建
./gradlew build

# 跳过测试构建
./gradlew build -x test

# 运行所有测试
./gradlew test

# 运行指定模块测试
./gradlew :evcs-gateway:test

# 生成测试报告
./gradlew test jacocoTestReport
```

#### 代码质量
```bash
# 代码格式化
./gradlew spotlessApply

# 代码检查
./gradlew spotlessCheck

# 静态分析
./gradlew checkstyleMain checkstyleTest
```

## 📊 监控和运维

### 访问监控界面

#### Grafana仪表盘
- **地址**: http://localhost:3001
- **用户名**: admin
- **密码**: admin123
- **功能**: 系统监控、性能分析、告警管理

#### Prometheus指标
- **地址**: http://localhost:9090
- **功能**: 指标收集、查询、告警规则

#### 应用健康检查
```bash
# API网关健康状态
curl http://localhost:8080/actuator/health

# 详细健康信息
curl http://localhost:8080/actuator/health/detailed

# 指标信息
curl http://localhost:8080/actuator/metrics
```

### 日志查看

#### Docker环境
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f gateway
docker-compose logs -f auth
docker-compose logs -f station

# 查看最近的日志
docker-compose logs --tail=100 gateway
```

#### 本地环境
```bash
# 日志文件位置
tail -f logs/evcs-gateway.log
tail -f logs/evcs-auth.log

# 或查看控制台输出
```

## 🧪 测试指南

### 运行测试
```bash
# 运行所有测试
./gradlew test

# 运行特定模块测试
./gradlew :evcs-tenant:test

# 运行集成测试
./gradlew :evcs-integration:test

# 生成测试覆盖率报告
./gradlew jacocoTestReport
```

### 测试数据管理
```bash
# 清理测试数据
./gradlew cleanTest

# 初始化测试数据
./gradlew :evcs-tenant:bootRun --args='--spring.profiles.active=test'
```

## 🔍 故障排查

### 常见问题

#### 1. 服务启动失败
```bash
# 查看详细日志
docker-compose logs gateway

# 检查端口占用
netstat -tulpn | grep 8080

# 重启服务
docker-compose restart gateway
```

#### 2. 数据库连接失败
```bash
# 检查数据库状态
docker-compose ps postgresql

# 查看数据库日志
docker-compose logs postgresql

# 重启数据库
docker-compose restart postgresql
```

#### 3. Redis连接失败
```bash
# 检查Redis状态
docker-compose exec redis redis-cli ping

# 查看Redis日志
docker-compose logs redis
```

#### 4. 内存不足
```bash
# 检查系统资源
free -h
df -h

# 检查Docker资源使用
docker stats

# 调整JVM内存
export JAVA_OPTS="-Xms512m -Xmx1024m"
```

### 性能问题诊断
```bash
# 查看应用指标
curl http://localhost:8080/actuator/metrics

# 查看JVM信息
curl http://localhost:8080/actuator/info

# 查看环境变量
curl http://localhost:8080/actuator/env
```

## 📚 下一步学习

### 了解系统架构
1. 📖 [README.md](README.md) - 项目概述
2. 🏗️ [技术架构设计](docs/01-core/architecture.md) - 详细架构说明
3. 🔐 [多租户隔离详解](README-TENANT-ISOLATION.md) - 数据隔离机制

### 开发指南
1. 📝 [编码规范](docs/02-development/coding-standards.md) - 开发规范
2. 🧪 [测试框架指南](docs/testing/TESTING-FRAMEWORK-GUIDE.md) - 测试规范
3. 📡 [API接口文档](docs/01-core/api-design.md) - 接口设计

### 运维指南
1. 🐳 [Docker部署指南](docs/03-deployment/docker-deployment.md) - 完整部署方案
2. 📊 [监控配置指南](docs/03-deployment/monitoring-setup.md) - 监控配置
3. 🔧 [故障排查手册](docs/04-operations/troubleshooting.md) - 问题诊断

## 🤝 获取帮助

### 文档资源
- 📚 [完整文档索引](DOCUMENTATION-INDEX.md) - 所有文档导航
- 📈 [项目状态报告](docs/PROJECT-STATUS-REPORT.md) - 项目现状分析
- 🗺️ [发展路线图](docs/05-planning/roadmap.md) - 发展规划

### 社区支持
- 🐛 **报告问题**: [GitHub Issues](https://github.com/Big-Dao/evcs-mgr/issues)
- 💬 **技术讨论**: [GitHub Discussions](https://github.com/Big-Dao/evcs-mgr/discussions)
- 📧 **联系邮箱**: support@evcs-manager.com

### 快速联系
如果遇到部署问题，请提供以下信息：
1. **系统环境**: OS版本、Docker版本、JDK版本
2. **错误信息**: 完整的错误日志
3. **操作步骤**: 复现问题的详细步骤
4. **配置信息**: 相关的配置文件（隐去敏感信息）

---

**祝您使用愉快！** 🎉

如果这个指南对您有帮助，请给我们一个 ⭐ Star！