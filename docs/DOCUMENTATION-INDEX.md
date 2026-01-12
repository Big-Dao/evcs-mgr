# DOCUMENTATION-INDEX（统一文档入口）

> 一句话说明：本文件是当前仓库“非归档文档”的统一导航入口；各主题只保留一个权威 SSOT，其它文档尽量通过链接引用避免重复维护。

**最后更新**: 2026-01-12  \
**维护者**: 技术负责人 / 各领域维护团队  \
**状态**: 已发布

---

## 快速入口（按任务）

- 读规范（必读）：[docs/overview/PROJECT-CODING-STANDARDS.md](overview/PROJECT-CODING-STANDARDS.md)
- 开发上手：
  - 开发者指南：[docs/development/DEVELOPER-GUIDE.md](development/DEVELOPER-GUIDE.md)
  - 代码质量清单：[docs/development/CODE-QUALITY-CHECKLIST.md](development/CODE-QUALITY-CHECKLIST.md)
- 跑测试：
  - 统一测试指南：[docs/testing/UNIFIED-TESTING-GUIDE.md](testing/UNIFIED-TESTING-GUIDE.md)
  - 集成测试指南：[docs/testing/INTEGRATION-TEST-GUIDE.md](testing/INTEGRATION-TEST-GUIDE.md)
- 部署：
  - 生产部署规划：[docs/deployment/DEPLOYMENT-GUIDE.md](deployment/DEPLOYMENT-GUIDE.md)
  - 测试环境部署：[docs/deployment/TEST-ENVIRONMENT-GUIDE.md](deployment/TEST-ENVIRONMENT-GUIDE.md)
- 查接口：API 文档：[docs/references/API-DOCUMENTATION.md](references/API-DOCUMENTATION.md)
- 常见故障预防： [docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md](troubleshooting/ERROR_PREVENTION_CHECKLIST.md)
- 看历史：版本历史记录：[docs/overview/VERSION-HISTORY.md](overview/VERSION-HISTORY.md)

## 项目与规范（SSOT）

- 仓库统一入口（AI 助手/规范索引）：[AGENTS.md](../AGENTS.md)
- 项目编码与架构规范（SSOT）：[docs/overview/PROJECT-CODING-STANDARDS.md](overview/PROJECT-CODING-STANDARDS.md)
- AI 助手统一配置（SSOT）：[docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md](development/AI-ASSISTANT-UNIFIED-CONFIG.md)

## 概览（overview）

- 管理层摘要：[docs/overview/EXECUTIVE-SUMMARY.md](overview/EXECUTIVE-SUMMARY.md)
- 版本历史记录：[docs/overview/VERSION-HISTORY.md](overview/VERSION-HISTORY.md)
- 规划/路线图：[docs/overview/NEXT-PLAN.md](overview/NEXT-PLAN.md)、[docs/overview/NEXT-STEP-ACTION-PLAN.md](overview/NEXT-STEP-ACTION-PLAN.md)
- 文档快速导航（指针）：[docs/overview/QUICK-DOCUMENTATION-GUIDE.md](overview/QUICK-DOCUMENTATION-GUIDE.md)

## 架构（architecture）

- 总体架构：[docs/architecture/architecture.md](architecture/architecture.md)
- API 设计：[docs/architecture/api-design.md](architecture/api-design.md)
- 数据模型：[docs/architecture/data-model.md](architecture/data-model.md)
- 多租户异步上下文 RFC：[docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md](architecture/TENANT-CONTEXT-ASYNC-RFC.md)

## 功能（features）

- 站点地图分析：[docs/features/STATION-MAP-ANALYTICS.md](features/STATION-MAP-ANALYTICS.md)
- 按枪口（Connector）控制与会话落库：[docs/features/CHARGER-CONNECTOR-CONTROL.md](features/CHARGER-CONNECTOR-CONTROL.md)

## 核心概念（concepts）

- **异步上下文传播**：[docs/concepts/ASYNC-CONTEXT-PROPAGATION.md](concepts/ASYNC-CONTEXT-PROPAGATION.md) ⭐  
  解释什么是异步上下文传播、为什么需要它，以及如何在项目中使用

## 开发（development）

- 开发者指南：[docs/development/DEVELOPER-GUIDE.md](development/DEVELOPER-GUIDE.md)
- API 设计规范：[docs/development/API-DESIGN-STANDARDS.md](development/API-DESIGN-STANDARDS.md)
- 数据库设计规范：[docs/development/DATABASE-DESIGN-STANDARDS.md](development/DATABASE-DESIGN-STANDARDS.md)
- 代码质量清单：[docs/development/CODE-QUALITY-CHECKLIST.md](development/CODE-QUALITY-CHECKLIST.md)

## 部署（deployment）

- 生产部署规划（发布/回滚/验收基线）：[docs/deployment/DEPLOYMENT-GUIDE.md](deployment/DEPLOYMENT-GUIDE.md)
- 测试部署规划（CI/CD 与集成验证）：[docs/deployment/TEST-ENVIRONMENT-GUIDE.md](deployment/TEST-ENVIRONMENT-GUIDE.md)

## 运维（operations）

- 默认凭据（演示/测试）：[docs/operations/DEFAULT-CREDENTIALS.md](operations/DEFAULT-CREDENTIALS.md)
- 服务参考：[docs/operations/SERVICES-REFERENCE.md](operations/SERVICES-REFERENCE.md)
- 服务映射：[docs/operations/SERVICE_NAMES_MAPPING.md](operations/SERVICE_NAMES_MAPPING.md)
- 监控指南：[docs/operations/MONITORING-GUIDE.md](operations/MONITORING-GUIDE.md)
- 项目结构：[docs/operations/PROJECT-STRUCTURE.md](operations/PROJECT-STRUCTURE.md)

## 测试（testing）

- 统一测试指南：[docs/testing/UNIFIED-TESTING-GUIDE.md](testing/UNIFIED-TESTING-GUIDE.md)
- 集成测试指南：[docs/testing/INTEGRATION-TEST-GUIDE.md](testing/INTEGRATION-TEST-GUIDE.md)
- 测试修复指南：[docs/testing/TEST-FIX-GUIDE.md](testing/TEST-FIX-GUIDE.md)
- 上下文传播验收清单（多租户/traceId/异步）：[docs/testing/CONTEXT-PROPAGATION-ACCEPTANCE-CHECKLIST.md](testing/CONTEXT-PROPAGATION-ACCEPTANCE-CHECKLIST.md)
- 按枪口控制测试报告：[docs/testing/CHARGER-CONNECTOR-CONTROL-TEST-REPORT.md](testing/CHARGER-CONNECTOR-CONTROL-TEST-REPORT.md)

## 故障排查（troubleshooting）

- 错误预防清单：[docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md](troubleshooting/ERROR_PREVENTION_CHECKLIST.md)

## 参考（references）

- API 文档：[docs/references/API-DOCUMENTATION.md](references/API-DOCUMENTATION.md)
- 版本历史记录（权威）：[docs/overview/VERSION-HISTORY.md](overview/VERSION-HISTORY.md)

## 归档（archive）

- 归档目录（历史文档/进度报告/旧版本）：[docs/archive/](archive/)

备注：归档区可能包含重复内容，这是为了保留历史快照；非归档区以本索引为准。
