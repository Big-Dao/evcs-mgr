# EVCS 项目安全基线（SECURITY-BASELINE）

> **版本**: v1.0 | **最后更新**: 2026-07-29 | **维护者**: 架构 / 安全团队 | **状态**: 活跃
>
> **用途**: 统一规范所有微服务的安全姿态，防止模块间安全配置漂移。
> 本文件为"单一来源"（SSOT）安全基线，各服务的安全配置必须符合本基线。

---

## 1. 基线目标

EVCS 是多租户充电站管理平台，**租户数据隔离**与**认证授权**是架构生命线。本基线确保：

1. **统一鉴权入口**：网关强制鉴权，下游服务做防御纵深。
2. **无弱密钥运行**：JWT 密钥必填、长度 ≥ 32 字符、禁止硬编码默认。
3. **一致的安全姿态**：每个服务都有 SecurityConfig + JWT filter + 方法安全。
4. **无 permitAll 调试残留**：生产代码禁止 `.anyRequest().permitAll()` 调试模式。

---

## 2. 强制规则（MUST）

### 2.1 网关（evcs-gateway）—— 入口鉴权

| 规则 | 说明 |
|------|------|
| 网关强制鉴权 | 无有效 Bearer JWT 返回 401（白名单除外）|
| tenant/user 上下文只从 JWT 派生 | 禁止信任客户端直接传入的 `X-Tenant-Id` 等头；默认剥离客户端同名伪造头 |
| JWT 密钥外置 | 网关存在**两个** JWT 密钥注入点，均无默认值（见 §3）：① 鉴权用 `jwt.secret`（`JwtServerAuthenticationConverter`/`JwtUtil`）；② 上下文头注入用 `evcs.gateway.security.context-headers.jwt-secret`（`ContextHeadersGlobalFilter`）。二者必须使用同一密钥来源，避免漂移 |
| 白名单最小化 | 仅登录/刷新/健康检查/文档放行；禁止 `/actuator` 前缀匹配（只放行 health/info）；白名单变更需代码评审 |

### 2.2 下游服务（evcs-auth/station/payment/order/tenant/protocol/monitoring）—— 防御纵深

| 规则 | 说明 |
|------|------|
| 必须有 SecurityConfig | `@EnableWebSecurity` + `SecurityFilterChain` bean |
| 必须有 JWT 过滤器 | 在 `UsernamePasswordAuthenticationFilter` 之前插入，校验 JWT 并构造 Authentication |
| 必须启用方法安全 | `@EnableMethodSecurity(prePostEnabled = true)`，支撑 `@PreAuthorize` |
| 无状态会话 | `SessionCreationPolicy.STATELESS` |
| 白名单最小化 | 仅健康检查/文档放行；业务端点一律 `authenticated()` |
| 禁止 permitAll 调试残留 | 生产代码禁止 `.anyRequest().permitAll()`；调试用白名单需可审计 |
| 禁止排除 SecurityAutoConfiguration | 除非有明确 RFC 说明，且必须有替代的 SecurityConfig |
| JWT 密钥外置 | `jwt.secret` 引用 `${JWT_SECRET}`，无默认值（见 §3）|

### 2.3 JWT 密钥管理

| 规则 | 说明 |
|------|------|
| 必填、无默认 | `@Value("${jwt.secret}")` 与 `@Value("${evcs.gateway.security.context-headers.jwt-secret}")` 均不得有默认值 |
| 长度 ≥ 32 字符 | 启动时校验，缺失或过短则启动失败 |
| 禁止硬编码 | 禁止在 Java 代码、`application.yml`、`docker-compose.yml`、K8s 清单中出现可猜测的字面量密钥 |
| 环境注入 | 本地开发通过 `.env`（gitignored）注入；生产由 CI/CD 或密钥管理服务（Vault / Sealed Secrets / External Secrets）注入 |
| 轮换 | 疑似泄露时立即轮换；K8s Secret 占位符必须由部署流水线覆盖 |

### 2.4 多租户上下文

| 规则 | 说明 |
|------|------|
| 只从可信源派生 | tenant/user 身份只能来自：网关转发的可信头（已从 JWT 派生）或请求中的 JWT Token |
| 禁止客户端参数注入 | **禁止** 从 `?tenantId=` 请求参数派生租户身份（防租户注入旁路）|
| 公开接口走 excludePathPatterns | 登录等无需租户上下文的接口应在 WebConfig 的 excludePathPatterns 中排除，而非读取参数 |

---

## 3. 禁止的架构违规（MUST NOT）

| 违规 | 后果 |
|------|------|
| `.anyRequest().permitAll()` 伪装成"已完成安全" | 虚假安全；必须用白名单 + authenticated |
| 注释掉 JWT 过滤器或 SecurityConfig | 虚假安全；必须接通或显式声明原因 |
| `@PreAuthorize` 存在但无 `@EnableMethodSecurity` | 死代码；注解不生效却给出受保护假象 |
| `exclude = {SecurityAutoConfiguration.class}` 绕过注解 | 虚假安全；必须接通方法安全或删除注解 |
| 硬编码 JWT 密钥 / 密码 / token | 凭证泄露；必须外置 |
| 从客户端请求参数读取 tenantId | 租户注入旁路 |

---

## 4. 各服务安全姿态路线图

### 4.1 已达标（符合基线）

| 服务 | SecurityConfig | JWT filter | @EnableMethodSecurity | 备注 |
|------|---------------|------------|-----------------------|------|
| evcs-gateway | ✅ GatewaySecurityConfig（reactive）| ✅ JwtServerAuthenticationConverter | — | 入口鉴权 |
| evcs-auth | ✅ | ✅ | ✅ | 完整 |
| evcs-station | ✅ | ✅ | ✅ | 完整 |
| evcs-payment | ✅ | ✅ | ✅ | 完整（2026-07-29 修复死 @PreAuthorize）|
| evcs-tenant | ✅ | ✅ | ✅ | 完整（2026-07-29 接通）|
| evcs-protocol | ✅ | ✅ | ✅ | 完整（2026-07-29 接通）|
| evcs-order | ✅ | ✅ | ✅ | 完整（2026-07-29 建设）|
| evcs-monitoring | ✅ | ✅ | ✅ | 完整（2026-07-29 建设）|

### 4.2 待达标（有安全依赖但未接通）

| 服务 | 当前状态 | 待办 |
|------|---------|------|
| （无）| — | — |

### 4.3 待建设（完全无安全）

| 服务 | 当前状态 | 待办 |
|------|---------|------|
| （无）| — | — |

---

## 5. 代码评审安全清单（Checklist）

评审涉及安全配置的变更时，逐项核对：

- [ ] 新服务是否包含 SecurityConfig + JWT filter + @EnableMethodSecurity
- [ ] JWT 密钥是否外置（无硬编码默认）
- [ ] 无 `.anyRequest().permitAll()` 调试残留
- [ ] 无注释掉的 JWT 过滤器或 SecurityConfig
- [ ] `@PreAuthorize` 所在服务已启用方法安全
- [ ] 未从客户端请求参数读取 tenantId
- [ ] 白名单路径最小化（仅健康检查/文档/登录）
- [ ] 无 `SecurityAutoConfiguration` 排除（或有 RFC 说明）

---

## 6. 相关文档

- 总体架构：`docs/architecture/architecture.md`
- 多租户异步上下文 RFC：`docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`
- 编码规范（SSOT）：`docs/overview/PROJECT-CODING-STANDARDS.md`
- AI 助手入口：`AGENTS.md`
- 本地开发密钥模板：`.env.example`

---

## 7. 变更记录

- 2026-07-29：初版创建，统一安全基线；修复 gateway/auth/station/payment 安全姿态；
  标记 order/tenant/protocol/monitoring 为待达标。

---

—— 结束（本文件为安全基线 SSOT，修改需经架构团队评审） ——
