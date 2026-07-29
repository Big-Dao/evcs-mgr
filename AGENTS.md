# EVCS Manager — AGENTS.md（统一AI助手入口）

本文件统一规范所有 AI 编程助手（Claude Code / GitHub Copilot / CodeX / 其它 LLM 工作流）在仓库中的行为，并建立“单一来源”（Single Source of Truth，SSOT）映射，避免重复维护与内容漂移。

## 单一来源（SSOT）说明
为防止多份规范分叉，详细规范不再在此复制，统一指向：
- 核心编码与架构规范：`docs/overview/PROJECT-CODING-STANDARDS.md`
- AI 助手统一配置：`docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`
- 项目共享上下文：`.ai-assistant-config/SHARED-PROJECT-CONTEXT.md`
- Claude 指令：`.claude/project-instructions.md`（应仅做引用，不重复详细规则）
- Copilot 指令：`.github/copilot-instructions.md`（引用统一配置）及 `.github/instructions/` 下的模块专用规范
- CodeX 指令：`.codex/project-context.md`（只引用统一配置）

除上述“单一来源”文档外，其它位置（含本文件）只可：
1. 做摘要或索引
2. 引用源文件路径
3. 标注最新更新时间与维护责任

不得：
- 复制整段规范正文并修改
- 引入与 SSOT 矛盾的实现建议
- 在不同 AI 助手配置中写出冲突策略

## AI 助手必读 (CRITICAL INSTRUCTION)
在开始任何任务前，**必须**先读取并理解 `docs/overview/PROJECT-CODING-STANDARDS.md`。这是本项目的最高法律。
任何生成的代码若违反该文档中的规范，将被视为**严重错误**。

## 助手统一行为基线（摘要）
- 严格遵守分层：Controller → Service → Domain/Repository，不跨层渗透。
- 所有变更考虑多租户上下文传递与隔离；异步需显式传播上下文（参见租户 RFC）。
- 不引入硬编码敏感信息；配置走 profile / 环境变量。
- 新功能需伴随单元/必要的集成测试；关键路径考虑性能与缓存。
- 遇到架构/上下文不确定 → 首查 SSOT 文档，不自行”臆测”。
- **安全配置必须符合安全基线**：新建/修改服务的安全配置（SecurityConfig、JWT 过滤器、密钥管理、租户上下文来源）前，必须先读 `docs/architecture/SECURITY-BASELINE.md`，不得引入 permitAll 调试残留、硬编码密钥、死 @PreAuthorize 等基线禁止的模式。

## 多租户与异步（仅保留关键提醒）
- 使用线程池 / @Async / Reactor 时需维护租户、请求ID、追踪信息。
- 推荐：TransmittableThreadLocal 或统一包装（详见 `docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`）。
- 禁止：裸 `new Thread`、无上下文的任务提交、跨租户数据拼接。

## 速查索引
| 目标 | 文档 | 说明 |
| ---- | ---- | ---- |
| 架构/编码全规范 | `docs/overview/PROJECT-CODING-STANDARDS.md` | 代码与分层硬性标准 |
| **安全基线（SSOT）** | **`docs/architecture/SECURITY-BASELINE.md`** | **各服务认证授权/密钥管理/租户上下文的统一基线** |
| AI 助手总配置 | `docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md` | 各助手行为统一入口 |
| 租户异步上下文 | `docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md` | 上下文传播策略与库选型 |
| 共享项目概述 | `.ai-assistant-config/SHARED-PROJECT-CONTEXT.md` | 项目简介与模块导航 |
| Claude 指令入口 | `.claude/project-instructions.md` | 应仅保留指向总配置 |
| Copilot 引导 | `.github/` 下指令文件 | 只引用统一配置，不重复规范 |
| CodeX 引导 | `.codex/project-context.md` | 调用时读取统一配置 |

## 修改与版本策略
- 若需调整规范：先更新 SSOT（`PROJECT-CODING-STANDARDS.md` 或 `AI-ASSISTANT-UNIFIED-CONFIG.md`），再在此文件更新索引时间戳。
- 不允许直接在 AI 助手各自的指令文件中写入“临时绕过”标准；必须经规范文件更新。
- 重大变更（架构/安全策略）需添加 RFC 或在相关文档增加“变更记录”段落。

## 更新记录
- 2026-07-29：增加安全基线（`SECURITY-BASELINE.md`）引用；助手行为基线增加"安全配置必须符合安全基线"强制规则。
- 2025-12-18：文档刷新，统一为中文表述，创建版本历史记录。
- 2025-11-10：整合为统一入口，去除重复规范正文，建立单一来源索引。

## 使用步骤（助手执行最小流程）
1. 读取本文件明确 SSOT 路径。
2. 加载 `PROJECT-CODING-STANDARDS.md` 与 `AI-ASSISTANT-UNIFIED-CONFIG.md` 内容。
3. 确认需求与租户/上下文影响面。
4. 生成代码（分层+校验+日志+异常）并补测试。
5. 自检：质量清单 + 不违反禁止模式。
6. 若涉及异步/跨线程 → 校验上下文传播实现。

## 子目录覆盖规则
如需在某子项目定制辅助规范（例如专用协议模块），可在该子目录放置新的 `AGENTS.md`，仅补充差异化约束；基础规则仍回退到本文件引用的 SSOT。

—— 结束（本文件仅为索引，不复制规范正文） ——
