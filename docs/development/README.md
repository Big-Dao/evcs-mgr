# 开发文�?
> **最后更�?*: 2025-11-07 | **维护�?*: 技术负责人 | **状�?*: 活跃

## 📋 目录说明

本目录包含EVCS充电站管理系统的开发指南、规范和工具文档，为开发团队提供完整的开发指导�?
### 📁 核心文档

#### 🎯 开发指南与规范
- **[DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md)** �?**开发者指�?*
  - 新模块开发流程、代码规�?  - 多租户开发注意事项、Git工作�?  - 适合：开发人员日常参�?
- **[AI-ASSISTANT-UNIFIED-CONFIG.md](AI-ASSISTANT-UNIFIED-CONFIG.md)** 🤖 **AI助手统一配置**
  - Claude Code、GitHub Copilot、CodeX统一配置
  - 项目上下文、编码标准、架构说�?
- **[coding-standards.md](../overview/PROJECT-CODING-STANDARDS.md)** 📝 **编码标准**
  - 代码规范、命名约定、最佳实�?
- **[API-DESIGN-STANDARDS.md](API-DESIGN-STANDARDS.md)** 🔌 **API设计标准**
  - RESTful API设计规范、接口约�?
- **[DATABASE-DESIGN-STANDARDS.md](DATABASE-DESIGN-STANDARDS.md)** 🗄�?**数据库设计标�?*
  - 数据库设计规范、命名约�?
- **[CODE-QUALITY-CHECKLIST.md](CODE-QUALITY-CHECKLIST.md)** �?**代码质量检查清�?*
  - 代码审查要点、质量标�?
#### 🤖 AI助手配置
- **[AI-ASSISTANTS-INDEX.md](../archive/documentation-docs-cleanup-2025-12-05/AI-ASSISTANTS-INDEX.md)** - AI助手规范索引
- **[AI-ASSISTANT-GUIDELINES.md](../archive/documentation-docs-cleanup-2025-12-05/AI-ASSISTANT-GUIDELINES.md)** - AI助手使用指南
- **[AI-CONFIG-NECESSITY-ANALYSIS.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-NECESSITY-ANALYSIS.md)** - AI配置必要性分�?- **[AI-CONFIG-CLEANUP-REPORT.md](AI-CONFIG-CLEANUP-REPORT.md)** - AI配置清理报告
- **[AI-CONFIG-CONSISTENCY-REPORT.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-CONSISTENCY-REPORT.md)** - AI配置一致性报�?- **[AI-CONFIG-DEDUPLICATION-REPORT.md](../archive/ai-config-dedup-2025-12-05/AI-CONFIG-DEDUPLICATION-REPORT.md)** - AI配置去重报告
- **[COPILOT-INSTRUCTIONS-SETUP.md](../archive/ai-config-dedup-2025-12-05/COPILOT-INSTRUCTIONS-SETUP.md)** - Copilot配置说明

#### 🔧 开发工具与问题修复
- **[DOCKER-BUILD-FIX.md](../archive/documentation-docs-cleanup-2025-12-05/DOCKER-BUILD-FIX.md)** - Docker构建依赖问题修复
- **[DOCUMENTATION-CLEANUP-SUMMARY.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-CLEANUP-SUMMARY.md)** - 文档整理总结
- **[DOCUMENTATION-MAINTENANCE-GUIDE.md](DOCUMENTATION-MAINTENANCE-GUIDE.md)** - 文档维护指南
- **[DOCUMENTATION-REORGANIZATION-PLAN.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-REORGANIZATION-PLAN.md)** - 文档重组计划
- **[DOCUMENTATION-REORGANIZATION-COMPLETE.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-REORGANIZATION-COMPLETE.md)** - 文档重组完成报告

## 🔗 相关文档

开发指南和规范请参考：
- [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md) - 开发者指�?�?- [../../.github/copilot-instructions.md](../../.github/copilot-instructions.md) - GitHub Copilot 使用指南
- [COPILOT-INSTRUCTIONS-SETUP.md](./COPILOT-INSTRUCTIONS-SETUP.md) - Copilot 配置说明

### 模块开发规�?- [../../.github/instructions/common.instructions.md](../../.github/instructions/common.instructions.md) - evcs-common 模块规范
- [../../.github/instructions/station.instructions.md](../../.github/instructions/station.instructions.md) - evcs-station 模块规范
- [../../.github/instructions/test.instructions.md](../../.github/instructions/test.instructions.md) - 测试编写规范

## 🛠�?快速链�?
### 配置开发环�?1. 安装 Java 21
2. 安装 VS Code + 推荐扩展
3. 参�?[IDE-FIX-GUIDE.md](IDE-FIX-GUIDE.md) 配置 VS Code
4. 运行 `./gradlew build` 验证环境

### 常见问题
- **3908 个编译错�?*: 参�?IDE-FIX-GUIDE.md 的自动修复步�?- **Gradle 构建失败**: 清理缓存后重新构�?- **代码提示不工�?*: 检�?Java 语言服务器配�?
---

**目录创建**: 2025-10-20  
**文档数量**: 1 �?
