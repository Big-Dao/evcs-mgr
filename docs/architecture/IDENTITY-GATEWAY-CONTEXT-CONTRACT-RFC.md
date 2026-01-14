# 身份与网关统一契约 RFC

> 一句话说明：统一 EVCS 系统的身份认证、JWT Claims、跨服务 Header 透传与网关注入边界，支撑 B 端（平台/租户人员）与 C 端（充电用户）长期演进。

**最后更新**: 2026-01-14  \
**维护者**: 技术负责人 / 架构组  \
**状态**: 已发布

---

## 目录

- [1. 背景与问题](#1-背景与问题)
- [2. 目标与非目标](#2-目标与非目标)
- [3. 术语与范围](#3-术语与范围)
- [4. 决策概要](#4-决策概要)
- [5. 方案对比（ADR）](#5-方案对比adr)
- [6. 最终决策](#6-最终决策)
- [7. 规范（Normative Requirements）](#7-规范normative-requirements)
- [8. 安全与合规考虑](#8-安全与合规考虑)
- [9. 可观测性与排障](#9-可观测性与排障)
- [10. 迁移与里程碑](#10-迁移与里程碑)
- [11. 风险与缓解](#11-风险与缓解)
- [12. 附录 A：JWT Claims 表](#12-附录-ajwt-claims-表)
- [13. 附录 B：Headers 表](#13-附录-bheaders-表)
- [14. 附录 C：网关校验规则表](#14-附录-c网关校验规则表)
- [15. 附录 D：/userinfo 响应与缓存策略表](#15-附录-duserinfo-响应与缓存策略表)
- [16. 附录 E：示例链路](#16-附录-e示例链路)

---

## 1. 背景与问题

系统存在 B 端（平台/租户人员）与 C 端（充电用户）的双端形态，且后端微服务采用多租户隔离（租户上下文、租户层级、数据范围等）。为了支持长期演进（第三方 OAuth、多端登录、回调入口治理、可观测性与安全审计），需要一份统一的“身份与网关契约”，明确以下边界：

- JWT 的最小 Claims 集合与命名。
- C/B 两套身份体系的硬隔离策略（避免 token 混用）。
- 网关与业务服务在“验签、注入、兜底解析、审计”的责任划分。
- 跨服务调用的 Header 透传语义与信任边界。

本 RFC 不描述具体代码实现细节，只定义必须遵守的契约与决策。

相关权威文档（SSOT）：

- 项目编码与架构规范（SSOT）：[docs/overview/PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md)
- 多租户异步上下文 RFC（SSOT）：[docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md](TENANT-CONTEXT-ASYNC-RFC.md)
- C 端用户模块 RFC（如需对齐账号模型）：[docs/architecture/EVCS-USER-MODULE-RFC.md](EVCS-USER-MODULE-RFC.md)

## 2. 目标与非目标

### 2.1 目标

- 统一身份契约：JWT Claims、标准 Headers、`/userinfo` 语义。
- 明确网关责任：统一验签与注入为主，服务端兜底解析为辅。
- 实现 C/B 完全分开：两套 auth 服务与两套 token 体系硬隔离，但允许共享基础组件。
- 支持长期演进：第三方 OAuth、设备会话、限流风控、支付回调入口治理、排障链路一致。

### 2.2 非目标

- 不定义数据库表结构细节（仅定义接口与契约所需字段）。
- 不指定使用 Spring Authorization Server 或自研 JWT 的具体实现方式（允许演进）。
- 不覆盖业务域流程细节（如充电协议、结算状态机等），仅覆盖身份与入口治理。

## 3. 术语与范围

### 3.1 用户类型

- B 端用户：平台/租户人员，主要访问管理类 API 与后台能力。
- C 端用户：充电用户（App/小程序），主要访问充电与订单支付相关 API。

### 3.2 统一入口

- 网关：对外统一入口，负责对外路由、鉴权、限流、熔断、灰度、可观测性。
- 服务：微服务实例，负责业务逻辑与数据一致性。

### 3.3 规范用语

本 RFC 使用以下词语表示强制性：

- MUST：必须。
- MUST NOT：禁止。
- SHOULD：建议。
- MAY：可选。

## 4. 决策概要

- C/B 身份体系完全分开：独立 auth 服务、独立密钥、独立 `iss/aud`，从机制上杜绝 token 混用。
- JWT 不承载全量细粒度权限：token 放最小身份与上下文，细粒度权限通过 `GET /userinfo` 获取并缓存（短 TTL 或 pv 机制）。
- 网关统一验签与 Header 注入：业务服务默认以网关注入 headers 为准，保留兜底解析能力（排障/直连/回放场景）。
- `/internal/api/**` 不对外暴露：网关运行时拦截 + 启动时路由 fail-fast 双保险。
- 回调入口（如支付回调）走网关专用路由：避免通用路由规则（例如 StripPrefix）破坏回调路径与验签。

## 5. 方案对比（ADR）

### 5.1 C 端 auth 与 B 端 auth：完全分开 vs 混用

| 维度 | 完全分开（推荐） | 混用（不推荐） |
|------|------------------|----------------|
| 解耦与故障隔离 | 高：C 端洪峰/风控/第三方依赖不影响 B 端 | 低：高并发或外部依赖可能影响后台 |
| 安全与越权风险 | 低：不同 `iss/aud/key` 天然隔离 | 高：最常见问题是 token 误用导致越权 |
| 演进速度 | 高：C 端可独立引入 OAuth/设备会话/反作弊 | 中：不同需求在同一服务冲突 |
| 研发/运维成本 | 中：两套部署与监控，但组件可共享 | 低：单服务维护 |
| 合规与审计 | 清晰：按用户域分开审计策略 | 容易模糊：策略与日志混杂 |

结论：以“服务完全分开 + 组件共享”为基线。

### 5.2 验签与上下文注入：网关统一 vs 服务各自处理

| 维度 | 网关统一（推荐） | 服务各自处理（不推荐） |
|------|------------------|--------------------------|
| 一致性 | 高：单点策略，避免漂移 | 低：每个服务实现容易分叉 |
| 可观测性与定位 | 高：入口层集中打点与审计 | 中：需要在多个服务拼接 |
| 性能 | 可控：统一缓存 JWK/公钥 | 难控：重复解析与验签 |
| 安全边界 | 清晰：服务信任网关注入 | 复杂：信任边界分散 |

结论：网关验签与注入为主，服务端兜底为辅。

### 5.3 权限放入 token vs 通过 userinfo 拉取

| 维度 | 权限进 token（不推荐作为默认） | `/userinfo` 拉取（推荐） |
|------|-------------------------------|--------------------------|
| 实时性（撤权） | 低：需等 token 过期 | 高：短 TTL 或 pv 可快速生效 |
| token 体积 | 易膨胀，影响链路 | 稳定，可控 |
| 权限演进 | 难：新增权限点需要谨慎兼容 | 易：服务端扩展返回字段 |
| 泄露影响 | 大：权限与信息更多暴露 | 小：token 保持最小 |

结论：token 最小化，权限通过 `/userinfo` 获取并缓存。

## 6. 最终决策

- C 端 auth 与 B 端 auth 服务完全分离。
- 两套 token 完全隔离：不同 `iss`、不同 `aud`、不同签名 key 与轮换策略。
- 网关作为对外统一鉴权与上下文注入点。
- 服务端必须支持兜底解析 `Authorization`，但默认优先使用网关注入的标准 headers。
- 细粒度权限不进入 JWT 默认契约；以 `/userinfo` 为权威来源，配合短 TTL 缓存与 pv 策略。

## 7. 规范（Normative Requirements）

### 7.1 JWT 发行与校验

#### 7.1.1 发行方与隔离

- C 端与 B 端 MUST 使用不同的 `iss`。
- C 端与 B 端 MUST 使用不同的 `aud`。
- C 端与 B 端 MUST 使用不同的签名 key（或不同的 JWK Set），并支持独立轮换。
- 网关 MUST 同时校验 `iss` 与 `aud`，不满足即拒绝。

推荐默认值（可按环境配置）：

- B 端：`iss=evcs-auth-b`，`aud=evcs-b-mobile`
- C 端：`iss=evcs-auth-c`，`aud=evcs-c-mobile`

#### 7.1.2 JWT 最小 claims

- JWT MUST 包含：`sub`、`tenantId`、`userType`、`jti`、`iat`、`exp`。
- JWT SHOULD 包含：`username`（展示与审计）。
- JWT MAY 包含：`deviceId`（C 端设备会话）、`roles`（少量角色，用于粗粒度判断）、`pv`（权限版本号）。
- JWT MUST NOT 包含：全量 `permissionCodes`（默认策略）。

详见附录 A。

#### 7.1.3 刷新与撤销（建议）

- C 端 SHOULD 采用 refresh token 机制，并维护会话/黑名单（用于强制退出与风控）。
- B 端 MAY 采用 refresh token 或更严格的 MFA/设备绑定。
- `jti` MUST 用于支持撤销、审计与幂等关联。

### 7.2 标准 Headers 与信任边界

#### 7.2.1 标准 Header 集合

跨网关与跨服务调用的标准 headers 详见附录 B。

#### 7.2.2 头部来源与覆盖规则

- `X-User-Id` 与 `X-Tenant-Id` MUST 由网关从 token 派生并覆盖写入。
- 业务服务 MUST NOT 信任客户端直接传入的 `X-User-Id/X-Tenant-Id`。
- 服务间调用（Feign/HTTP）SHOULD 透传 `X-Trace-Id` 与 `X-Request-Id`，并在缺失时补齐。

#### 7.2.3 服务端兜底解析

- 若标准 headers 缺失，服务端 MAY 从 `Authorization: Bearer <token>` 进行兜底解析（用于排障、回放、直连测试）。
- 服务端兜底解析 MUST 遵循与网关一致的 `iss/aud` 校验策略。

### 7.3 `/userinfo` 契约与缓存策略

#### 7.3.1 语义

- `/userinfo` MUST 返回：用户基础信息、租户信息、角色列表、权限列表（如使用细粒度权限）。
- `/userinfo` MUST 返回 `pv`（或等价字段）用于权限缓存失效。

#### 7.3.2 缓存

- 客户端/网关 MAY 对 `/userinfo` 结果缓存，TTL SHOULD 为 1-5 分钟。
- 遇到 403 或权限不足提示时，客户端 SHOULD 强制刷新 `/userinfo`。
- 当用户角色/权限变更时，服务端 MUST bump `pv`（或使缓存失效的等价策略）。

详见附录 D。

### 7.4 内部接口不对外暴露（/internal/api）

- 内部接口路径前缀 MUST 使用 `/internal/api/**`。
- 网关 MUST 对 `/internal/api/**` 进行运行时拦截并返回 404。
- 网关 MUST 在启动时扫描 routes，若任何 route 的 path predicate 覆盖 `/internal/api/**`，启动 MUST 失败（fail-fast）。

### 7.5 回调入口治理（以支付回调为代表）

- 回调入口 SHOULD 走网关专用路由（独立 path predicate），避免通用业务路由规则破坏路径。
- 网关 MUST 保持回调请求 body 原样转发（不改写）。
- payment 服务 MUST 在回调入口处完成验签/解密、幂等处理与状态机推进；耗时操作 MUST 异步化。

## 8. 安全与合规考虑

- token 隔离：不同 `iss/aud/key` 是强制安全边界。
- 防越权：网关路由 MUST 与 `aud/userType` 绑定，禁止 C 端 token 访问管理类 API。
- 防伪造：`X-User-Id/X-Tenant-Id` 必须由网关覆盖写入，避免客户端伪造。
- 防重放：`jti` + 幂等键（对敏感写操作）用于抵御重放与重复提交。

## 9. 可观测性与排障

- 网关 MUST 生成或透传 `X-Trace-Id` 与 `X-Request-Id`，并写入日志/响应头。
- 业务服务 SHOULD 将 `tenantId/userId/userType/jti` 写入结构化日志字段（避免仅在 message 拼接）。
- 服务间调用 SHOULD 透传 trace/request id，保证跨服务检索一致。

## 10. 迁移与里程碑

### 10.1 阶段 1：契约定稿与网关改造

- 定稿本 RFC。
- 网关实现统一验签与标准 headers 注入。
- 业务服务保留兜底解析，但默认使用标准 headers。

### 10.2 阶段 2：C 端 auth 服务落地

- 新增 C 端 auth：验证码登录、会话管理、基础风控与限流。
- 引入第三方 OAuth（微信/支付宝/Apple）与账号绑定模型。

### 10.3 阶段 3：权限与缓存策略落地

- B 端 `/userinfo` 对齐角色/权限输出与 `pv`。
- 前端/网关实现 `/userinfo` 缓存与强制刷新策略。

## 11. 风险与缓解

- 风险：网关成为单点。
  - 缓解：网关集群化、限流熔断、灰度发布与回滚预案。
- 风险：历史调用依赖客户端传 `X-User-Id/X-Tenant-Id`。
  - 缓解：网关覆盖写入，逐步下线客户端直传，服务端保留短期兼容与告警。
- 风险：权限变更不及时生效。
  - 缓解：短 TTL + `pv` 机制；对高风险操作强制 refresh。

## 12. 附录 A：JWT Claims 表

| 字段 | 类型 | 说明 | 来源 | 适用 |
|------|------|------|------|------|
| `iss` | String | 发行方标识 | 签发器写入 | C/B（不同值） |
| `aud` | String | 受众标识 | 签发器写入 | C/B（不同值） |
| `sub` | String/Long | 用户 ID | 签发器写入 | C/B |
| `tenantId` | Long | 租户 ID | 签发器写入 | C/B |
| `userType` | String | 用户类型（B/C） | 签发器写入 | C/B |
| `jti` | String | Token 唯一标识 | 签发器写入 | C/B |
| `iat` | Instant | 签发时间 | 签发器写入 | C/B |
| `exp` | Instant | 过期时间 | 签发器写入 | C/B |
| `username` | String | 用户名（展示/审计） | 签发器写入 | C/B（建议） |
| `pv` | Long | 权限版本号 | 签发器写入 | C/B（建议） |
| `roles` | List<String> | 角色列表（粗粒度） | 签发器写入 | 可选 |
| `deviceId` | String | 设备标识（会话/风控） | 签发器写入 | C（可选） |

## 13. 附录 B：Headers 表

| Header | 谁写入 | 谁读取 | 语义 | 客户端可覆盖 |
|--------|--------|--------|------|--------------|
| `Authorization` | 客户端 | 网关/服务兜底 | `Bearer <jwt>` | 是 |
| `X-Trace-Id` | 网关/服务（缺失时生成） | 全链路 | Trace 关联 ID | 允许输入但不可信 |
| `X-Request-Id` | 网关/服务（缺失时生成） | 全链路 | 请求关联 ID | 允许输入但不可信 |
| `X-Tenant-Id` | 网关（从 token 派生并覆盖） | 业务服务 | 租户上下文 | 否（必须覆盖） |
| `X-User-Id` | 网关（从 token 派生并覆盖） | 业务服务 | 用户上下文 | 否（必须覆盖） |
| `X-User-Type` | 网关（从 token 派生并覆盖） | 业务服务 | B/C 类型 | 否（必须覆盖） |
| `X-Tenant-Type` | 网关/服务派生 | 业务服务 | 租户类型 | 否 |
| `X-Tenant-Ancestors` | 网关/服务派生 | 业务服务 | 租户祖先链 | 否 |

## 14. 附录 C：网关校验规则表

| 规则 | 校验项 | 失败动作 | 关键日志字段 |
|------|--------|----------|--------------|
| JWT 验签 | 签名、公钥/JWK | 401 | `traceId/requestId` |
| `iss` 校验 | `iss` 必须在允许列表 | 401 | `iss/traceId/requestId` |
| `aud` 校验 | `aud` 必须与路由匹配 | 403 | `aud/path/traceId/requestId` |
| 过期校验 | `exp` 未过期 | 401 | `sub/jti/traceId/requestId` |
| 内部接口拦截 | 路径前缀 `/internal/api/` | 404 | `path/traceId/requestId` |
| 启动时守卫 | routes 不得匹配 `/internal/api/**` | 启动失败 | routeId/pathPredicate |

## 15. 附录 D：/userinfo 响应与缓存策略表

| 字段 | 说明 | 建议缓存 | pv 策略 |
|------|------|----------|--------|
| `id` | 用户 ID | TTL ≤ token 剩余有效期 | 随 token |
| `tenantId` | 租户 ID | 切租户必须清空 | 随 token |
| `username` | 用户名 | 1-5 分钟 | pv 不变 |
| `realName` | 真实姓名 | 1-5 分钟 | pv 不变 |
| `status` | 账号状态 | 建议更短 TTL 或启动刷新 | 停用需尽快生效 |
| `userType` | B/C | 1-5 分钟 | 随 token |
| `roleCodes` | 角色列表 | 1-5 分钟 | 变更 bump pv |
| `permissionCodes` | 权限列表 | 1-5 分钟 | 变更 bump pv |
| `pv` | 权限版本号 | 与权限同 TTL | 权限变更必增 |

## 16. 附录 E：示例链路

### 16.1 示例链路 A：B 端移动端登录与访问业务 API

1. 客户端调用 `/api/auth-b/login`（或网关路由到 B 端 auth 的登录端点）。
2. B 端 auth 签发 JWT（`iss=evcs-auth-b`，`aud=evcs-b-mobile`），返回 `accessToken`（及可选 refresh token）。
3. 客户端后续请求携带 `Authorization: Bearer <token>`。
4. 网关验签并校验 `iss/aud/exp`，注入 `X-User-Id/X-Tenant-Id/X-User-Type`，生成/透传 `X-Trace-Id/X-Request-Id`。
5. 业务服务优先使用标准 headers 建立 TenantContext；headers 缺失时从 `Authorization` 兜底解析。
6. 客户端按需调用 `/api/auth/userinfo` 获取 `roleCodes/permissionCodes/pv` 并缓存。

### 16.2 示例链路 B：服务间调用（Feign）透传上下文

1. 外部请求经网关进入服务 A，服务 A 获取 `X-Trace-Id/X-Request-Id` 与用户/租户 headers。
2. 服务 A 建立 TenantContext 与日志 MDC。
3. 服务 A 通过 Feign 调用服务 B，透传 `X-Trace-Id/X-Request-Id/X-User-Id/X-Tenant-Id/X-User-Type`。
4. 服务 B 使用透传 headers 建立上下文，保证日志与审计字段一致。
5. 异步/线程池场景需要确保 TenantContext 与 trace 信息可传播（详见多租户异步上下文 RFC）。

### 16.3 示例链路 C：支付回调经网关专用路由

1. 第三方支付平台回调网关公开地址（例如 `/callback/payment/wechat`）。
2. 网关路由到 payment 服务回调入口（专用 route，不应用通用 StripPrefix）。
3. 网关保持 body 原样转发，并保留真实源 IP（`X-Forwarded-For`）。
4. payment 服务完成验签/解密与幂等处理，推进支付状态。
5. payment 服务异步通知 order 服务更新订单状态（事件或可靠回调）。
6. 排障通过 `X-Trace-Id/X-Request-Id` 串联网关与下游日志。
