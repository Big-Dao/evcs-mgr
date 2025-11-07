# EVCS项目AI助手统一配置

> **最后更新**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 活跃
>
> 📋 **本文档为所有AI编程助手提供统一的项目配置**

## 🎯 项目概述

基于Spring Boot 3.2.10 + Java 21的微服务架构电动汽车充电站管理系统（EVCS），专为小规模业务优化设计，支持多租户隔离。

### 📋 完整规范
**🔥 重要：请首先阅读项目完整规范文档**: [PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md)

该文档包含了生成高质量代码所需的**所有规范要求**，包括：
- 强制架构规范和代码模板
- 必须使用的注解和禁止的模式
- 完整的质量检查清单
- 测试要求和性能优化标准

**在生成任何代码之前，请务必遵循PROJECT-CODING-STANDARDS.md中的所有规范！**

## 🏗️ 架构概览

**详细的微服务架构、分层规范、注解要求、禁止模式等完整内容，请参考**: [项目编程规范总览](../overview/PROJECT-CODING-STANDARDS.md)

该文档包含：
- 完整的微服务架构定义和模块划分
- 强制分层架构和注解使用规范
- 严禁的架构违规和代码模式
- 正确的实现方式示例
- 完整的技术栈和最佳实践

**🔥 重要：所有代码生成必须严格遵循PROJECT-CODING-STANDARDS.md中的规范要求！**

## 🤖 AI助手使用指南

### Claude Code配置
Claude Code通过以下配置获取项目上下文：
- [project-instructions.md](../../.claude/project-instructions.md) → 指向本文档

### GitHub Copilot配置
GitHub Copilot通过以下配置获取项目上下文：
- [copilot-instructions.md](../../.github/copilot-instructions.md) → 指向本文档

### OpenAI CodeX配置
OpenAI CodeX通过以下配置获取项目上下文：
- [project-context.md](../../.codex/project-context.md) → 指向本文档

## 💡 使用技巧

### 通用使用技巧
1. **明确需求**: 清楚描述要实现的功能
2. **提供上下文**: 包含相关的业务场景和技术要求
3. **遵循规范**: 严格按照PROJECT-CODING-STANDARDS.md中的规范
4. **包含测试**: 为生成的代码编写相应的测试
5. **验证结果**: 人工审查生成的代码质量和安全性

### 提示词最佳实践
- **最佳实践**: 提供结构化的prompt，包含详细的业务需求
- **明确指定**: 明确指出要遵循的架构模式和编码规范
- **上下文完整**: 提供足够的业务背景和技术约束

## ⚠️ 严格执行文档规范

### 强制性要求
1. **遵循架构规范**: 严格按照分层架构进行开发
2. **使用标准注解**: 必须使用Spring Boot标准注解
3. **数据安全**: 严禁硬编码敏感信息
4. **多租户支持**: 所有数据访问必须考虑租户隔离
5. **异常处理**: 使用统一的异常处理机制

### 代码质量要求
1. **代码注释**: 关键业务逻辑必须有清晰注释
2. **单元测试**: 核心业务逻辑必须有单元测试覆盖
3. **性能考虑**: 数据库查询必须考虑性能优化
4. **安全防护**: 所有对外接口必须有安全防护措施

## 📚 相关文档

### 核心规范文档
- **[项目编程规范总览](../overview/PROJECT-CODING-STANDARDS.md)** - 完整的架构和编码规范
- **[开发者指南](DEVELOPER-GUIDE.md)** - 开发流程和最佳实践
- **[API设计标准](API-DESIGN-STANDARDS.md)** - API接口设计规范
- **[数据库设计标准](DATABASE-DESIGN-STANDARDS.md)** - 数据库设计规范

### 质量保证文档
- **[代码质量检查清单](CODE-QUALITY-CHECKLIST.md)** - 代码质量检查要点
- **[统一测试指南](../testing/UNIFIED-TESTING-GUIDE.md)** - 测试规范和指南

### 项目文档
- **[文档导航中心](../README.md)** - 完整的文档导航
- **[架构设计](../architecture/architecture.md)** - 系统架构设计
- **[部署指南](../deployment/DEPLOYMENT-GUIDE.md)** - 部署和运维指南

## 📋 使用检查清单

在生成代码前，请确认：

### 规范遵循检查
- [ ] 已阅读并理解PROJECT-CODING-STANDARDS.md中的所有规范
- [ ] 了解项目的微服务架构和分层要求
- [ ] 熟悉必须使用的注解和禁止的模式
- [ ] 理解多租户数据隔离的要求

### 代码生成检查
- [ ] 遵循Controller → Service → Repository → Entity的分层架构
- [ ] 使用正确的Spring Boot注解（@RestController, @Service等）
- [ ] 实现了适当的异常处理和数据校验
- [ ] 考虑了缓存策略和性能优化
- [ ] 包含了必要的单元测试

### 安全和质量检查
- [ ] 没有硬编码敏感信息
- [ ] 实现了适当的权限控制
- [ ] 遵循了多租户数据隔离要求
- [ ] 代码注释清晰完整
- [ ] 通过代码质量检查清单的验证

---

**通过严格遵循以上配置和规范，AI助手可以为EVCS项目生成高质量、符合标准的代码。**