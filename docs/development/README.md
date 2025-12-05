# 开发文档
> 最后更新: 2025-11-07 | 维护者: 技术负责人 | 状态: 活跃

## 目录说明

本目录包含 EVCS 充电站管理系统的开发指南、规范与工具文档，为开发团队提供完整的开发指引。

### 核心文档

#### 开发指南与规范
- [DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md) — 开发者指南（新模块开发流程、代码规范、多租户注意事项、Git 工作流）
- [AI-ASSISTANT-UNIFIED-CONFIG.md](AI-ASSISTANT-UNIFIED-CONFIG.md) — AI 助手统一配置（Claude Code / Copilot / Codex 配置入口）
- [PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md) — 项目编码标准（统一规范的单一来源）
- [API-DESIGN-STANDARDS.md](API-DESIGN-STANDARDS.md) — API 设计标准（REST 设计与接口约定）
- [DATABASE-DESIGN-STANDARDS.md](DATABASE-DESIGN-STANDARDS.md) — 数据库设计标准（命名与建模规范）
- [CODE-QUALITY-CHECKLIST.md](CODE-QUALITY-CHECKLIST.md) — 代码质量检查清单（审核要点与质量标准）

#### AI 助手配置（索引）
- [AI-ASSISTANTS-INDEX.md](../archive/documentation-docs-cleanup-2025-12-05/AI-ASSISTANTS-INDEX.md) — AI 助手规范索引（归档入口）
- [AI-ASSISTANT-GUIDELINES.md](../archive/documentation-docs-cleanup-2025-12-05/AI-ASSISTANT-GUIDELINES.md) — AI 助手使用指南（归档入口）
- [AI-CONFIG-NECESSITY-ANALYSIS.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-NECESSITY-ANALYSIS.md) — AI 配置必要性分析（归档入口）
- [AI-CONFIG-CLEANUP-REPORT.md](AI-CONFIG-CLEANUP-REPORT.md) — AI 配置清理报告（当前文档）
- [AI-CONFIG-CONSISTENCY-REPORT.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-CONSISTENCY-REPORT.md) — AI 配置一致性报告（归档入口）
- [AI-CONFIG-DEDUPLICATION-REPORT.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-DEDUPLICATION-REPORT.md) — AI 配置去重报告（归档入口）
- [COPILOT-INSTRUCTIONS-SETUP.md](../archive/ai-config-dedup-2025-12-05/COPILOT-INSTRUCTIONS-SETUP.md) — Copilot 配置说明（归档入口）

#### 开发工具与文档维护
- [DOCKER-BUILD-FIX.md](../archive/documentation-docs-cleanup-2025-12-05/DOCKER-BUILD-FIX.md) — Docker 构建依赖问题修复（归档入口）
- [DOCUMENTATION-CLEANUP-SUMMARY.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-CLEANUP-SUMMARY.md) — 文档整理总结（归档入口）
- [DOCUMENTATION-MAINTENANCE-GUIDE.md](DOCUMENTATION-MAINTENANCE-GUIDE.md) — 文档维护指南（现行）
- [DOCUMENTATION-REORGANIZATION-PLAN.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-REORGANIZATION-PLAN.md) — 文档重组计划（归档入口）
- [DOCUMENTATION-REORGANIZATION-COMPLETE.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-REORGANIZATION-COMPLETE.md) — 文档重组完成报告（归档入口）

## 相关文档
- [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md) — 开发者指南
- [../../.github/copilot-instructions.md](../../.github/copilot-instructions.md) — GitHub Copilot 使用指引
- [COPILOT-INSTRUCTIONS-SETUP.md](../archive/ai-config-dedup-2025-12-05/COPILOT-INSTRUCTIONS-SETUP.md) — Copilot 配置说明（归档入口）

### 模块开发规范
- [../../.github/instructions/common.instructions.md](../../.github/instructions/common.instructions.md) — evcs-common 模块规范
- [../../.github/instructions/station.instructions.md](../../.github/instructions/station.instructions.md) — evcs-station 模块规范
- [../../.github/instructions/test.instructions.md](../../.github/instructions/test.instructions.md) — 测试编写规范

## 快速链路
### 配置开发环境
1. 安装 Java 21
2. 安装 VS Code 与推荐扩展
3. 参考 [IDE-FIX-GUIDE.md](IDE-FIX-GUIDE.md) 配置 VS Code
4. 运行 `./gradlew build` 验证环境

### 常见问题
- 构建失败：清理缓存后重试 `./gradlew clean build`
- 依赖拉取慢：配置镜像源或使用企业制品库
- 代码提示不工作：检查 Java 语言服务器配置

---
目录创建: 2025-10-20  
文档数量: 1