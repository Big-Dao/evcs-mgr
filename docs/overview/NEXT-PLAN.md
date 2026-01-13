# EVCS Manager 下一步行动计划

> **版本**: v3.1 | **创建日期**: 2025-12-07 | **更新日期**: 2026-01-13 | **维护者**: 项目管理办公室（PMO）  
> **适用范围**: 开发团队、测试团队、运维团队

---

## 📊 项目当前状态

| 指标 | 状态 |
|------|------|
| **项目阶段** | 🟢 生产就绪 |
| **整体完成度** | 92% |
| **测试通过率** | 100%（168 用例） |
| **测试覆盖率** | 85%（目标 80%） |
| **K8s 服务状态** | ✅ 8/8 UP |

### K8s 服务清单（2026-01-13 验证）

| 服务 | 状态 | 服务 | 状态 |
|------|------|------|------|
| evcs-auth | ✅ UP | evcs-order | ✅ UP |
| evcs-gateway | ✅ UP | evcs-payment | ✅ UP |
| evcs-monitoring | ✅ UP | evcs-protocol | ✅ UP |
| evcs-station | ✅ UP | evcs-tenant | ✅ UP |

---

## ✅ 近期完成

### 2026-01-13
- Payment 服务健康检查修复（自引用循环问题）
- 监控聚合 API（`/api/monitoring/versions`）
- K8s 部署脚本 WSL 适配
- ConfigMap 热更新脚本（`k8s/update-config-repo.sh`）

### 2025-12-22
- 多层级租户治理 Part 1（递归禁用、数据隔离修复、审计基础）

### 2025-12-21
- 按枪口启停落库闭环
- 整桩启停入口收敛

### 2025-12-14
- 代码 TODO 清零（29/30 完成，详见[归档](../archive/todo-tracking-2026-01-13/)）
- 协议事件闭环增强

---

## 🎯 待办事项

> 原 `TODO-ITEMS-TRACKING.md` 已合并至此。剩余 3 项功能规划如下。

### 🔴 P0 - 关键功能（Week 1-2）

#### 1. 多层级租户治理（Part 2）

| 任务 | 状态 | 预计工时 |
|------|------|---------|
| 实现"上级管理下级"能力边界（用户/配额/能力开关）+ 审计日志 | ⏳ | 3天 |
| 集成测试：跨层只读、越权拒绝、禁用联动、异步上下文传播 | ⏳ | 2天 |

**需求基线**: [requirements.md](../architecture/requirements.md) / [TENANT-CONTEXT-ASYNC-RFC.md](../architecture/TENANT-CONTEXT-ASYNC-RFC.md)

#### 2. 协议栈真实集成

| 任务 | 文件 | 状态 | 预计工时 |
|------|------|------|---------|
| 集成真实协议栈 `invokeStartProtocol` | `ChargerServiceImpl.java:584` | ⏳ | 5天 |

#### 3. 站点/桩管理增强

| 任务 | 状态 | 预计工时 |
|------|------|---------|
| 资源变更审计（新增/删除/启停） | ⏳ | 1天 |
| 地理位置合法性校验 | ⏳ | 0.5天 |
| ChargerStatusManager 状态机 | ⏳ | 2天 |

---

### 🟠 P1 - 代码质量（Week 3）

| 任务 | 目标 |
|------|------|
| 完善 `AuthServiceTestTemplate` / `PaymentServiceTestTemplate` | 覆盖率 85% → 90% |

---

### 🟡 P2 - 性能与监控（Week 4-6）

| 任务 | 目标指标 |
|------|----------|
| 性能压测（JMeter/Gatling） | 订单创建 ≥500 TPS，查询 ≥1000 TPS |
| Grafana Dashboard | P99 < 200ms |
| Prometheus 业务指标 + 告警规则 | 核心指标 100% 覆盖 |

---

### 🟢 P3 - C 端用户管理模块（Week 7-28）

**目标**: 建立独立的 C 端用户模型，支撑积分、优惠券、会员、营销等业务场景。

**详细规划**: [evcs-user 模块 RFC](../architecture/EVCS-USER-MODULE-RFC.md)

| 阶段 | 周期 | 核心功能 |
|------|------|----------|
| Phase 1-2 | W7-10 | 基础用户管理 + 钱包与积分系统 |
| Phase 3-5 | W11-14 | 优惠券 + 客诉 + 会员体系 |
| Phase 6-8 | W15-18 | 便捷功能 + 运营增长 + 用户群组 |
| Phase 9 | W19-22 | 用户画像与标签（RFM/分群） |
| Phase 10-13 | W23-28 | 营销活动/权益包/任务/评价 |

**规模**: 33 张表 | 23 组 API | 22 周工期

---

## 📅 周计划

| 周 | 重点 | 任务 |
|----|------|------|
| **W1**（01-13） | 多层级租户 | 能力边界 + 审计日志 + 集成测试 |
| **W2** | 协议栈 + 站点 | `invokeStartProtocol` 集成、审计、状态机 |
| **W3** | 代码质量 | 测试模板完善、覆盖率提升 |
| **W4** | 性能 | 压测、基线建立、Dashboard |
| **W5-6** | 监控 + 文档 | Prometheus/Grafana、API 文档、运维手册 |
| **W7-28** | evcs-user | C 端用户模块开发（详见 [RFC](../architecture/EVCS-USER-MODULE-RFC.md)） |

---

## ✔️ 验收标准

| 类别 | 标准 |
|------|------|
| **功能** | P0 任务 100% 完成；多层级租户可管理、可审计 |
| **质量** | 测试覆盖率 ≥ 90%；质量扫描无严重问题 |
| **性能** | TPS 达标；P99 < 200ms |
| **运维** | 监控/告警完善；文档可用 |

---

## 📊 风险与应对

| 风险 | 等级 | 应对 |
|------|------|------|
| 协议栈复杂度超预期 | 🔴 | 分阶段：先 mock → 再真实对接 |
| 压测资源不足 | 🟠 | 本地环境 / 申请云资源 |
| 覆盖率提升困难 | 🟠 | 聚焦核心逻辑，工具类适度降低要求 |

---

## 🔗 相关文档

- [需求概览](../architecture/requirements.md)
- [编码规范](PROJECT-CODING-STANDARDS.md)
- [部署指南](../deployment/DEPLOYMENT-GUIDE.md)
- [测试指南](../testing/UNIFIED-TESTING-GUIDE.md)
- [evcs-user 模块 RFC](../architecture/EVCS-USER-MODULE-RFC.md)
- [TODO 归档](../archive/todo-tracking-2026-01-13/)

---

## 📝 更新记录

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-01-13 | v3.1 | 新增 P3 evcs-user 模块规划（22周，详见 RFC） |
| 2026-01-13 | v3.0 | 合并 TODO-ITEMS-TRACKING.md；精简重复内容 |
| 2026-01-13 | v2.1 | 刷新 K8s 状态；更新完成项 |
| 2025-12-07 | v2.0 | 初始版本 |
