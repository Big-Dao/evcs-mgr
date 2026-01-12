# AI 助手配置文件清理报告（归档）

> 用途：记录一次“AI 助手配置去重与单一来源（SSOT）”整理的执行结果，作为历史留档。

**最后更新**: 2025-11-10  
**维护者**: 技术负责人  
**状态**: 已归档

---

## 背景与问题

在 AI 助手相关配置目录中，存在以下典型问题：

1. 配置内容重复：不同助手（Claude / Copilot / CodeX）各自维护“完整版说明”，造成维护成本与冲突风险。
2. 配置口径漂移：同一规则在多个文件里被复述，容易出现不一致。
3. 多版本并存：同一助手的配置文件出现“完整版/简化版”并存，读者不确定哪份有效。

---

## 清理目标

- 建立清晰的“单一来源（SSOT）”入口。
- 让各助手配置文件只做“指针”，不重复正文。
- 将过时/重复/示例类内容归档，保留历史但不干扰日常阅读。

---

## 执行结果（当时口径）

### 归档位置

过时/重复内容被迁移到：

- `docs/archive/ai-config-cleanup-2025-11-07/`

### 主要调整

1. **简化为指针文件**
    - `.claude/project-instructions.md`
    - `.github/copilot-instructions.md`
    - `.codex/project-context.md`

2. **统一核心入口**
    - `docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`
    - `AGENTS.md`
    - `docs/overview/PROJECT-CODING-STANDARDS.md`

3. **移除或归档示例/快速参考**
    - 仅保留“对齐 SSOT 的必要指针”，其余全部归档。

---

## 维护建议（长期有效）

1. 规则变更只改 SSOT：优先更新 `PROJECT-CODING-STANDARDS.md` 与 `AI-ASSISTANT-UNIFIED-CONFIG.md`。
2. 指针文件只做引用：各助手配置不要复制长段规范正文。
3. 定期检查重复：发现重复内容立即合并或转为引用。

---

## 相关文档

- `AGENTS.md`
- `docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`
- `docs/overview/PROJECT-CODING-STANDARDS.md`

