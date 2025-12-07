# EVCS 项目 AI 助手统一配置（SSOT）

版本：v1.1｜最后更新：2025-11-10｜维护：技术负责人｜状态：活跃

本文件是所有 AI 助手的统一入口与唯一指针，严格遵守 SSOT 原则：不复制规范正文，只引用源文件路径。

## 统一加载顺序
1. 读取仓库统一入口：`AGENTS.md`
2. 加载编码与架构规范：`docs/overview/PROJECT-CODING-STANDARDS.md`
3. 加载共享项目上下文：`.ai-assistant-config/SHARED-PROJECT-CONTEXT.md`
4. 根据助手类型加载指针文件：`.codex/project-context.md` 或 `.claude/project-instructions.md`

## 使用原则（摘要）
- 严格遵守分层：Controller → Service → Domain/Repository
- 多租户上下文必须传递与隔离；异步显式传播上下文（详见 `docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`）
- 禁止硬编码敏感信息；配置走 profile／环境变量
- 新功能需伴随必要的测试与质量检查；关键路径考虑性能与缓存
- 架构或上下文不确定时，先查 SSOT 文档，不臆测

## 指针文件（仅保留引用，不重复正文）
- CodeX 指针：`.codex/project-context.md`（指向本文件与 `AGENTS.md`）
- Claude 指针：`.claude/project-instructions.md`（指向本文件与 `AGENTS.md`）
- Copilot 指针：`.github/*` 中相关说明（仅引用本文件）

## 相关链接
- **AI 助手能力说明**：`docs/development/AI-ASSISTANT-CAPABILITIES.md`（我能做些什么？）
- 编码与架构规范：`docs/overview/PROJECT-CODING-STANDARDS.md`
- 租户异步上下文：`docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`
- 项目概述与模块导航：`.ai-assistant-config/SHARED-PROJECT-CONTEXT.md`

备注：历史的“重组/清理/总结”类文档保持在 `docs/archive/`，不在助手配置重复维护。
