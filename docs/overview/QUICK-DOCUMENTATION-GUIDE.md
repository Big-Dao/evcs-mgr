# 📚 文档快速导航指�?
> **版本**: v1.1 | **最后更�?*: 2025-11-10 | **维护�?*: PMO | **状�?*: 活跃
>
> 🎯 **目标**: 30 秒内定位到合适的项目文档

## 🎯 快速定�?
### 我要了解项目...
- **项目概述**: [../README.md](../README.md) �?- **管理层摘�?*: [EXECUTIVE-SUMMARY.md](./EXECUTIVE-SUMMARY.md)
- **行动计划（Week 4-8�?*: [NEXT-STEP-ACTION-PLAN.md](./NEXT-STEP-ACTION-PLAN.md)
- **完整文档索引**: [../archive/documentation-docs-cleanup-2025-12-05/../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-INDEX.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-INDEX.md) �?- **产品功能需�?*: [产品需求文档](../architecture/requirements.md)
- **项目规范**: [项目编程规范](../overview/PROJECT-CODING-STANDARDS.md) �?
### 我要开�?..
- **技术架�?*: [架构设计](../architecture/architecture.md) �?- **编码规范**: [编码规范](../overview/PROJECT-CODING-STANDARDS.md)
- **API设计**: [API接口设计](../architecture/api-design.md)
- **数据模型**: [数据模型设计](../architecture/data-model.md)
- **开发指�?*: [开发者指南](../development/DEVELOPER-GUIDE.md)
- **测试指南**: [统一测试指南](../testing/TEST-FIX-GUIDE.md)

### 我要部署...
- **部署指南**: [部署指南](../deployment/DEPLOYMENT-GUIDE.md) �?- **Docker配置**: [Docker配置指南](../deployment/DEPLOYMENT-GUIDE.md)
- **默认凭据**: [默认账号密码](../operations/DEFAULT-CREDENTIALS.md)
- **错误预防**: [错误预防检查清单](../troubleshooting/ERROR_PREVENTION-CHECKLIST.md)

### 我要运维...
- **监控指南**: [监控指南](../operations/MONITORING-GUIDE.md)
- **业务指标**: [业务指标](../operations/BUSINESS-METRICS.md)
- **故障排查**: [问题排查](../troubleshooting/)
- **项目结构**: [项目结构说明](../operations/PROJECT-STRUCTURE.md)

### 我要快速开�?..
- **快速开�?*: 查看../overview/目录（即将完善）
- **服务参�?*: [服务详细参考](../references/API-DOCUMENTATION.md)

## 🔍 常见问题快速解�?
### Q: 如何快速部署项目？
**A**: 直接查看 [部署指南](../deployment/DEPLOYMENT-GUIDE.md)，一键部署命令：
```bash
docker-compose up -d
```

### Q: 服务名配置在哪里�?**A**: 查看 [Docker配置指南](../deployment/DEPLOYMENT-GUIDE.md)，包含所有服务的标准命名�?
### Q: 遇到配置错误怎么办？
**A**: 参�?[错误预防检查清单](../troubleshooting/ERROR_PREVENTION-CHECKLIST.md) �?[Claude错误记忆库](../troubleshooting/CLAUDE_ERROR_MEMORY.md)�?
### Q: 如何了解技术架构？
**A**: �?[架构设计](../architecture/architecture.md) 开始，然后查看 [数据模型](../architecture/data-model.md) �?[API设计](../architecture/api-design.md)�?
### Q: 各个微服务分别做什么？
**A**: 查看 [API文档](../references/API-DOCUMENTATION.md)，了解每个服务的功能、端口和配置�?
### Q: 编码规范是什么？
**A**: 查看 [编码规范](../overview/PROJECT-CODING-STANDARDS.md)，包含完整的Java开发规范�?
### Q: AI助手配置在哪里？
**A**: 查看 [AI助手配置](../development/AI-ASSISTANT-UNIFIED-CONFIG.md) �?[AI助手索引](../development/AI-ASSISTANTS-INDEX.md)�?
## 📋 文档优先�?
### 🔥 必读文档 (优先级最�?
1. [项目概述](../README.md) - 项目概述
2. [文档导航中心](../README.md) - 文档导航
3. [项目规范](../overview/PROJECT-CODING-STANDARDS.md) - 编程规范 �?4. [部署指南](../deployment/DEPLOYMENT-GUIDE.md) - 部署指南
5. [架构设计](../architecture/architecture.md) - 技术架�?
### 📖 重要文档
1. [产品需求文档](../architecture/requirements.md) - 需求理�?2. [开发者指南](../development/DEVELOPER-GUIDE.md) - 开发指�?3. [统一测试指南](../testing/TEST-FIX-GUIDE.md) - 测试指南
4. [项目结构说明](../operations/PROJECT-STRUCTURE.md) - 项目结构

### 🔧 参考文�?1. [API文档](../references/API-DOCUMENTATION.md) - API参�?2. [错误预防检查清单](../troubleshooting/ERROR_PREVENTION-CHECKLIST.md) - 错误预防
3. [API接口设计](../architecture/api-design.md) - API参�?4. [数据模型设计](../architecture/data-model.md) - 数据库参�?
## 🚀 学习路径

### 新手入门路径 (30分钟)
1. **了解项目** �?[README.md](README.md) (5分钟)
2. **理解架构** �?[架构设计](../architecture/architecture.md) (10分钟)
3. **快速部�?* �?[部署指南](../deployment/DEPLOYMENT-GUIDE.md) (10分钟)
4. **熟悉规范** �?[编码规范](../overview/PROJECT-CODING-STANDARDS.md) (5分钟)

### 开发者路�?(1小时)
1. **需求理�?* �?[产品需求文档](../architecture/requirements.md) (15分钟)
2. **技术深�?* �?[架构设计](../architecture/architecture.md) + [数据模型](../architecture/data-model.md) (20分钟)
3. **API掌握** �?[API接口设计](../architecture/api-design.md) (15分钟)
4. **测试框架** �?[统一测试指南](../testing/TEST-FIX-GUIDE.md) (10分钟)

### 运维人员路径 (45分钟)
1. **部署掌握** �?[部署指南](../deployment/DEPLOYMENT-GUIDE.md) (20分钟)
2. **配置理解** �?[服务名映射表](../operations/SERVICE_NAMES_MAPPING.md) + [错误预防检查清单](../troubleshooting/ERROR_PREVENTION_CHECKLIST.md) (15分钟)
3. **监控了解** �?[监控配置](../operations/MONITORING-GUIDE.md) (10分钟)

## 📞 获取帮助

### 文档问题
- 如果找不到需要的文档，查�?[../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-INDEX.md](../archive/documentation-docs-cleanup-2025-12-05/DOCUMENTATION-INDEX.md)
- 如果文档内容过时，查看归档目录或提交Issue

### 技术问�?- 查看相关文档的FAQ部分
- 查看错误预防文档和Claude错误记忆�?- 提交技术Issue到项目仓�?
### 文档贡献
- 发现文档问题欢迎提交PR
- 遵循现有文档格式和风�?- 重要更新请记录在变更日志�?
---

**最后更�?*: 2025-11-06
**维护�?*: Claude & 项目团队
**反馈**: 请通过Issue或PR提供反馈
