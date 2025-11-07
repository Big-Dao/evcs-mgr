# 开发文档

> **版本**: v1.0 | **更新日期**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 活跃

## 📋 目录说明

本目录包含EVCS充电站管理系统的开发指南、规范和工具文档，为开发团队提供完整的开发指导。

### 📁 核心文档

#### 🎯 开发指南与规范
- **[DEVELOPER-GUIDE.md](DEVELOPER-GUIDE.md)** ⭐ **开发者指南**
  - 新模块开发流程、代码规范
  - 多租户开发注意事项、Git工作流
  - 适合：开发人员日常参考

- **[AI-ASSISTANT-UNIFIED-CONFIG.md](AI-ASSISTANT-UNIFIED-CONFIG.md)** 🤖 **AI助手统一配置**
  - Claude Code、GitHub Copilot、CodeX统一配置
  - 项目上下文、编码标准、架构说明

- **[coding-standards.md](coding-standards.md)** 📝 **编码标准**
  - 代码规范、命名约定、最佳实践

- **[API-DESIGN-STANDARDS.md](API-DESIGN-STANDARDS.md)** 🔌 **API设计标准**
  - RESTful API设计规范、接口约定

- **[DATABASE-DESIGN-STANDARDS.md](DATABASE-DESIGN-STANDARDS.md)** 🗄️ **数据库设计标准**
  - 数据库设计规范、命名约定

- **[CODE-QUALITY-CHECKLIST.md](CODE-QUALITY-CHECKLIST.md)** ✅ **代码质量检查清单**
  - 代码审查要点、质量标准

#### 🤖 AI助手配置
- **[AI-ASSISTANTS-INDEX.md](AI-ASSISTANTS-INDEX.md)** - AI助手规范索引
- **[AI-ASSISTANT-GUIDELINES.md](AI-ASSISTANT-GUIDELINES.md)** - AI助手使用指南
- **[AI-CONFIG-NECESSITY-ANALYSIS.md](AI-CONFIG-NECESSITY-ANALYSIS.md)** - AI配置必要性分析
- **[AI-CONFIG-CLEANUP-REPORT.md](AI-CONFIG-CLEANUP-REPORT.md)** - AI配置清理报告
- **[AI-CONFIG-CONSISTENCY-REPORT.md](AI-CONFIG-CONSISTENCY-REPORT.md)** - AI配置一致性报告
- **[AI-CONFIG-DEDUPLICATION-REPORT.md](AI-CONFIG-DEDUPLICATION-REPORT.md)** - AI配置去重报告
- **[COPILOT-INSTRUCTIONS-SETUP.md](COPILOT-INSTRUCTIONS-SETUP.md)** - Copilot配置说明

#### 🔧 开发工具与问题修复
- **[DOCKER-BUILD-FIX.md](DOCKER-BUILD-FIX.md)** - Docker构建依赖问题修复
- **[DOCUMENTATION-CLEANUP-SUMMARY.md](DOCUMENTATION-CLEANUP-SUMMARY.md)** - 文档整理总结
- **[DOCUMENTATION-MAINTENANCE-GUIDE.md](DOCUMENTATION-MAINTENANCE-GUIDE.md)** - 文档维护指南
- **[DOCUMENTATION-REORGANIZATION-PLAN.md](DOCUMENTATION-REORGANIZATION-PLAN.md)** - 文档重组计划
- **[DOCUMENTATION-REORGANIZATION-COMPLETE.md](DOCUMENTATION-REORGANIZATION-COMPLETE.md)** - 文档重组完成报告

## 🔗 相关文档

开发指南和规范请参考：
- [DEVELOPER-GUIDE.md](./DEVELOPER-GUIDE.md) - 开发者指南 ⭐
- [../../.github/copilot-instructions.md](../../.github/copilot-instructions.md) - GitHub Copilot 使用指南
- [COPILOT-INSTRUCTIONS-SETUP.md](./COPILOT-INSTRUCTIONS-SETUP.md) - Copilot 配置说明

### 模块开发规范
- [../../.github/instructions/common.instructions.md](../../.github/instructions/common.instructions.md) - evcs-common 模块规范
- [../../.github/instructions/station.instructions.md](../../.github/instructions/station.instructions.md) - evcs-station 模块规范
- [../../.github/instructions/test.instructions.md](../../.github/instructions/test.instructions.md) - 测试编写规范

## 🛠️ 快速链接

### 配置开发环境
1. 安装 Java 21
2. 安装 VS Code + 推荐扩展
3. 参考 [IDE-FIX-GUIDE.md](IDE-FIX-GUIDE.md) 配置 VS Code
4. 运行 `./gradlew build` 验证环境

### 常见问题
- **3908 个编译错误**: 参考 IDE-FIX-GUIDE.md 的自动修复步骤
- **Gradle 构建失败**: 清理缓存后重新构建
- **代码提示不工作**: 检查 Java 语言服务器配置

---

**目录创建**: 2025-10-20  
**文档数量**: 1 个

