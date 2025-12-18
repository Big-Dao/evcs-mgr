# 技术设计（收敛版）

> **简短描述**：给出 EVCS Manager 的关键技术选型与强制设计约束（非历史全集）。

**最后更新**: 2025-12-18  \
**维护者**: 架构团队  \
**状态**: 已发布

---

## 1. 文档定位

本文件只保留“必须一致”的设计口径：分层、边界、多租户、异步与可靠性、缓存与性能。

更详细的历史版本在归档中保留快照：

- 归档（历史与详细设计）：[docs/archive/documentation-docs-cleanup-2025-12-05/TECHNICAL-DESIGN.md](../archive/documentation-docs-cleanup-2025-12-05/TECHNICAL-DESIGN.md)

## 2. 设计基线（SSOT）

- 编码与架构规范（SSOT）：[docs/overview/PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md)
- 需求边界（收敛版）：[docs/architecture/requirements.md](requirements.md)
- 多租户异步上下文 RFC：[docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md](TENANT-CONTEXT-ASYNC-RFC.md)

## 3. 核心设计约束（强制）

### 3.1 分层与依赖

- 严格分层：Controller → Service → Domain/Repository → Entity
- 禁止跨边界：禁止跨服务数据库访问、禁止在 Controller 堆业务逻辑、禁止直接返回 Entity

### 3.2 多租户（含多层级分级治理）

EVCS 的多租户不仅要求“tenant_id 隔离”，还必须支持**多层级租户的分级管理**。

- 租户层级模型（L0/L1/L2+）、分级管理与跨层访问规则以需求为准：见 [docs/architecture/requirements.md](requirements.md)
- 异步/消息必须传播租户上下文：见 [docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md](TENANT-CONTEXT-ASYNC-RFC.md)

设计落地的最小约束：

- 默认严格隔离：业务数据按 `tenant_id` 过滤，禁止隐式跨租户写入
- 跨层访问必须“显式声明 + 只读优先 + 可审计”：上级查看下级数据必须具备明确开关/权限与审计
- 租户生命周期联动：禁用上级租户时下级租户应同步不可用（至少鉴权拒绝）

补充说明：需求侧的“行为约束与验收口径”见 [docs/architecture/requirements.md](requirements.md)；本节仅给出实现层需要统一的命名与语义约定。

#### 3.2.1 权限点（建议命名，允许按现有 RBAC 体系映射）

- `tenant:manage`：租户管理（创建/禁用/恢复/基础信息变更）
- `tenant:quota:manage`：配额与能力开关管理（含对子配额分配）
- `tenant:user:manage`：下级租户用户管理（创建/重置/禁用/角色绑定）
- `tenant:descendants:read`：跨层只读访问（显式包含下级/后代范围；作为“范围扩展”的额外 gate）
- `tenant:descendants:write`：跨层写入（默认禁止，仅在特批场景开放）
- `audit:read`：审计日志查询

约束：

- 下级租户不得拥有任何“向上管理/向上访问”权限（无论角色名称如何）。
- `tenant:descendants:write` 默认为关闭；若开启，必须采用专用管理接口并强制审计。

#### 3.2.2 API 语义约定（不绑定具体路径，但绑定参数与语义）

1. **租户管理接口（L0/L1 管理能力）**
   - 语义：创建子租户、禁用/恢复子租户、调整配额/能力开关、指定/重置子租户管理员。
   - 约束：只能对“本租户的后代租户”生效；不得对上级/兄弟租户生效。
2. **业务查询接口（站点/设备/订单/支付等）**
    - 默认：仅返回“当前租户”的数据。
    - 若支持跨层只读：必须通过显式参数开启范围，统一采用：
       - `tenantScope=SELF|SELF_AND_DESCENDANTS`
    - 参数语义（强制一致）：
       - `SELF`：仅当前租户
       - `SELF_AND_DESCENDANTS`：当前租户 + 后代租户（L2+ 全树，非仅子级）
    - 鉴权约束（强制）：
       - 业务资源权限（例如 `station:list`/`charger:list` 等）必须满足
       - 当且仅当 `tenantScope=SELF_AND_DESCENDANTS` 时，额外要求具备 `tenant:descendants:read`（范围扩展 gate）
    - 范围解析与过滤落地（强制一致）：
       - `SELF_AND_DESCENDANTS` 仅允许包含“当前租户的后代租户”，禁止包含上级/兄弟租户。
       - 落库过滤口径统一为：`tenant_id IN (allowedTenantIds)`；其中 `allowedTenantIds` 由“租户服务/统一租户组件”解析得到，并建议按 `(tenantId -> descendants)` 做缓存（TTL/上限保护）。
    - 审计约束（强制）：
       - 当且仅当实际启用跨层范围（`tenantScope=SELF_AND_DESCENDANTS`）时，必须记录审计（字段基线见 3.2.3）。
    - 错误处理（建议统一）：
       - 参数非法（未知枚举/空值等）：返回 400（或业务错误码）
       - 无权限启用跨层范围：返回 403（或业务错误码）；禁止“静默降级”为 `SELF`
    - 性能与安全（强制）：
       - 跨层查询必须分页，且需要合理的上限保护；导出类接口必须有额外的权限与审计。
3. **业务写接口（创建/更新/删除）**
   - 默认：只允许写入当前租户的数据。
   - 跨层写入：默认禁止；若确需开放，必须：专用接口 + 强鉴权 + 显式目标租户 + 完整审计 + 风险评估。

#### 3.2.3 审计字段最小集合（建议字段名）

只要发生以下任一情况，必须写审计日志：

- 跨层管理行为（创建/禁用/恢复子租户、重置子租户管理员、调整配额/能力开关）。
- 跨层只读访问（`tenantScope=SELF_AND_DESCENDANTS`）。
- 跨层写入（如被特批开放）。

审计日志最小字段建议包含：

- `operatorTenantId`：操作者所属租户
- `operatorUserId`：操作者用户 ID
- `targetTenantId`：被管理/被访问的目标租户
- `action`：动作类型（如 `TENANT_DISABLE`、`QUOTA_UPDATE`、`READ_DESCENDANTS`）
- `requestId`：请求 ID/链路追踪 ID
- `result`：成功/失败与失败原因（必要时含错误码）
- `timestamp`：操作时间
- `detail`：变更前后差异（脱敏后）

### 3.3 异步与消息（可靠性）

- 统一封装消息发布/消费；禁止裸线程与无上下文任务提交
- 幂等优先：消费端必须可重试、可观测，必要时配置死信与告警

### 3.4 缓存与性能

- 明确 TTL、失效策略与指标；避免 N+1 查询
- 列表接口必须分页；关键查询需要可解释的索引策略

## 4. 相关入口

- 文档统一入口：[docs/DOCUMENTATION-INDEX.md](../DOCUMENTATION-INDEX.md)
