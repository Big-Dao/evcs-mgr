# 📚 EVCS Manager 文档导航

**最后更新**: 2025-10-30  
**项目阶段**: P4 Week 2 - 生产就绪  
**维护者**: EVCS Development Team

---

## 🚀 快速开始

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [README.md](README.md) | 项目概览、服务列表与启动方式 | 所有人 |
| [docs/deployment/DOCKER-QUICKSTART.md](docs/deployment/DOCKER-QUICKSTART.md) | 本地 Docker 快速启动指引 | 开发者 |
| [docs/deployment/TEST-ENVIRONMENT-GUIDE.md](docs/deployment/TEST-ENVIRONMENT-GUIDE.md) | 测试环境配置与验证步骤 | 开发者 / 运维 |
| [docs/deployment/LOCAL-DOCKER-DEPLOYMENT.md](docs/deployment/LOCAL-DOCKER-DEPLOYMENT.md) | 多服务本地部署全流程 | 开发者 |
| [DEFAULT-CREDENTIALS.md](DEFAULT-CREDENTIALS.md) | 默认账号、端口与访问凭证 | 运维 |

## 🧭 核心架构与规范

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/TECHNICAL-DESIGN.md](docs/TECHNICAL-DESIGN.md) | 微服务架构、租户隔离与配置管理设计 | 架构师 / 开发者 |
| [docs/README-TENANT-ISOLATION.md](docs/README-TENANT-ISOLATION.md) | 多租户隔离方案与实现细节 | 架构师 / 开发者 |
| [docs/TENANT-ISOLATION-COMPARISON.md](docs/TENANT-ISOLATION-COMPARISON.md) | 隔离策略对比与选型依据 | 架构师 |
| [docs/TENANT-CONTEXT-ASYNC-RFC.md](docs/TENANT-CONTEXT-ASYNC-RFC.md) | 租户上下文异步传递 RFC | 架构师 |
| [docs/SPRING-CLOUD-CONFIG-CONVENTIONS.md](docs/SPRING-CLOUD-CONFIG-CONVENTIONS.md) | Spring Cloud Config 规范与命名约定 | 开发者 |
| [docs/API-DOCUMENTATION.md](docs/API-DOCUMENTATION.md) | 后端 API 总览与字段说明 | 开发者 |

## 👨‍💻 开发指南

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md) | 本地开发流程、提交规范、常见问题 | 开发者 |
| [docs/COPILOT-INSTRUCTIONS-SETUP.md](docs/COPILOT-INSTRUCTIONS-SETUP.md) | GitHub Copilot 最佳实践 | 开发者 |
| [docs/development/README.md](docs/development/README.md) | 开发目录结构与近期调整说明 | 开发者 |
| [docs/development/IDE-FIX-GUIDE.md](docs/development/IDE-FIX-GUIDE.md) | IDE 常见错误与修复建议 | 开发者 |
| [docs/development/DOCKER-BUILD-FIX.md](docs/development/DOCKER-BUILD-FIX.md) | Docker 构建问题排查记录 | 开发者 |
| [docs/development/DOCKER-BUILD-FIX-SUMMARY.md](docs/development/DOCKER-BUILD-FIX-SUMMARY.md) | Docker 构建修复总结 | 开发者 |

## 🔄 测试与质量

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/testing/README.md](docs/testing/README.md) | 测试体系概览与执行约定 | 开发者 / 测试 |
| [docs/testing/TESTING-GUIDE.md](docs/testing/TESTING-GUIDE.md) | 单元 / 集成测试最佳实践 | 开发者 |
| [docs/testing/TESTING-FRAMEWORK-GUIDE.md](docs/testing/TESTING-FRAMEWORK-GUIDE.md) | 测试框架与工具链说明 | 开发者 |
| [docs/testing/TEST-FIX-GUIDE.md](docs/testing/TEST-FIX-GUIDE.md) | 常见失败用例修复技巧 | 开发者 |
| [docs/testing/TEST-COVERAGE-REPORT.md](docs/testing/TEST-COVERAGE-REPORT.md) | 覆盖率指标与分析 | 开发者 / 项目经理 |
| [docs/testing/TEST-COMPLETION-SUMMARY.md](docs/testing/TEST-COMPLETION-SUMMARY.md) | 测试完成情况汇总 | 项目经理 |
| [docs/testing/FRONTEND-TESTING-CHECKLIST.md](docs/testing/FRONTEND-TESTING-CHECKLIST.md) | 前端测试检查清单 | 前端开发者 |

## ⚙️ 运维与部署

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/OPERATIONS-MANUAL.md](docs/OPERATIONS-MANUAL.md) | 生产运维操作手册 | 运维 |
| [docs/deployment/DEPLOYMENT-GUIDE.md](docs/deployment/DEPLOYMENT-GUIDE.md) | 正式环境部署（含 K8s）流程 | 运维 |
| [docs/deployment/DOCKER-OPTIMIZATION.md](docs/deployment/DOCKER-OPTIMIZATION.md) | Docker 多阶段构建与镜像优化 | 运维 |
| [docs/deployment/DOCKER-OPTIMIZATION-COMPARISON.md](docs/deployment/DOCKER-OPTIMIZATION-COMPARISON.md) | 优化前后对比数据 | 运维 |
| [docs/deployment/HEALTHCHECK-FIX-REPORT.md](docs/deployment/HEALTHCHECK-FIX-REPORT.md) | 健康检查修复与验证记录 | 运维 |
| [docs/deployment/LOCAL-DOCKER-DEPLOYMENT-STATUS.md](docs/deployment/LOCAL-DOCKER-DEPLOYMENT-STATUS.md) | 本地环境部署状态追踪 | 开发者 |

## 📈 性能与稳定性

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/performance/PERFORMANCE-OPTIMIZATION-PLAN.md](docs/performance/PERFORMANCE-OPTIMIZATION-PLAN.md) | P4 Week 2-4 性能优化总体计划 | 架构师 / 开发者 |
| [docs/performance/HIGH-ROI-OPTIMIZATION-SUMMARY.md](docs/performance/HIGH-ROI-OPTIMIZATION-SUMMARY.md) | 高 ROI 优化项执行总结 | 项目经理 / 架构师 |
| [docs/performance/week2-day4-optimization-report.md](docs/performance/week2-day4-optimization-report.md) | Week 2 Day 4 优化结果 | 开发者 |
| [docs/performance/week2-day3-final-test-report.md](docs/performance/week2-day3-final-test-report.md) | 最终性能压测报告 | 开发者 / 测试 |
| [docs/performance/GRAALVM-MIGRATION-EVALUATION.md](docs/performance/GRAALVM-MIGRATION-EVALUATION.md) | GraalVM 迁移可行性评估 | 架构师 |
| [docs/performance/jvm-baseline.md](docs/performance/jvm-baseline.md) | JVM 基线参数与调优结论 | 开发者 |

## 📊 监控与可观测性

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/MONITORING-GUIDE.md](docs/MONITORING-GUIDE.md) | 监控体系、告警策略与指标 | 运维 |
| [docs/BUSINESS-METRICS.md](docs/BUSINESS-METRICS.md) | 业务指标定义与追踪方式 | 运维 / 项目经理 |
| [docs/monitoring/business-metrics-implementation-summary.md](docs/monitoring/business-metrics-implementation-summary.md) | 业务指标落地实施总结 | 运维 / 开发者 |
| [docs/监控架构图.md](docs/监控架构图.md) | 监控架构图与信号流 | 架构师 / 运维 |

## 📡 协议与集成

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/协议对接指南.md](docs/协议对接指南.md) | OCPP / 云快充对接流程与示例 | 开发者 |
| [docs/协议事件模型说明.md](docs/协议事件模型说明.md) | 协议事件模型与领域事件映射 | 开发者 |
| [docs/协议故障排查手册.md](docs/协议故障排查手册.md) | 协议故障排查指南与案例 | 开发者 / 运维 |

## 🗺️ 路线规划与管理

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/ROADMAP.md](docs/ROADMAP.md) | P0-P4 里程碑与交付计划 | 项目经理 |
| [docs/DEVELOPMENT-PLAN.md](docs/DEVELOPMENT-PLAN.md) | 开发阶段详细计划 | 项目经理 |
| [docs/PROGRESS.md](docs/PROGRESS.md) | 项目进度与里程碑追踪 | 项目经理 |
| [docs/PRODUCT-REQUIREMENTS.md](docs/PRODUCT-REQUIREMENTS.md) | 产品需求说明书 | 产品经理 |
| [docs/管理层摘要.md](docs/管理层摘要.md) | 管理层高层摘要 | 管理层 |
| [docs/项目进度甘特图.md](docs/项目进度甘特图.md) | 项目进度甘特图（静态） | 项目经理 |
| [docs/如何查看项目计划.md](docs/如何查看项目计划.md) | 项目计划查看指南 | 全员 |
| [docs/10周开发路线图.md](docs/10周开发路线图.md) | 10 周开发节奏规划 | 项目经理 / 开发者 |
| [docs/下一步开发计划-执行摘要.md](docs/下一步开发计划-执行摘要.md) | 下一步开发计划执行摘要 | 管理层 |
| [docs/下一步开发计划-分析总结.md](docs/下一步开发计划-分析总结.md) | 下一步开发计划分析 | 管理层 / 项目经理 |
| [docs/NEXT-DEVELOPMENT-PLAN-ANALYSIS.md](docs/NEXT-DEVELOPMENT-PLAN-ANALYSIS.md) | 下一阶段规划分析（英文版） | 项目经理 |

## 🗃️ 历史归档

| 文档 | 描述 | 适用对象 |
|------|------|---------|
| [docs/DOCUMENTATION-REVIEW-REPORT.md](docs/DOCUMENTATION-REVIEW-REPORT.md) | 文档体系审查报告（2025-10-30） | 文档维护者 |
| [docs/archive/](docs/archive/) | 归档文档目录，包含历史版本与阶段总结 | 文档维护者 |

> 提示：归档目录中的链接以历史研究为主，如需引用到当前项目，请确认内容仍然适用。
