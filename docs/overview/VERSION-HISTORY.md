# 版本历史记录

> **简短描述**：EVCS充电站管理系统的完整版本变更历史

**最后更新**: 2025-12-18  
**维护者**: 技术负责人  
**状态**: 活跃

---

## 目录

- [当前版本](#当前版本-v11---2025-12-18)
- [历史版本](#历史版本)
  - [v1.0 (2025-11-10)](#v10---2025-11-10)
  - [P4 Week 2 (2025-10-28)](#p4-week-2---2025-10-28)
  - [P4 Week 1 (2025-10-20)](#p4-week-1---2025-10-20)
  - [P3 完成 (2025-10-12)](#p3-完成---2025-10-12)
  - [P1 完成 (2025-10-04)](#p1-完成---2025-10-04)
  - [P0 完成 (2025-10-04)](#p0-完成---2025-10-04)

---

## 当前版本: v1.1 - 2025-12-18

### 📚 文档刷新

- **文档统一规范化**: 全面刷新项目文档，统一为中文表述
  - 更新所有非归档文档为纯中文版本
  - 创建统一的版本历史记录文档
  - 整合变更日志到docs/overview目录
  - 优化文档导航和索引结构

### 🔧 工具链升级 (2025-12-14)

- **构建工具更新**: 
  - Gradle 升级至 8.11.1
  - Node.js 升级至 20
  - Spring Boot 升级至 3.2.12
  
- **构建优化**: 
  - 提升Gradle构建内存至4GB
  - 优化国内镜像配置
  - 修正前端依赖版本，确保构建稳定性

---

## 历史版本

### v1.0 - 2025-11-10

#### 文档优化

- **文档审查与修复**: 全面审查并修复文档冲突、错误和过时内容 (042d08b)
  - 完全重写 DOCUMENTATION-INDEX.md，去除所有重复内容，统一格式
  - 修复 README.md 中错误的文件路径引用（README-TENANT-ISOLATION.md）
  - 补充 TECHNICAL-DESIGN.md 认证架构详细描述（Gateway JWT 验证流程）
  - 更新 DEVELOPER-GUIDE.md 添加 Spring Cloud Config 规范引用
  - 更新 OPERATIONS-MANUAL.md 添加配置管理详细指导
  - 创建 DOCUMENTATION-REVIEW-REPORT.md 完整审查报告
  - 影响: +1046/-131 行，文档质量从混乱/过时提升到清晰/准确

#### 配置管理

- **Spring Cloud Config 规范化**: 完成配置管理标准化 (5a35fb9)
  - 创建 application-local.yml 全局配置（JWT、Eureka、Actuator）
  - 所有服务配置移除重复的 JWT、Eureka 配置
  - 创建 SPRING-CLOUD-CONFIG-CONVENTIONS.md 完整规范文档（1605行）
  - 修复 Gateway 路由配置（orders→order, payments→payment）
  - 所有非 Auth 服务排除 SecurityAutoConfiguration
  - Station 服务依赖调整（仅保留 spring-security-core）
  - 影响: 16个文件变更，1956行新增，180行删除

#### 资源优化 (2025-11-08)

- **容器资源优化**: 大幅降低系统资源占用
  - 内存占用减少50-67%
  - CPU需求减少50-67%
  - 支持多级配置：
    - 最小配置：2-4GB内存
    - 优化配置：6-8GB内存
    - 完整配置：12-16GB内存
  - 智能部署：根据系统资源自动选择最佳配置
  - 监控工具：实时资源监控和自动优化脚本

#### 文档体系重构 (2025-11-06)

- **文档架构优化**: 建立清晰的文档体系
  - 彻底清理重复和冲突文档
  - 统一服务名配置，解决nginx与docker-compose不一致问题
  - 建立错误预防机制：Claude错误记忆库和检查流程

#### 监控体系建设 (2025-11-02)

- **企业级监控**: 完整的可观测性体系
  - 实现完整的监控基础设施
  - JDK统一升级至21版本
  - API网关安全加固：企业级安全防护体系

---

### P4 Week 2 - 2025-10-28

#### 重构

- **Charger 实体重构**: 修复 MyBatis Plus 重复 @TableId 问题 (3cb3a30)
  - 重构 Charger 实体继承 BaseEntity
  - 使用 `@TableId(value="charger_id")` 映射主键
  - 统一使用 `getId()`/`setId()` 替代 `getChargerId()`/`setChargerId()`
  - 影响文件: Charger.java, ChargerServiceImpl.java, ChargerController.java, BillingAssignController.java 及所有测试

#### 修复

- **H2 数据库兼容性**: 修复测试环境 SQL 语法问题
  - 将 PostgreSQL 的 `INSERT...ON CONFLICT` 改为 H2 的 `MERGE INTO` 语法
  - StationMapper.selectNearbyStations 使用子查询统计充电桩数量字段
  
- **租户编码检查逻辑**: 修复 SysTenantServiceImpl.checkTenantCodeExists
  - 将错误的 `tenant_id` 改为正确的主键 `id`

#### 测试

- ✅ **全模块测试通过** (97 个测试任务)
  - evcs-auth: 6个测试通过
  - evcs-common: 28个测试通过
  - evcs-gateway: 13个测试通过
  - evcs-integration: 18个测试通过
  - evcs-order: 17个测试通过
  - evcs-payment: 12个测试通过
  - evcs-protocol: 5个测试通过
  - evcs-station: 27个测试通过
  - evcs-tenant: 42个测试通过

---

### P4 Week 1 - 2025-10-20

#### 新增

- **Docker 部署**: 完成13个服务的容器化部署
  - 业务服务: gateway, auth, tenant, station, order, payment, protocol, monitoring
  - 基础设施: config-server, eureka, postgresql, redis, rabbitmq
  - Docker Compose 配置: docker-compose.local.yml
  - 健康检查配置: 所有服务支持健康检查

#### 修复

- **测试基础设施**: 修复H2测试数据库相关问题
  - H2 PostgreSQL模式下DOUBLE类型错误 (6709f5b)
  - H2测试schema表结构问题 (167f288)
  - 添加数据清理语句防止主键冲突 (eceb088)
  - H2测试数据SQL中文乱码问题 (2571366)
  
- **Docker部署冲突**: 解决合并冲突，完善测试基础设施 (53c62d4)

#### 改进

- **工作流规范**: 建立"先测试再提交"的开发流程
- **文档更新**: 更新项目状态、进度文档和README

#### 已知问题

- evcs-integration 模块有12个测试失败（租户上下文相关）
- evcs-payment, evcs-station, evcs-tenant 部分测试失败
- 测试覆盖率约30%，需要提升到80%

---

### P3 完成 - 2025-10-12

#### 里程碑达成

- ✅ M1: 环境与安全加固
- ✅ M2: 协议对接完成
- 🟡 M3: 支付集成（支付宝/微信模拟通道完成，真实 SDK 对接进行中）
- ✅ M4: 性能优化完成
- ✅ M5: 前端原型完成
- ✅ M6: 代码质量提升
- ✅ M7: 监控体系建设
- ✅ M8: 文档体系完善

---

### P1 完成 - 2025-10-04

#### 新增功能

- **对账服务**: evcs-order 模块
  - 新增 ReconciliationService 对账服务
  - 新增 /reconcile/run-daily 接口
  - 暴露 actuator 与 Prometheus 指标

- **计费优化**: 
  - 修正 BillingServiceImpl 结构与健壮性
  - 保持最小改动原则

---

### P0 完成 - 2025-10-04

#### 核心功能

- **分时计费计划**: 
  - 每日最多 96 段时间段
  - 站点最多 16 组计费方案
  - 充电桩可绑定计费计划

- **订单管理**: 
  - 新增 /orders/start 启动订单接口
  - 新增 /orders/stop 停止订单接口
  - 实现幂等流程
  - 订单结束自动触发计费

- **多租户隔离**: 
  - @DataScope 注解全面应用
  - TenantContext 上下文贯穿全系统

---

## 文档归档历史

### 2025-12-05 文档精简归档

- **目录结构优化**: 
  - 统一各目录 README 为简短索引
  - 建立档案入口
  - 归档清理/总结/分析类文档至 `docs/archive/*`

- **开发文档归档**: 归档至 `docs/archive/`
  - DOCUMENTATION-REORGANIZATION-PLAN/COMPLETE
  - DOCUMENTATION-CLEANUP-SUMMARY
  - PROJECT-COMPLIANCE-*
  - GITHUB-*

- **运维文档归档**: 
  - operations：DOCUMENTATION-INDEX
  - deployment：RESOURCE-OPTIMIZATION-SUMMARY
  - testing：TEST-COMPLETION-SUMMARY、TEST-COVERAGE-REPORT

- **保留单一来源**: 
  - PROJECT-CODING-STANDARDS.md
  - AI-ASSISTANT-UNIFIED-CONFIG.md
  - SHARED-PROJECT-CONTEXT.md

### 2025-12-05 AI 配置类文档归档

- **AI配置整合**: 归档至 `docs/archive/ai-config-dedup-2025-12-05/`
  - AI-CONFIG-NECESSITY-ANALYSIS.md
  - AI-CONFIG-DEDUPLICATION-REPORT.md
  - AI-CONFIG-CONSISTENCY-REPORT.md
  - CODEX-INTEGRATION-GUIDE.md
  - COPILOT-INSTRUCTIONS-SETUP.md

- **统一入口**: 
  - `docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`
  - 指针文件：`.codex/`, `.claude/`, `.github/*`

### 2025-12-05 模板与重复规范归档

- **模板归档**: 迁移至 `docs/archive/documentation-docs-cleanup-2025-12-05/`
  - MICROSERVICE-README-TEMPLATE.md
  - MINIMAL-README-TEMPLATE.md
  - SINGLE-SOURCE-PRINCIPLE-GUIDE.md
  - SINGLE-SOURCE-README-TEMPLATE.md
  - coding-standards.md

- **现行规范**: 
  - 仅保留于 `docs/overview/PROJECT-CODING-STANDARDS.md`

### 2025-12-05 部署文档精简

- **权威入口**: 
  - `DEPLOYMENT-GUIDE.md`（生产部署规划）
  - `TEST-ENVIRONMENT-GUIDE.md`（测试部署规划）

- **归档优化文档**: 归档至 `docs/archive/deployment-docs-cleanup-2025-12-05/`
  - DOCKER-OPTIMIZATION.md
  - RESOURCE-OPTIMIZATION-GUIDE.md

### 2025-12-05 开发临时文档清理

- **归档临时文档**: 
  - `docs/archive/documentation-docs-cleanup-2025-12-05/DOCKER-BUILD-FIX.md`

- **删除无关文档**: 
  - docs/development/IDE-FIX-GUIDE.md
  - docs/development/IDE-FIX-GUIDE.OLD.md

---

## 维护说明

本文件作为项目版本变更的统一记录点，所有重要变更都应在此记录。

### 记录规范

1. **版本格式**: 使用语义化版本号 (vX.Y.Z) 或阶段标识 (PX Week Y)
2. **日期格式**: YYYY-MM-DD
3. **分类标签**: 新增、修复、重构、改进、文档、测试等
4. **详细程度**: 包含关键变更点、影响范围、相关提交哈希

### 更新频率

- 每次版本发布必须更新
- 重大功能变更必须记录
- 文档结构调整必须说明

---

**最后更新**: 2025-12-18  
**下次审查**: 随版本发布更新
