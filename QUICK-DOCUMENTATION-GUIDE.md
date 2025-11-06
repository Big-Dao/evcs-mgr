# 📚 文档快速导航指南

> **更新日期**: 2025-11-06 | **目标**: 30秒找到你需要的信息

## 🎯 快速定位

### 我要了解项目...
- **项目概述**: [README.md](README.md) ⭐
- **完整文档索引**: [DOCUMENTATION-INDEX.md](DOCUMENTATION-INDEX.md) ⭐
- **产品功能需求**: [产品需求文档](docs/01-core/requirements.md)
- **微服务详情**: [服务详细参考](SERVICES-REFERENCE.md) ⭐

### 我要开发...
- **技术架构**: [架构设计](docs/01-core/architecture.md) ⭐
- **编码规范**: [编码规范](docs/02-development/coding-standards.md)
- **API设计**: [API接口设计](docs/01-core/api-design.md)
- **数据模型**: [数据模型设计](docs/01-core/data-model.md)
- **测试指南**: [测试框架指南](docs/testing/TESTING-FRAMEWORK-GUIDE.md)

### 我要部署...
- **Docker部署**: [Docker部署指南](docs/03-deployment/docker-deployment.md) ⭐
- **配置说明**: [服务名映射表](SERVICE_NAMES_MAPPING.md)
- **错误预防**: [错误预防检查清单](ERROR_PREVENTION_CHECKLIST.md)

### 我要运维...
- **用户手册**: [用户手册](docs/04-operations/user-manual.md)（待创建）
- **故障排查**: [故障排查](docs/04-operations/troubleshooting.md)（待创建）
- **监控配置**: [监控配置](docs/03-deployment/monitoring-setup.md)（待创建）

### 我要规划...
- **发展路线图**: [发展路线图](docs/05-planning/roadmap.md) ⭐
- **里程碑计划**: [里程碑计划](docs/05-planning/milestones.md)（待创建）

## 🔍 常见问题快速解答

### Q: 如何快速部署项目？
**A**: 直接查看 [Docker部署指南](docs/03-deployment/docker-deployment.md)，一键部署命令：
```bash
docker-compose up -d
```

### Q: 服务名配置在哪里？
**A**: 查看 [服务名映射表](SERVICE_NAMES_MAPPING.md)，包含所有服务的标准命名。

### Q: 遇到配置错误怎么办？
**A**: 参考 [错误预防检查清单](ERROR_PREVENTION_CHECKLIST.md) 和 [Claude错误记忆库](CLAUDE_ERROR_MEMORY.md)。

### Q: 如何了解技术架构？
**A**: 从 [架构设计](docs/01-core/architecture.md) 开始，然后查看 [数据模型](docs/01-core/data-model.md) 和 [API设计](docs/01-core/api-design.md)。

### Q: 各个微服务分别做什么？
**A**: 查看 [服务详细参考](SERVICES-REFERENCE.md)，了解每个服务的功能、端口和配置。

### Q: 编码规范是什么？
**A**: 查看 [编码规范](docs/02-development/coding-standards.md)，包含完整的Java开发规范。

## 📋 文档优先级

### 🔥 必读文档 (优先级最高)
1. [README.md](README.md) - 项目概述
2. [DOCUMENTATION-INDEX.md](DOCUMENTATION-INDEX.md) - 文档导航
3. [服务详细参考](SERVICES-REFERENCE.md) - 微服务详解 ⭐
4. [Docker部署指南](docs/03-deployment/docker-deployment.md) - 部署指南
5. [架构设计](docs/01-core/architecture.md) - 技术架构

### 📖 重要文档
1. [产品需求文档](docs/01-core/requirements.md) - 需求理解
2. [编码规范](docs/02-development/coding-standards.md) - 开发规范
3. [测试框架指南](docs/testing/TESTING-FRAMEWORK-GUIDE.md) - 测试指南
4. [发展路线图](docs/05-planning/roadmap.md) - 项目规划

### 🔧 参考文档
1. [服务名映射表](SERVICE_NAMES_MAPPING.md) - 配置参考
2. [错误预防检查清单](ERROR_PREVENTION_CHECKLIST.md) - 错误预防
3. [API接口设计](docs/01-core/api-design.md) - API参考
4. [数据模型设计](docs/01-core/data-model.md) - 数据库参考

## 🚀 学习路径

### 新手入门路径 (30分钟)
1. **了解项目** → [README.md](README.md) (5分钟)
2. **理解架构** → [架构设计](docs/01-core/architecture.md) (10分钟)
3. **快速部署** → [Docker部署指南](docs/03-deployment/docker-deployment.md) (10分钟)
4. **熟悉规范** → [编码规范](docs/02-development/coding-standards.md) (5分钟)

### 开发者路径 (1小时)
1. **需求理解** → [产品需求文档](docs/01-core/requirements.md) (15分钟)
2. **技术深入** → [架构设计](docs/01-core/architecture.md) + [数据模型](docs/01-core/data-model.md) (20分钟)
3. **API掌握** → [API接口设计](docs/01-core/api-design.md) (15分钟)
4. **测试框架** → [测试框架指南](docs/testing/TESTING-FRAMEWORK-GUIDE.md) (10分钟)

### 运维人员路径 (45分钟)
1. **部署掌握** → [Docker部署指南](docs/03-deployment/docker-deployment.md) (20分钟)
2. **配置理解** → [服务名映射表](SERVICE_NAMES_MAPPING.md) + [错误预防检查清单](ERROR_PREVENTION_CHECKLIST.md) (15分钟)
3. **监控了解** → [监控配置](docs/03-deployment/monitoring-setup.md) (10分钟)

## 📞 获取帮助

### 文档问题
- 如果找不到需要的文档，查看 [DOCUMENTATION-INDEX.md](DOCUMENTATION-INDEX.md)
- 如果文档内容过时，查看归档目录或提交Issue

### 技术问题
- 查看相关文档的FAQ部分
- 查看错误预防文档和Claude错误记忆库
- 提交技术Issue到项目仓库

### 文档贡献
- 发现文档问题欢迎提交PR
- 遵循现有文档格式和风格
- 重要更新请记录在变更日志中

---

**最后更新**: 2025-11-06
**维护者**: Claude & 项目团队
**反馈**: 请通过Issue或PR提供反馈