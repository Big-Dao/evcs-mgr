# EVCS Manager 文档索引# 📚 EVCS Manager 文档导航



**最后更新**: 2025-10-28  > **最后更新**: 2025-10-20  

**项目阶段**: P4 Week 2 - 代码重构与测试修复完成> **项目阶段**: P4 Week 1 - 质量提升  

> **文档状态**: ✅ 已整理优化（2025-10-20）

---

---

## 📚 快速导航

## 🚀 快速开始（根目录）

### 核心文档（必读）

| 文档 | 描述 | 适用对象 |

| 文档 | 说明 | 位置 ||------|------|---------|

|------|------|------|| [README.md](README.md) | 项目概览、技术栈、快速启动 | 所有人 ⭐ |

| [README.md](README.md) | 项目概览、快速开始 | 根目录 || [DOCKER-DEPLOYMENT.md](DOCKER-DEPLOYMENT.md) | Docker 快速部署指南 | 开发者/运维 ⭐ |

| [README-TENANT-ISOLATION.md](README-TENANT-ISOLATION.md) | 多租户隔离详解 | 根目录 || [TEST-ENVIRONMENT-QUICKSTART.md](TEST-ENVIRONMENT-QUICKSTART.md) | 测试环境 5分钟快速启动 | 开发者 ⭐ |

| [TECHNICAL-DESIGN.md](docs/TECHNICAL-DESIGN.md) | 技术架构设计 | docs/ || [NEXT-STEPS-QUICKSTART.md](NEXT-STEPS-QUICKSTART.md) | 12周行动计划（P4阶段） | 项目经理 ⭐ |

| [API-DOCUMENTATION.md](docs/API-DOCUMENTATION.md) | API接口文档 | docs/ || [NEXT-STEP-PROGRESS-REPORT.md](NEXT-STEP-PROGRESS-REPORT.md) | 当前进度报告（每日更新） | 项目经理/开发者 ⭐ |

| [README-TENANT-ISOLATION.md](README-TENANT-ISOLATION.md) | 多租户隔离技术说明 | 架构师/开发者 |

### 开发指南

---

| 文档 | 说明 | 位置 |

|------|------|------|## 📋 进度与规划

| [DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md) | 开发者指南 | docs/ |

| [COPILOT-INSTRUCTIONS-SETUP.md](docs/COPILOT-INSTRUCTIONS-SETUP.md) | GitHub Copilot配置 | docs/ |### 进度跟踪

| [IDE-FIX-GUIDE.md](docs/development/IDE-FIX-GUIDE.md) | IDE问题修复 | docs/development/ |- [docs/PROGRESS.md](docs/PROGRESS.md) - 项目进度和里程碑

- [docs/CHANGELOG.md](docs/CHANGELOG.md) - 版本变更日志

### 运维文档

### 规划文档

| 文档 | 说明 | 位置 |- [docs/ROADMAP.md](docs/ROADMAP.md) - 项目路线图（P0-P4+）

|------|------|------|- [docs/DEVELOPMENT-PLAN.md](docs/DEVELOPMENT-PLAN.md) - 开发计划详情

| [OPERATIONS-MANUAL.md](docs/OPERATIONS-MANUAL.md) | 运维手册 | docs/ |

| [MONITORING-GUIDE.md](docs/MONITORING-GUIDE.md) | 监控指南 | docs/ |---

| [DEPLOYMENT-GUIDE.md](docs/deployment/DEPLOYMENT-GUIDE.md) | 部署指南 | docs/deployment/ |

| [TEST-ENVIRONMENT-GUIDE.md](docs/deployment/TEST-ENVIRONMENT-GUIDE.md) | 测试环境指南 | docs/deployment/ |## 🏗️ 技术文档



### 测试文档### 架构设计

- [docs/TECHNICAL-DESIGN.md](docs/TECHNICAL-DESIGN.md) - 技术架构设计 ⭐

| 文档 | 说明 | 位置 |- [README-TENANT-ISOLATION.md](README-TENANT-ISOLATION.md) - 多租户隔离架构 ⭐

|------|------|------|- [docs/TENANT-ISOLATION-COMPARISON.md](docs/TENANT-ISOLATION-COMPARISON.md) - 租户隔离方案对比

| [TESTING-GUIDE.md](docs/testing/TESTING-GUIDE.md) | 测试指南 | docs/testing/ |- [docs/TENANT-CONTEXT-ASYNC-RFC.md](docs/TENANT-CONTEXT-ASYNC-RFC.md) - 租户上下文异步方案 RFC

| [TESTING-FRAMEWORK-GUIDE.md](docs/testing/TESTING-FRAMEWORK-GUIDE.md) | 测试框架指南 | docs/testing/ |- [docs/协议事件模型说明.md](docs/协议事件模型说明.md) - 协议事件架构

| [TEST-FIX-GUIDE.md](docs/testing/TEST-FIX-GUIDE.md) | 测试修复指南 | docs/testing/ |

| [TEST-COMPLETION-SUMMARY.md](docs/testing/TEST-COMPLETION-SUMMARY.md) | 测试完成总结 | docs/testing/ |### API 文档

| [TEST-COVERAGE-REPORT.md](docs/testing/TEST-COVERAGE-REPORT.md) | 测试覆盖率报告 | docs/testing/ |- [docs/API-DOCUMENTATION.md](docs/API-DOCUMENTATION.md) - API 接口文档

- [docs/协议对接指南.md](docs/协议对接指南.md) - OCPP/云快充协议对接

### 性能优化（Week 2成果）⭐

---

| 文档 | 说明 | 位置 |

|------|------|------|## 🚢 部署与运维

| [GRAALVM-MIGRATION-EVALUATION.md](docs/performance/GRAALVM-MIGRATION-EVALUATION.md) | GraalVM迁移评估 | docs/performance/ |

| [PERFORMANCE-OPTIMIZATION-PLAN.md](docs/performance/PERFORMANCE-OPTIMIZATION-PLAN.md) | 性能优化计划 | docs/performance/ |### 部署文档

| [HIGH-ROI-OPTIMIZATION-SUMMARY.md](docs/performance/HIGH-ROI-OPTIMIZATION-SUMMARY.md) | 高ROI优化总结 | docs/performance/ |- [DOCKER-DEPLOYMENT.md](DOCKER-DEPLOYMENT.md) - Docker 快速部署（⭐ 根目录）

| [jvm-baseline.md](docs/performance/jvm-baseline.md) | JVM基线报告 | docs/performance/ |- [TEST-ENVIRONMENT-QUICKSTART.md](TEST-ENVIRONMENT-QUICKSTART.md) - 测试环境 5分钟启动（⭐ 根目录）

| [week2-day1-baseline-report.md](docs/performance/week2-day1-baseline-report.md) | Day 1基线测试 | docs/performance/ |- [docs/deployment/DEPLOYMENT-GUIDE.md](docs/deployment/DEPLOYMENT-GUIDE.md) - 完整部署指南（含 K8s）

| [week2-day2-optimization-report.md](docs/performance/week2-day2-optimization-report.md) | Day 2 GC优化 | docs/performance/ |- [docs/deployment/TEST-ENVIRONMENT-GUIDE.md](docs/deployment/TEST-ENVIRONMENT-GUIDE.md) - 测试环境详细指南

| [week2-day3-final-test-report.md](docs/performance/week2-day3-final-test-report.md) | Day 3 GC最终测试 | docs/performance/ |

| [week2-day4-optimization-report.md](docs/performance/week2-day4-optimization-report.md) | Day 4数据库优化 ⭐ | docs/performance/ |### 运维手册

- [docs/OPERATIONS-MANUAL.md](docs/OPERATIONS-MANUAL.md) - 运维手册

### 监控体系

- [docs/MONITORING-GUIDE.md](docs/MONITORING-GUIDE.md) - 监控指南
- [docs/BUSINESS-METRICS.md](docs/BUSINESS-METRICS.md) - **业务监控指标** ⭐ **（新增 2025-10-28）**
- [docs/monitoring/business-metrics-implementation-summary.md](docs/monitoring/business-metrics-implementation-summary.md) - 业务监控实施总结
- [monitoring/grafana/dashboards/business-metrics.json](monitoring/grafana/dashboards/business-metrics.json) - Grafana Dashboard 配置
- [scripts/verify-business-metrics.ps1](scripts/verify-business-metrics.ps1) - 监控指标验证脚本

### 协议对接
- [docs/协议故障排查手册.md](docs/协议故障排查手册.md) - 协议故障排查

| 文档 | 说明 | 位置 |

|------|------|------|---

| [协议对接指南.md](docs/协议对接指南.md) | 协议对接指南 | docs/ |

| [协议事件模型说明.md](docs/协议事件模型说明.md) | 事件模型说明 | docs/ |## 🧪 测试文档

| [协议故障排查手册.md](docs/协议故障排查手册.md) | 故障排查手册 | docs/ |

### 测试指南

### 规划文档- [TEST-ENVIRONMENT-QUICKSTART.md](TEST-ENVIRONMENT-QUICKSTART.md) - 测试环境快速启动（⭐ 根目录）

- [docs/testing/TESTING-FRAMEWORK-GUIDE.md](docs/testing/TESTING-FRAMEWORK-GUIDE.md) - 测试框架完整指南 ⭐ **（新合并）**

| 文档 | 说明 | 位置 |- [docs/testing/TESTING-GUIDE.md](docs/testing/TESTING-GUIDE.md) - 测试最佳实践

|------|------|------|

| [PRODUCT-REQUIREMENTS.md](docs/PRODUCT-REQUIREMENTS.md) | 产品需求 | docs/ |### 测试报告

| [DEVELOPMENT-PLAN.md](docs/DEVELOPMENT-PLAN.md) | 开发计划 | docs/ |- [docs/testing/TEST-COVERAGE-REPORT.md](docs/testing/TEST-COVERAGE-REPORT.md) - 测试覆盖率报告

| [ROADMAP.md](docs/ROADMAP.md) | 产品路线图 | docs/ |- [docs/testing/TEST-COMPLETION-SUMMARY.md](docs/testing/TEST-COMPLETION-SUMMARY.md) - 测试完成总结

| [PROGRESS.md](docs/PROGRESS.md) | 项目进度 | docs/ |- [docs/testing/TEST-FIX-GUIDE.md](docs/testing/TEST-FIX-GUIDE.md) - 测试修复指南

| [CHANGELOG.md](docs/CHANGELOG.md) | 变更日志 | docs/ |

---

### 管理文档

## 👨‍💻 开发文档

| 文档 | 说明 | 位置 |

|------|------|------|### 开发指南

| [管理层摘要.md](docs/管理层摘要.md) | 管理层摘要 | docs/ |- [docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md) - 开发者指南 ⭐

| [项目进度甘特图.md](docs/项目进度甘特图.md) | 甘特图 | docs/ |- [docs/COPILOT-INSTRUCTIONS-SETUP.md](docs/COPILOT-INSTRUCTIONS-SETUP.md) - Copilot 配置说明

| [如何查看项目计划.md](docs/如何查看项目计划.md) | 计划查看指南 | docs/ |

| [监控架构图.md](docs/监控架构图.md) | 监控架构 | docs/ |### AI 辅助开发 🤖

- [.github/copilot-instructions.md](.github/copilot-instructions.md) - GitHub Copilot 项目指令 ⭐

---- [.github/README.md](.github/README.md) - .github 目录说明

- [.github/MAINTENANCE.md](.github/MAINTENANCE.md) - .github 维护指南

## 🗂️ 目录结构

### 模块开发规范

```- [.github/instructions/common.instructions.md](.github/instructions/common.instructions.md) - evcs-common 模块规范

evcs-mgr/- [.github/instructions/station.instructions.md](.github/instructions/station.instructions.md) - evcs-station 模块规范

├── README.md                          # 项目主文档- [.github/instructions/test.instructions.md](.github/instructions/test.instructions.md) - 测试编写规范

├── README-TENANT-ISOLATION.md         # 租户隔离详解- [.github/instructions/documentation.instructions.md](.github/instructions/documentation.instructions.md) - **文档管理规范** ⭐ **（新增）**

├── docker-compose.yml                 # Docker编排配置

├── docker-compose.yml.backup-day1     # Day 1配置备份

### 问题修复

- [docs/development/IDE-FIX-GUIDE.md](docs/development/IDE-FIX-GUIDE.md) - IDE 错误修复指南
- [docs/development/DOCKER-BUILD-FIX.md](docs/development/DOCKER-BUILD-FIX.md) - Docker 构建依赖修复 ⭐

├── docs/                              # 文档目录

│   ├── README.md                      # 文档说明### CI/CD

│   ├── TECHNICAL-DESIGN.md            # 技术设计- [.github/workflows/build.yml](.github/workflows/build.yml) - 构建流水线

│   ├── API-DOCUMENTATION.md           # API文档- [.github/workflows/test-environment.yml](.github/workflows/test-environment.yml) - 测试环境部署

│   ├── DEVELOPER-GUIDE.md             # 开发指南

│   ├── OPERATIONS-MANUAL.md           # 运维手册---

│   ├── MONITORING-GUIDE.md            # 监控指南

│   ├── TENANT-CONTEXT-ASYNC-RFC.md    # 异步租户上下文RFC## 📊 周报与总结

│   ├── TENANT-ISOLATION-COMPARISON.md # 租户隔离对比

│   │### 最新周报（已归档）

│   ├── performance/                   # 性能优化文档 ⭐- [docs/archive/completed-weeks/第8周完成报告.md](docs/archive/completed-weeks/第8周完成报告.md) - 第8周完成报告

│   │   ├── GRAALVM-MIGRATION-EVALUATION.md- [docs/archive/completed-weeks/第7周完成报告.md](docs/archive/completed-weeks/第7周完成报告.md) - 第7周完成报告

│   │   ├── PERFORMANCE-OPTIMIZATION-PLAN.md

│   │   ├── HIGH-ROI-OPTIMIZATION-SUMMARY.md### Day-by-Day 进度（已归档）

│   │   ├── jvm-baseline.md- [docs/archive/progress-reports/](docs/archive/progress-reports/) - 每日进度报告归档

│   │   ├── week2-day1-baseline-report.md  - WEEK1-DAY1-SUMMARY.md

│   │   ├── week2-day2-gc-optimization-plan.md  - WEEK1-DAY2-PROGRESS.md

│   │   ├── week2-day2-optimization-report.md  - WEEK1-DAY1-3-FINAL-REPORT.md

│   │   ├── week2-day2-summary.md  - 等共12个临时进度报告

│   │   ├── week2-day3-final-test-report.md

│   │   ├── week2-day4-optimization-report.md  # 最新 ✅---

│   │   ├── week2-day4-plan.md

│   │   └── WEEK2-PREPARATION-COMPLETE.md## 📦 模块 README

│   │

│   ├── deployment/                    # 部署文档- [evcs-payment/README.md](evcs-payment/README.md) - 支付模块说明

│   │   ├── README.md- [evcs-admin/README.md](evcs-admin/README.md) - 前端管理界面说明

│   │   ├── DEPLOYMENT-GUIDE.md- [monitoring/README.md](monitoring/README.md) - 监控系统说明

│   │   └── TEST-ENVIRONMENT-GUIDE.md- [performance-tests/README.md](performance-tests/README.md) - 性能测试说明

│   │

│   ├── development/                   # 开发文档---

│   │   ├── README.md

│   │   └── IDE-FIX-GUIDE.md## 🗂️ 归档文档

│   │

│   ├── testing/                       # 测试文档### 已完成阶段

│   │   ├── README.md- [docs/archive/completed-weeks/](docs/archive/completed-weeks/) - 已完成周报归档

│   │   ├── TESTING-GUIDE.md  - 第8周完成报告.md

│   │   ├── TESTING-FRAMEWORK-GUIDE.md  - 第7周完成报告.md

│   │   ├── TEST-FIX-GUIDE.md  - 第7周任务验收清单.md

│   │   ├── TEST-COMPLETION-SUMMARY.md  - WEEK1-EXECUTIVE-SUMMARY.md

│   │   └── TEST-COVERAGE-REPORT.md  - WEEK2-COMPLETION-SUMMARY.md

│   │  - WEEK4-COMPLETION-SUMMARY.md

│   └── archive/                       # 归档文档  - WEEK5-COMPLETION-SUMMARY.md

│       ├── README.md  - WEEK6-SUMMARY.md

│       ├── completed-weeks/           # 已完成周报

│       ├── documentation-history/     # 文档历史### 临时进度报告

│       ├── old-issues/                # 历史问题- [docs/archive/progress-reports/](docs/archive/progress-reports/) - 临时进度报告归档（12个文档）

│       ├── old-planning/              # 历史规划

│       ├── progress-reports/          # 历史进度报告### 历史规划

│       ├── pull-requests/             # PR记录- [docs/archive/old-planning/](docs/archive/old-planning/) - 历史规划文档

│       └── test-fixes/                # 测试修复记录- [docs/archive/old-issues/](docs/archive/old-issues/) - 历史问题记录

│

├── performance-tests/                 # 性能测试脚本---

│   ├── README.md                      # 测试说明

│   ├── simple-baseline.ps1            # 基线测试脚本## 🔍 快速查找

│   ├── long-load-test.ps1             # 长时间负载测试

│   ├── simple-long-test.ps1           # 简化长测试### 按角色查找

│   ├── health-check.ps1               # 健康检查

│   ├── quick-verify.ps1               # 快速验证**项目经理/PO**:

│   ├── baseline-health-test.ps1       # 基线健康测试1. [NEXT-STEP-PROGRESS.md](NEXT-STEP-PROGRESS.md) - 当前进度

│   ├── jvm-tuning-test.jmx            # JMeter测试计划2. [docs/PROGRESS.md](docs/PROGRESS.md) - 里程碑

│   ├── JMeter-Test-Plan-Design.md     # JMeter设计文档3. [docs/ROADMAP.md](docs/ROADMAP.md) - 路线图

│   ├── TOOLS-INSTALLATION-GUIDE.md    # 工具安装指南4. [docs/P2-P3-FINAL-SUMMARY.md](docs/P2-P3-FINAL-SUMMARY.md) - P2/P3总结

│   ├── results/                       # 测试结果

│   └── logs/                          # 测试日志**开发者**:

│1. [README.md](README.md) - 快速启动

├── sql/                               # SQL脚本2. [docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md) - 开发指南

│   ├── init.sql                       # 初始化脚本3. [TEST-FRAMEWORK-SUMMARY.md](TEST-FRAMEWORK-SUMMARY.md) - 测试框架

│   ├── charging_station_tables.sql    # 充电站表4. [.github/copilot-instructions.md](.github/copilot-instructions.md) - AI 辅助开发

│   ├── evcs_order_tables.sql          # 订单表

│   ├── payment_order.sql              # 支付表**运维/DevOps**:

│   └── performance-indexes.sql        # 性能索引 ⭐1. [DOCKER-DEPLOYMENT.md](DOCKER-DEPLOYMENT.md) - Docker 部署

│2. [docs/OPERATIONS-MANUAL.md](docs/OPERATIONS-MANUAL.md) - 运维手册

├── scripts/                           # 运维脚本3. [docs/MONITORING-GUIDE.md](docs/MONITORING-GUIDE.md) - 监控配置

│   ├── start-local.ps1                # 本地启动

│   ├── start-dev.ps1                  # 开发启动**QA/测试**:

│   ├── stop-all.ps1                   # 停止服务1. [TEST-ENVIRONMENT-QUICKSTART.md](TEST-ENVIRONMENT-QUICKSTART.md) - 测试环境

│   ├── health-check.sh                # 健康检查2. [TEST-COVERAGE-REPORT.md](TEST-COVERAGE-REPORT.md) - 覆盖率报告

│   ├── smoke-test.sh                  # 冒烟测试3. [docs/TESTING-QUICKSTART.md](docs/TESTING-QUICKSTART.md) - 测试快速入门

│   ├── start-test.sh                  # 测试启动

│   └── stop-test.sh                   # 测试停止### 按任务查找

│

└── monitoring/                        # 监控配置**首次接触项目**: 

    ├── README.md- → [README.md](README.md) → [docs/TECHNICAL-DESIGN.md](docs/TECHNICAL-DESIGN.md)

    ├── elk/                           # ELK配置

    └── grafana/                       # Grafana配置**配置开发环境**: 

```- → [docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md) → [IDE-FIX-GUIDE.md](IDE-FIX-GUIDE.md)



---**部署项目**: 

- → [DOCKER-DEPLOYMENT.md](DOCKER-DEPLOYMENT.md) → [TEST-ENVIRONMENT-QUICKSTART.md](TEST-ENVIRONMENT-QUICKSTART.md)

## 📋 文档分类说明

**编写测试**: 

### 根目录文档- → [TEST-FRAMEWORK-SUMMARY.md](TEST-FRAMEWORK-SUMMARY.md) → [.github/instructions/test.instructions.md](.github/instructions/test.instructions.md)

保留最核心、最常用的文档：

- **README.md**: 项目入口文档**对接协议**: 

- **README-TENANT-ISOLATION.md**: 多租户核心特性文档- → [docs/协议对接指南.md](docs/协议对接指南.md) → [docs/协议事件模型说明.md](docs/协议事件模型说明.md)



### docs/ 主文档**了解多租户**: 

架构、设计、开发、运维等主要文档- → [README-TENANT-ISOLATION.md](README-TENANT-ISOLATION.md) → [docs/TECHNICAL-DESIGN.md](docs/TECHNICAL-DESIGN.md)



### docs/performance/ 性能优化---

Week 2完整的性能优化过程记录

## 📝 文档维护说明

### docs/deployment/ 部署相关

部署、环境配置相关文档### 文档更新频率

- **每日更新**: NEXT-STEP-PROGRESS.md

### docs/development/ 开发相关- **每次发布**: CHANGELOG.md, PROGRESS.md

开发工具、IDE配置等文档- **重大变更**: TECHNICAL-DESIGN.md, API-DOCUMENTATION.md

- **按需更新**: README.md, 各模块 README

### docs/testing/ 测试相关

测试框架、测试指南、测试报告等### 文档规范

- 所有文档使用 Markdown 格式

### docs/archive/ 归档- 重要文档标记 ⭐

历史文档、已完成任务、过时报告等- 保持目录结构清晰

- 过时文档及时归档到 docs/archive/

---

### 联系方式

## 🎯 Week 2 性能优化成果- 项目仓库: [Big-Dao/evcs-mgr](https://github.com/Big-Dao/evcs-mgr)

- 问题反馈: GitHub Issues

### 最终配置

```yaml---

# GC配置

JAVA_OPTS: -Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100**文档数量**: 200+ 个 Markdown 文档  

**最后检查**: 2025-10-20

# HikariCP连接池

maximum-pool-size: 30
minimum-idle: 10
connection-timeout: 20000
```

### 性能指标
| 服务 | TPS | 平均响应 | 错误率 |
|------|-----|---------|--------|
| Order | 3.71 | 265ms | 0% |
| Station | **3.79** | **264ms** | 0% |
| Gateway | 3.66 | 263ms | 0% |

**Station性能突破**: TPS +232%, 响应时间 -68% ✅

---

## 📖 阅读路径建议

### 新成员入门
1. README.md
2. TECHNICAL-DESIGN.md
3. DEVELOPER-GUIDE.md
4. README-TENANT-ISOLATION.md

### 开发人员
1. DEVELOPER-GUIDE.md
2. API-DOCUMENTATION.md
3. TESTING-GUIDE.md
4. COPILOT-INSTRUCTIONS-SETUP.md

### 运维人员
1. OPERATIONS-MANUAL.md
2. DEPLOYMENT-GUIDE.md
3. MONITORING-GUIDE.md
4. TEST-ENVIRONMENT-GUIDE.md

### 性能优化
1. PERFORMANCE-OPTIMIZATION-PLAN.md
2. week2-day1-baseline-report.md
3. week2-day2-optimization-report.md
4. week2-day3-final-test-report.md
5. week2-day4-optimization-report.md ⭐

### 协议开发
1. 协议对接指南.md
2. 协议事件模型说明.md
3. 协议故障排查手册.md

---

## 🔄 文档维护规则

### 文档位置规范
- **根目录**: 仅保留最核心的2-3个文档
- **docs/**: 主要文档（架构、开发、运维）
- **docs/[category]/**: 分类文档（performance/deployment/testing/development）
- **docs/archive/**: 归档文档（历史报告、已完成任务）

### 文档命名规范
- **大写蛇形**: `API-DOCUMENTATION.md` (主要文档)
- **小写连字符**: `week2-day1-baseline-report.md` (临时/周期性文档)
- **中文**: `协议对接指南.md` (中文优先的业务文档)

### 归档规则
超过1个月的临时文档自动归档到 `docs/archive/`

---

**索引维护**: 每次文档结构变更后更新本索引  
**最后更新**: 2025-10-25 23:50  
**维护者**: GitHub Copilot

### Docker 镜像优化

- [DOCKER-OPTIMIZATION.md](DOCKER-OPTIMIZATION.md) - Docker 多阶段构建优化指南（⭐ 新增）
- [DOCKER-OPTIMIZATION-COMPARISON.md](DOCKER-OPTIMIZATION-COMPARISON.md) - 优化前后对比分析（⭐ 新增）
- [scripts/verify-docker-builds.sh](scripts/verify-docker-builds.sh) - Docker 构建验证脚本

**关键改进**：
- ✅ 三阶段构建：依赖下载 → 编译构建 → 运行时
- ✅ 镜像体积减少 30%（前端减少 73%）
- ✅ 增量构建时间降低 70%
- ✅ 依赖缓存命中率 > 90%
- ✅ 所有服务非 root 运行
- ✅ 统一的容器优化 JVM 参数

