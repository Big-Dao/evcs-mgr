# 文档清理计划（动刀整理）

> 目标：减少“长期文档/临时记录/AI 会话记录”混杂，确保 `docs/` 非归档区只保留可持续维护的权威文档与操作手册；将一次性修复记录、会话记录、历史报告迁移到 `docs/archive/`，并更新导航入口。

**最后更新**: 2026-01-12  
**维护者**: 技术负责人 / 文档维护  
**状态**: 审核中（执行前确认）

---

## 清理原则（本次执行口径）

1. **非归档区只放“可持续维护的长期文档”**：规范（SSOT）、指南（Guide）、运行手册（Runbook）、架构（Design）、API（Reference）。
2. **一次性产物进入归档**：修复记录、进度报告、会话记录、清理报告、临时对比与分析。
3. **不做大规模重命名**：优先“移动归档 + 修正入口链接”，避免全仓库链接断裂。
4. **入口不变**：继续以 `docs/DOCUMENTATION-INDEX.md` 为唯一入口页。

---

## 拟执行变更（文件级）

### A. 清理 troubleshooting：仅保留长期排障清单

**保留（长期）**
- `docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md`

**迁移到归档（一次性/会话/修复记录）**
- `docs/troubleshooting/CLAUDE-SESSION-START.md` → `docs/archive/obsolete-docs-2026-01-12/CLAUDE-SESSION-START.md`
- `docs/troubleshooting/CLAUDE_ERROR_MEMORY.md` → `docs/archive/obsolete-docs-2026-01-12/CLAUDE_ERROR_MEMORY.md`
- `docs/troubleshooting/BUGFIX-CHARGER-API.md` → `docs/archive/old-issues/BUGFIX-CHARGER-API.md`
- `docs/troubleshooting/FRONTEND-HARDCODE-FIX.md` → `docs/archive/progress-reports/2025-12/FRONTEND-HARDCODE-FIX.md`
- `docs/troubleshooting/FRONTEND-HARDCODE-FIX-FINAL.md` → `docs/archive/progress-reports/2025-12/FRONTEND-HARDCODE-FIX-FINAL.md`

理由：以上内容属于“会话记录/历史修复过程/阶段性整改记录”，不应占用长期排障目录。

### B. 清理 development：移除历史报告/一次性总结

- `docs/development/AI-CONFIG-CLEANUP-REPORT.md` → `docs/archive/documentation-history/AI-CONFIG-CLEANUP-REPORT-2025-11-10.md`

理由：属于一次性清理报告，且当前文件存在编码乱码，归档同时做一次内容整理以便保留历史价值。

### C. 清理 references：只保留权威 API 文档

- `docs/references/API-DOCUMENTATION-GUIDE.md` → `docs/archive/obsolete-docs-2026-01-12/API-DOCUMENTATION-GUIDE.md`
- `docs/references/CHANGELOG.md` → `docs/archive/documentation-history/CHANGELOG.md`

理由：
- API 指南内容与 `docs/references/API-DOCUMENTATION.md` 存在重复且有乱码，归档保留历史。
- CHANGELOG 文件自述为“归档”，从非归档区移走，入口以 `docs/overview/VERSION-HISTORY.md` 为准。

---

## 拟执行变更（入口页）

- 重写 `docs/DOCUMENTATION-INDEX.md` 顶部导航：增加“按任务/按角色”入口，减少读者选路成本。
- 修正 references 部分：移除对 `docs/references/CHANGELOG.md` 的引用，统一指向 `docs/overview/VERSION-HISTORY.md`。

---

## 回滚策略

- 所有变更均为“移动归档 + 更新入口链接”，不删除内容。
- 如需回滚：将归档文件移回原位置并恢复索引条目即可。

---

## 执行确认

- 本计划执行后，`docs/troubleshooting/` 将只保留长期排障清单。
- `docs/references/` 将只保留 `API-DOCUMENTATION.md`（API 权威文档）。

