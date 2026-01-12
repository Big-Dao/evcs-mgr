# 文档清理总结（动刀整理）

> 用途：记录一次“文档去噪 + 归档整理 + 入口修正”的执行结果，便于后续追溯。

**最后更新**: 2026-01-12  
**维护者**: 技术负责人 / 文档维护  
**状态**: 已归档

---

## 本次目标

- 让 `docs/` 非归档区只保留“可持续维护的长期文档”。
- 将一次性修复记录、会话记录、历史报告迁移到 `docs/archive/`，降低读者选路成本。
- 更新 `docs/DOCUMENTATION-INDEX.md`，增加按任务的快速入口。

---

## 已执行变更

### 1) `docs/troubleshooting/` 去噪

保留（长期）
- `docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md`

迁移到归档（一次性/会话/历史修复记录）
- `docs/troubleshooting/CLAUDE-SESSION-START.md` → `docs/archive/obsolete-docs-2026-01-12/CLAUDE-SESSION-START.md`
- `docs/troubleshooting/CLAUDE_ERROR_MEMORY.md` → `docs/archive/obsolete-docs-2026-01-12/CLAUDE_ERROR_MEMORY.md`
- `docs/troubleshooting/BUGFIX-CHARGER-API.md` → `docs/archive/old-issues/BUGFIX-CHARGER-API.md`
- `docs/troubleshooting/FRONTEND-HARDCODE-FIX.md` → `docs/archive/progress-reports/2025-12/FRONTEND-HARDCODE-FIX.md`
- `docs/troubleshooting/FRONTEND-HARDCODE-FIX-FINAL.md` → `docs/archive/progress-reports/2025-12/FRONTEND-HARDCODE-FIX-FINAL.md`

### 2) `docs/development/` 移出一次性报告

- `docs/development/AI-CONFIG-CLEANUP-REPORT.md` → `docs/archive/documentation-history/AI-CONFIG-CLEANUP-REPORT-2025-11-10.md`

备注：原文件存在编码乱码，本次在归档位置做了内容整理（保留事实与结论，不再保留乱码文本）。

### 3) `docs/references/` 收敛为权威 API 文档

- `docs/references/API-DOCUMENTATION-GUIDE.md` → `docs/archive/obsolete-docs-2026-01-12/API-DOCUMENTATION-GUIDE.md`
- `docs/references/CHANGELOG.md` → `docs/archive/documentation-history/CHANGELOG.md`

说明：对外“版本历史”统一以 `docs/overview/VERSION-HISTORY.md` 为权威入口。

### 4) 更新统一入口页

- 更新 `docs/DOCUMENTATION-INDEX.md`：新增“按任务快速入口”，并移除已归档的 CHANGELOG 引用。

---

## 相关计划与留档

- 执行计划：`docs/archive/documentation-history/CLEANUP-PLAN-2026-01-12.md`

