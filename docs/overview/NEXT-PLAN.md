# EVCS Manager 下一步行动计划

> **版本**: v3.4 | **创建日期**: 2025-12-07 | **更新日期**: 2026-02-10 | **维护者**: 项目管理办公室（PMO）
> **适用范围**: 开发团队、测试团队、运维团队

---

## 1. 项目当前状态（口径）

| 指标 | 状态 |
|------|------|
| **项目阶段** | 生产就绪（按计划目标） |
| **整体完成度** | 以里程碑/看板为准（不在 Markdown 中硬编码百分比） |
| **测试通过率** | 以 CI 测试报告为准 |
| **测试覆盖率** | 以 CI 覆盖率报告为准 |
| **运行态服务状态（K8s/Compose）** | 以目标环境的命令输出为准（不在本计划中维护 UP 数量） |

### 1.1 如何验证（本地/环境命令）

```bash
# 本地构建与测试（以 Gradle 报告为准）
./gradlew test

# 若项目启用 JaCoCo 等覆盖率报告，以任务输出为准
./gradlew jacocoTestReport

# 运行态（K8s）示例：以目标 namespace 与上下文为准
kubectl get deploy -A | grep evcs || true
kubectl get pods -A | grep evcs || true
```

### 1.2 权威来源

- 服务主标识与端口： [docs/architecture/SERVICE-IDENTIFIERS-AND-PORTS.md](../architecture/SERVICE-IDENTIFIERS-AND-PORTS.md)
- Kubernetes 资源：`k8s/deployments/**/*.yaml`
- Docker Compose： [docker-compose.yml](../../docker-compose.yml)

---

## 2. 近期完成

### 2.1 2026-02-10
- P0 关键风险修复（5项）
  - 修复协议栈 fallback 返回 true 逻辑（`ChargerServiceImpl.java:549-599`）
  - 补充缺失的 `@PreAuthorize` 权限注解（4 Controller + 3 build.gradle）
  - 引入 Redisson 分布式锁保护充电操作（`ChargerServiceImpl.java:323-452`）
  - 引入 Resilience4j 熔断/限流机制（3 配置类 + YAML）
  - 统一异常处理规范（BaseException + 4 特化异常 + GlobalExceptionHandler 增强）
- 多层级租户治理 Part 2（审计日志 + 配额管理）
  - `TenantAuditLog` 实体 + Mapper + Service + Controller
  - `ITenantQuotaService` 配额检查与管理
  - 数据库迁移 `V20260210__create_tenant_audit_log.sql`
- 清理代码 TODO/FIXME 标记

### 2.2 2026-01-13
- Payment 服务健康检查修复（自引用循环问题）
- 监控聚合 API（`/api/monitoring/versions`）
- K8s 部署脚本 WSL 适配
- ConfigMap 热更新脚本（`k8s/update-config-repo.sh`）

### 2.3 2025-12-22
- 多层级租户治理 Part 1（递归禁用、数据隔离修复、审计基础）

### 2.3 2025-12-21
- 按枪口启停落库闭环
- 整桩启停入口收敛

### 2.4 2025-12-14
- 代码 TODO 清零（29/30 完成，详见[归档](../archive/todo-tracking-2026-01-13/)）
- 协议事件闭环增强

---

## 3. 待办事项

> 原 `TODO-ITEMS-TRACKING.md` 已合并至此。剩余功能规划如下。
> 
> **风险审计报告**: [RISK-AUDIT-REPORT.md](../architecture/RISK-AUDIT-REPORT.md) — 识别出 3 项 P0 严重风险需立即处理

### 3.1 P0 - 关键功能 + 风险修复（Week 1-2）

#### 3.1.1 架构风险修复

| 任务 | 优先级 | 状态 | 预计工时 |
|------|----------|------|---------|
| 引入 Redisson 分布式锁（充电桩启停/支付） | P0 | ✅ 已完成 | 2 天 |
| 引入 Resilience4j 熔断/限流 | P0 | ✅ 已完成 | 2 天 |
| 补充缺失的 `@PreAuthorize` 权限注解 | P0 | ✅ 已完成 | 1 天 |
| 修复协议栈 fallback 返回 true 问题 | P0 | ✅ 已完成 | 1 天 |
| 统一异常处理规范 | P1 | ✅ 已完成 | 1 天 |

**详细分析**: [系统架构风险审计报告](../architecture/RISK-AUDIT-REPORT.md)

#### 3.1.2 多层级租户治理（Part 2）

| 任务 | 状态 | 预计工时 |
|------|---------|----------|
| 实现"上级管理下级"能力边界（用户/配额/能力开关）+ 审计日志 | ✅ 已完成 | 3 天 |
| 集成测试：跨层只读、越权拒绝、禁用联动、异步上下文传播 | 待开始 | 2 天 |

**需求基线**: [requirements.md](../architecture/requirements.md) / [TENANT-CONTEXT-ASYNC-RFC.md](../architecture/TENANT-CONTEXT-ASYNC-RFC.md)

#### 3.1.3 协议栈真实集成

| 任务 | 文件 | 状态 | 预计工时 |
|------|------|------|---------|
| 集成真实协议栈 `invokeStartProtocol` | `ChargerServiceImpl.java:549-599` | ✅ 已完成（移除 fallback） | 5 天 |

#### 3.1.4 站点/桩管理增强

| 任务 | 状态 | 预计工时 |
|------|------|---------|
| 资源变更审计（新增/删除/启停） | 待开始 | 1 天 |
| 地理位置合法性校验 | 待开始 | 0.5 天 |
| ChargerStatusManager 状态机 | 待开始 | 2 天 |

---

### 3.2 P1 - 代码质量（Week 3）

| 任务 | 目标 |
|------|------|
| 完善 `AuthServiceTestTemplate` / `PaymentServiceTestTemplate` | 覆盖率 85% → 90% |

---

### 3.3 P2 - 性能与监控（Week 4-6）

| 任务 | 目标指标 |
|------|----------|
| 性能压测（JMeter/Gatling） | 订单创建 ≥500 TPS，查询 ≥1000 TPS |
| Grafana Dashboard | P99 < 200ms |
| Prometheus 业务指标 + 告警规则 | 核心指标 100% 覆盖 |

### 3.4 P2.5 - 海量数据处理（Week 6-10）

**目标**: 为高增长表实施 PostgreSQL 原生分区，建立完整的海量数据处理机制。

**详细规划**: [海量数据处理方案 RFC](../architecture/DATA-PARTITIONING-RFC.md)

| 阶段 | 周期 | 核心任务 |
|------|------|----------|
| Phase 1 | W6-7 | P0 表分区（7张：曲线点、行为事件、订单、支付、会话、流水） |
| Phase 2 | W7-8 | P1 表分区（9张：日志、消息、优惠券、签到等） |
| Phase 3 | W8-9 | 归档机制（对象存储集成、归档查询接口） |
| Phase 4 | W9-10 | 读写分离 + 缓存策略 + 慢查询治理 |

**分区优先级汇总**:

| 优先级 | 表数量 | 关键表 |
|--------|--------|--------|
| P0 必须 | 7 | `charger_connector_curve_point`, `charging_order`, `payment_order` |
| P1 建议 | 9 | `user_login_log`, `user_message`, `user_coupon` |
| P2 可选 | 6 | `complaint`, `reconciliation_task` |
| P3 无需 | 29 | 配置表、主数据表 |

**补充能力**:

| 能力 | 优先级 | 说明 |
|------|--------|------|
| 读写分离 | P1 | 报表/导出走只读副本 |
| 缓存策略 | P1 | Redis + 本地缓存分层 |
| 慢查询治理 | P0 | 强制时间范围、分页上限、超时控制 |
| 连接池优化 | P2 | HikariCP 调优、PgBouncer 预留 |
| 灾备恢复 | P1 | 每日备份、WAL 归档、PITR |

---

### 3.5 P3 - C 端用户管理模块（Week 7-28）

**目标**: 建立独立的 C 端用户模型，支撑积分、优惠券、会员、营销等业务场景。

**详细规划**: [evcs-user 模块 RFC v2.0](../architecture/EVCS-USER-MODULE-RFC.md)

| 阶段 | 周期 | 核心功能 |
|------|------|----------|
| Phase 1-2 | W7-10 | 基础用户管理（主表+7张扩展表）+ 钱包与积分系统 |
| Phase 3-5 | W11-14 | 优惠券 + 客诉 + 会员体系 |
| Phase 6-8 | W15-18 | 便捷功能 + 运营增长 + 用户群组 |
| Phase 9 | W19-22 | 用户画像与标签（RFM/分群） |
| Phase 10-13 | W23-28 | 营销活动/权益包/任务/评价 |

**表结构设计（v2.0 拆分方案）**:

| 表名 | 关系 | 用途 |
|------|------|------|
| `charging_user` | 主表 | 高频访问的核心信息（~25字段） |
| `user_identity` | 1:1 | 证件信息 |
| `user_contact` | 1:1 | 联系方式、紧急联系人 |
| `user_address` | 1:N | 收货/常用地址 |
| `user_profile_ext` | 1:1 | 职业信息、客服备注 |
| `user_preference` | 1:1 | 偏好设置、快捷引用 |
| `user_stats` | 1:1 | 统计数据（高频更新） |
| `user_lifecycle` | 1:1 | 生命周期节点 |

**规模**: 40+ 张表 | 60+ 组 API | 22 周工期

---

## 4. 周计划

| 周 | 重点 | 任务 |
|----|------|------|
| **W1**（01-13） | 多层级租户 | 能力边界 + 审计日志 + 集成测试 |
| **W2** | 协议栈 + 站点 | `invokeStartProtocol` 集成、审计、状态机 |
| **W3** | 代码质量 | 测试模板完善、覆盖率提升 |
| **W4** | 性能 | 压测、基线建立、Dashboard |
| **W5-6** | 监控 + 文档 | Prometheus/Grafana、API 文档、运维手册 |
| **W6-8** | 数据分区 | P0/P1 表分区、归档机制 |
| **W9-10** | 数据优化 | 读写分离、缓存策略、慢查询治理、灾备 |
| **W7-28** | evcs-user | C 端用户模块开发（详见 [用户 RFC](../architecture/EVCS-USER-MODULE-RFC.md)） |

---

## 5. 验收标准

| 类别 | 标准 |
|------|------|
| **功能** | P0 任务 100% 完成；多层级租户可管理、可审计 |
| **质量** | 测试覆盖率 ≥ 90%；质量扫描无严重问题 |
| **性能** | TPS 达标；P99 < 200ms |
| **运维** | 监控/告警完善；文档可用 |

---

## 6. 风险与应对

| 风险 | 等级 | 应对 |
|------|------|------|
| 协议栈复杂度超预期 | P0 | 分阶段：先 mock → 再真实对接 |
| 压测资源不足 | P1 | 本地环境 / 申请云资源 |
| 覆盖率提升困难 | P1 | 聚焦核心逻辑，工具类适度降低要求 |

---

## 7. 相关文档

- [需求概览](../architecture/requirements.md)
- [编码规范](PROJECT-CODING-STANDARDS.md)
- [部署指南](../deployment/DEPLOYMENT-GUIDE.md)
- [测试指南](../testing/UNIFIED-TESTING-GUIDE.md)
- [evcs-user 模块 RFC](../architecture/EVCS-USER-MODULE-RFC.md)
- [TODO 归档](../archive/todo-tracking-2026-01-13/)

---

## 8. 更新记录

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-02-10 | v3.4 | 统一异常处理规范完成（新增 5 项异常类 + GlobalExceptionHandler 增强）|
| 2026-02-10 | v3.3 | P0 关键风险修复完成（4项）、多层级租户治理 Part2 完成、代码 TODO 清零 |
| 2026-01-13 | v3.2 | 新增 P2.5 海量数据分区规划（详见 DATA-PARTITIONING-RFC.md） |
| 2026-01-13 | v3.1 | 新增 P3 evcs-user 模块规划（22 周，详见 RFC） |
| 2026-01-13 | v3.0 | 合并 TODO-ITEMS-TRACKING.md（已归档），精简重复内容 |
| 2026-01-13 | v2.1 | 刷新计划结构与近期完成项 |
| 2025-12-07 | v2.0 | 初始版本 |
