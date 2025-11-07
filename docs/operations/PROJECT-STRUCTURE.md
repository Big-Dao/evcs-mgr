# 项目目录结构说明

## 📁 完整目录结构

```
evcs-mgr/                                    # 项目根目录
├── 📁 evcs-*/                              # 微服务模块
│   ├── evcs-auth/                         # 🔐 认证授权服务
│   ├── evcs-gateway/                      # 🚪 API网关
│   ├── evcs-station/                      # ⚡ 充电站管理服务
│   ├── evcs-order/                        # 📋 订单管理服务
│   ├── evcs-payment/                      # 💳 支付服务
│   ├── evcs-tenant/                       # 🏢 租户管理服务
│   ├── evcs-protocol/                     # 🔌 协议处理服务
│   ├── evcs-monitoring/                   # 📊 监控服务
│   ├── evcs-config/                       # ⚙️ 配置中心
│   ├── evcs-eureka/                       # 📡 服务注册中心
│   ├── evcs-common/                       # 🔧 公共模块
│   ├── evcs-integration/                  # 🔗 第三方集成服务
│   ├── evcs-admin/                        # 🖥️ 前端管理界面
│   └── evcs-web/                          # 🌐 Web前端（如果存在）
├── 📁 docs/                               # 项目文档
│   ├── README.md                           # 📚 文档导航中心 ⭐
│   ├── overview/                           # 📖 项目概览
│   │   ├── PROJECT-CODING-STANDARDS.md    # 项目编程规范
│   │   └── 管理层摘要.md                   # 管理层摘要
│   ├── architecture/                       # 🏗️ 架构设计
│   │   ├── architecture.md                # 系统架构设计
│   │   ├── data-model.md                  # 数据模型设计
│   │   ├── api-design.md                  # API设计规范
│   │   ├── requirements.md                # 产品需求文档
│   │   └── 协议事件模型说明.md              # 协议文档
│   ├── development/                        # 🔧 开发指南
│   │   ├── DEVELOPER-GUIDE.md            # 开发者指南
│   │   ├── AI-ASSISTANT-UNIFIED-CONFIG.md # AI助手配置
│   │   ├── AI-ASSISTANTS-INDEX.md         # AI助手索引
│   │   ├── coding-standards.md            # 编码规范
│   │   ├── API-DESIGN-STANDARDS.md        # API设计标准
│   │   └── [其他开发文档...]              # 开发相关文档
│   ├── deployment/                         # 🚀 部署运维
│   │   ├── DEPLOYMENT-GUIDE.md            # 部署指南
│   │   ├── DOCKER-CONFIGURATION-GUIDE.md # Docker配置
│   │   └── DOCKER-OPTIMIZATION.md         # Docker优化
│   ├── operations/                         # ⚙️ 运营管理
│   │   ├── MONITORING-GUIDE.md            # 监控指南
│   │   ├── BUSINESS-METRICS.md            # 业务指标
│   │   ├── DEFAULT-CREDENTIALS.md         # 默认凭据
│   │   ├── PROJECT-STRUCTURE.md           # 项目结构（本文档）
│   │   └── DOCUMENTATION-INDEX.md         # 文档索引
│   ├── testing/                            # 🧪 测试质量
│   │   ├── UNIFIED-TESTING-GUIDE.md      # 统一测试指南
│   │   ├── TEST-COMPLETION-SUMMARY.md     # 测试完成总结
│   │   └── [其他测试文档...]              # 测试相关文档
│   ├── troubleshooting/                    # 🔍 问题排查
│   │   ├── ERROR_PREVENTION-CHECKLIST.md # 错误预防清单
│   │   ├── CLAUDE_ERROR_MEMORY.md         # Claude错误记忆
│   │   └── [其他排查文档...]              # 故障处理文档
│   ├── references/                         # 📚 参考资料
│   │   ├── API-DOCUMENTATION.md          # API文档
│   │   ├── API-DOCUMENTATION-GUIDE.md    # API指南
│   │   └── CHANGELOG.md                   # 变更日志
│   ├── quick-start/                         # 📦 快速开始 (建设中)
│   └── archive/                            # 🗂️ 历史归档
│       ├── completed-weeks/               # 已完成周报
│       ├── documentation-history/         # 文档历史版本
│       ├── duplicate-docs-cleanup-*       # 重复文档清理记录
│       ├── obsolete-docs-*/               # 过时文档归档
│       ├── old-issues/                    # 历史问题记录
│       ├── progress-reports/              # 进度报告
│       └── pull-requests/                 # PR记录
├── 📁 scripts/                            # 🔧 脚本工具
│   ├── docker/                            # 🐳 Docker相关脚本
│   │   ├── health-check.sh                # 健康检查脚本
│   │   ├── start-services.sh              # 启动服务脚本
│   │   └── stop-services.sh               # 停止服务脚本
│   └── pre-commit-check.sh                # Git预提交检查
├── 📁 database/                           # 🗄️ 数据库相关
│   └── scripts/                           # 数据库脚本
│       ├── reset-admin-password.sql       # 重置管理员密码
│       └── update-password.sql            # 更新密码脚本
├── 📁 config-repo/                        # ⚙️ 配置仓库
│   ├── evcs-*-local.yml                   # 各服务本地配置
│   └── application-local.yml              # 应用本地配置
├── 📁 sql/                                # 🗄️ SQL初始化脚本
│   ├── init.sql                           # 数据库初始化
│   ├── charging_station_tables.sql        # 充电站相关表
│   └── evcs_order_tables.sql              # 订单相关表
├── 📁 monitoring/                         # 📊 监控配置
│   ├── prometheus/                        # Prometheus配置
│   ├── grafana/                           # Grafana配置
│   └── alertmanager/                      # 告警配置
├── 📁 performance-tests/                  # ⚡ 性能测试
│   ├── jmeter/                            # JMeter测试脚本
│   └── reports/                           # 性能测试报告
├── 📁 logs/                               # 📋 日志文件
├── 📁 build/                              # 🔨 构建输出
├── 📁 gradle/                             # 📦 Gradle配置
├── 📁 .git/                               # 📚 Git版本控制
├── 📁 .github/                            # 🐙 GitHub配置
│   └── workflows/                         # GitHub Actions工作流
├── 📁 .gradle/                            # 📦 Gradle缓存
├── 📁 .claude/                            # 🤖 Claude相关配置
├── 📁 .vscode/                            # 💻 VSCode配置
├── 📁 .settings/                          # ⚙️ IDE设置
├── 📄 README.md                           # 📖 项目说明文档
├── 📄 build.gradle                        # 🔨 Gradle构建配置
├── 📄 settings.gradle                     # ⚙️ Gradle设置
├── 📄 gradlew                             # 🐧 Unix构建脚本
├── 📄 gradlew.bat                         # 🪟 Windows构建脚本
├── 📄 .gitignore                          # 🚫 Git忽略文件
├── 📄 .dockerignore                       # 🐳 Docker忽略文件
├── 📄 .classpath                          # 💻 Eclipse类路径
├── 📄 .project                            # 💻 Eclipse项目
├── 📄 docker-compose*.yml                 # 🐳 Docker Compose配置
│   ├── docker-compose.yml                 # 主配置（生产环境）
│   ├── docker-compose.core-dev.yml        # 核心开发环境 ⭐
│   ├── docker-compose.dev.yml             # 开发环境
│   ├── docker-compose.local.yml           # 本地环境
│   ├── docker-compose.local-images.yml    # 本地镜像版本
│   ├── docker-compose.local-jars.yml      # 本地JAR版本
│   ├── docker-compose.monitoring.yml      # 监控服务
│   └── docker-compose.test.yml            # 测试环境
├── 📄 nul                                 # 🔨 临时文件（可删除）
└── 📄 *.md                                # 📖 各种Markdown文档
```

## 🎯 目录功能说明

### 🚀 快速开始目录 (`../overview/`)
这个目录包含新用户快速上手所需的核心文档：
- **部署指南**: 详细的环境搭建和服务启动说明
- **服务参考**: 各个微服务的功能介绍和API示例
- **默认凭据**: 系统的默认账号密码信息
- **快速文档指南**: 文档结构和使用说明

### 🔧 开发文档目录 (`../development/`)
包含开发人员需要的技术文档：
- **环境搭建**: 开发环境配置指南
- **编码规范**: 代码风格和最佳实践
- **API文档**: 接口设计和使用说明
- **测试指南**: 单元测试和集成测试指导

### 🚀 部署文档目录 (`../deployment/`)
包含系统部署相关的文档：
- **Docker部署**: 容器化部署完整指南
- **生产部署**: 生产环境部署注意事项
- **Kubernetes**: K8s部署配置和说明

### 🔧 故障排除目录 (`docs/troubleshooting/`)
包含问题诊断和解决方案：
- **错误预防清单**: 常见错误的预防措施
- **错误记忆库**: 历史问题和解决方案记录

### ⚙️ 运维文档目录 (`docs/operations/`)
包含系统运维和管理的文档：
- **文档索引**: 所有文档的导航索引
- **项目结构**: 目录结构说明（本文档）
- **用户手册**: 系统使用和操作指南

### 📦 归档目录 (`docs/archive/`)
存放历史和过时的文档：
- **历史版本**: 文档的版本历史记录
- **完成周报**: 项目周进度报告
- **清理记录**: 文档清理的记录和说明

## 🔧 脚本工具说明

### Docker脚本 (`scripts/docker/`)
- **health-check.sh**: 检查所有服务的健康状态
- **start-services.sh**: 一键启动所有服务
- **stop-services.sh**: 一键停止所有服务

### 数据库脚本 (`database/scripts/`)
- **reset-admin-password.sql**: 重置管理员管理员密码
- **update-password.sql**: 更新用户密码

## 🐳 Docker配置文件说明

### 核心配置
- **docker-compose.yml**: 生产环境完整配置
- **docker-compose.core-dev.yml**: 小规模开发环境（推荐）

### 环境特定配置
- **docker-compose.dev.yml**: 开发环境配置
- **docker-compose.test.yml**: 测试环境配置
- **docker-compose.monitoring.yml**: 监控服务扩展

### 本地开发配置
- **docker-compose.local-jars.yml**: 使用本地构建JAR
- **docker-compose.local-images.yml**: 使用本地镜像

## 📊 服务端口映射

| 服务名 | 端口 | 描述 | 配置文件 |
|--------|------|------|----------|
| gateway | 8080 | API网关 | evcs-gateway-local.yml |
| auth-service | 8081 | 认证服务 | evcs-auth-local.yml |
| station-service | 8082 | 充电站服务 | evcs-station-local.yml |
| order-service | 8083 | 订单服务 | evcs-order-local.yml |
| payment-service | 8084 | 支付服务 | evcs-payment-local.yml |
| protocol-service | 8085 | 协议服务 | evcs-protocol-local.yml |
| tenant-service | 8086 | 租户服务 | evcs-tenant-local.yml |
| monitoring-service | 8087 | 监控服务 | evcs-monitoring-local.yml |
| eureka | 8761 | 服务注册中心 | - |
| config-server | 8888 | 配置中心 | evcs-config-local.yml |
| postgres | 5432 | 数据库 | - |
| redis | 6379 | 缓存 | - |
| rabbitmq | 5672/15672 | 消息队列 | - |

## 🎯 使用建议

### 新用户
1. 先阅读 `../overview/DEPLOYMENT-GUIDE.md`
2. 使用 `docker-compose.core-dev.yml` 快速启动
3. 参考 `../overview/SERVICES-REFERENCE.md` 了解各服务

### 开发人员
1. 查看 `../development/` 目录下的开发文档
2. 使用 `scripts/docker/` 下的脚本管理服务
3. 遇到问题查看 `docs/troubleshooting/` 目录

### 运维人员
1. 熟悉 `docs/operations/` 目录下的运维文档
2. 使用 `docker-compose.yml` 进行生产部署
3. 关注 `docs/monitoring/` 目录下的监控配置

## 📝 文档维护

### 新增文档
- 快速开始类文档放入 `../overview/`
- 技术文档放入相应的分类目录
- 过时文档移入 `docs/archive/`

### 文档更新
- 定期检查文档的时效性
- 重大变更后更新相关文档
- 保持目录结构的一致性

这个目录结构设计旨在：
- 📁 **逻辑清晰**: 按功能和用途分类
- 🚀 **易于导航**: 新用户能快速找到所需信息
- 🔧 **便于维护**: 结构化的文档管理
- 📈 **可扩展**: 支持未来功能扩展