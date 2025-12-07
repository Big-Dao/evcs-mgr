# 开发文档
> 更新日期: 2025-12-07 | 维护者: 技术负责人 | 状态: 活跃

## 目录说明
本目录包含 EVCS 充电站管理系统的开发指南、规范和工具文档，为开发团队提供完整的开发指导。

### 核心文档

#### 🤖 AI 助手使用指南（优先阅读）
- **[我能做些什么.md](我能做些什么.md)** ⭐ - AI 助手能力快速概览（中文版）
- **[AI-ASSISTANT-CAPABILITIES.md](AI-ASSISTANT-CAPABILITIES.md)** ⭐ - AI 助手完整能力说明（详细版）
- [AI-ASSISTANT-UNIFIED-CONFIG.md](AI-ASSISTANT-UNIFIED-CONFIG.md) - AI 助手统一配置（Claude Code / Copilot / Codex 等入口）

#### 开发指南和规范
- [DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md) - 开发者指南（环境搭建、开发流程、编码规范、横切关注点、Git 工作流等）
- [PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md) - 项目编码标准（统一规范文档单一来源）
- [API-DESIGN-STANDARDS.md](API-DESIGN-STANDARDS.md) - API 设计标准（REST 风格、接口约定）
- [DATABASE-DESIGN-STANDARDS.md](DATABASE-DESIGN-STANDARDS.md) - 数据库设计标准（建表、建模规范）
- [CODE-QUALITY-CHECKLIST.md](CODE-QUALITY-CHECKLIST.md) - 代码质量检查清单（核心开发质量标准）

#### AI 助手配置（归档）
- [AI-ASSISTANTS-INDEX.md](../archive/documentation-docs-cleanup-2025-12-05/AI-ASSISTANTS-INDEX.md) - AI 工具规范导航（归档文档入口）
- [AI-ASSISTANT-GUIDELINES.md](../archive/documentation-docs-cleanup-2025-12-05/AI-ASSISTANT-GUIDELINES.md) - AI 助手使用指南（归档）
- [AI-CONFIG-NECESSITY-ANALYSIS.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-NECESSITY-ANALYSIS.md) - AI 配置必要性分析（归档）
- [AI-CONFIG-CLEANUP-REPORT.md](AI-CONFIG-CLEANUP-REPORT.md) - AI 配置清理报告（当前文档）
- [AI-CONFIG-CONSISTENCY-REPORT.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-CONSISTENCY-REPORT.md) - AI 配置一致性报告（归档）
- [AI-CONFIG-DEDUPLICATION-REPORT.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-DEDUPLICATION-REPORT.md) - AI 配置去重报告（归档）
- [COPILOT-INSTRUCTIONS-SETUP.md](../archive/ai-config-dedup-2025-12-05/COPILOT-INSTRUCTIONS-SETUP.md) - Copilot 配置说明（归档）

#### 历史性质文档（文档维护）
- [DOCKER-BUILD-FIX.md](../archive/documentation-docs-cleanup-2025-12-05/DOCKER-BUILD-FIX.md) - Docker 构建问题修复（归档）
- [DOCUMENTATION-CLEANUP-SUMMARY.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-CLEANUP-SUMMARY.md) - 文档清理总结（归档）
- [DOCUMENTATION-MAINTENANCE-GUIDE.md](../overview/PROJECT-CODING-STANDARDS.md) - 文档维护指南（迁入）
- [DOCUMENTATION-REORGANIZATION-PLAN.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-REORGANIZATION-PLAN.md) - 文档重组计划（归档）
- [DOCUMENTATION-REORGANIZATION-COMPLETE.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-REORGANIZATION-COMPLETE.md) - 文档重组完成报告（归档）

## 快速文档
- [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md) - 开发者指南
- [../../.github/copilot-instructions.md](../../.github/copilot-instructions.md) - GitHub Copilot 使用指南
- [COPILOT-INSTRUCTIONS-SETUP.md](../archive/ai-config-dedup-2025-12-05/COPILOT-INSTRUCTIONS-SETUP.md) - Copilot 配置说明（归档）

### 模块开发规范
- [../../.github/instructions/common.instructions.md](../../.github/instructions/common.instructions.md) - evcs-common 模块规范
- [../../.github/instructions/station.instructions.md](../../.github/instructions/station.instructions.md) - evcs-station 模块规范
- [../../.github/instructions/test.instructions.md](../../.github/instructions/test.instructions.md) - 测试编写规范

## 快速路径
### 首次开发配置
1. 安装 Java 21
2. 安装 VS Code 和推荐扩展
3. 参考 IDE 配置指南（未提供 - 待用 VS Code）
4. 运行 `./gradlew build` 验证构建

### 常见问题
- 构建失败：尝试清理构建 `./gradlew clean build`
- 依赖获取失败：检查是否启用了内源或使用商业镜像
- 运行提示堆溢出：增加 Java 内存分配（参考配置）

---
目录更新日期: 2025-12-07  
文档版本: 2
