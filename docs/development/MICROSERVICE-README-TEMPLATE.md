# 微服务README模板 (推荐)

> **端口**: {port} | **状态**: {active|development|maintenance}
>
> **功能**: {一句话描述服务功能}

## 🎯 服务概述
{1-2句话描述服务的核心价值和主要职责}

## 🚀 快速启动

### 本地开发
```bash
# 构建服务
./gradlew :{service-name}:build

# 运行服务
./gradlew :{service-name}:bootRun

# 健康检查
curl http://localhost:{port}/actuator/health
```

### Docker部署
```bash
# 构建镜像
docker build -t evcs-{service-name}:latest ./{service-name}

# 运行容器
docker run -p {port}:{port} evcs-{service-name}:latest
```

## 🔗 详细文档

### 📚 核心文档
- **[完整API文档](../../docs/references/API-DOCUMENTATION.md#{service-name})** - 所有API端点和数据格式
- **[架构设计](../../docs/architecture/architecture.md#{service-name})** - 服务架构和设计原理
- **[部署指南](../../docs/deployment/DEPLOYMENT-GUIDE.md#{service-name})** - 生产环境部署配置

### 🛠️ 开发文档
- **[开发指南](../../docs/development/DEVELOPER-GUIDE.md#{service-name})** - 开发规范和最佳实践
- **[测试指南](../../docs/testing/UNIFIED-TESTING-GUIDE.md#{service-name})** - 测试策略和用例
- **[故障排查](../../docs/troubleshooting/README.md#{service-name})** - 常见问题和解决方案

### 🔧 运维文档
- **[监控指南](../../docs/operations/MONITORING-GUIDE.md#{service-name})** - 监控配置和指标
- **[项目结构](../../docs/operations/PROJECT-STRUCTURE.md#{service-name})** - 代码组织结构

## 📊 服务信息

| 属性 | 值 |
|------|---|
| **服务名称** | evcs-{service-name} |
| **端口** | {port} |
| **技术栈** | {主要技术栈} |
| **数据库** | {数据库类型} |
| **消息队列** | {MQ类型} |
| **缓存** | {缓存类型} |

## 👥 团队信息

- **服务负责人**: {团队名称}
- **代码审查**: {审查团队}
- **部署负责人**: {部署团队}
- **值班人员**: {值班安排}

---

## 📝 使用说明

### 何时更新本README？
- 服务端口变更时
- 主要功能变更时
- 技术栈重大更新时
- 团队负责人变更时

### 本README不包含什么？
- 详细的API文档 (→ 引用API-DOCUMENTATION.md)
- 完整的部署步骤 (→ 引用DEPLOYMENT-GUIDE.md)
- 复杂的配置说明 (→ 引用具体配置文档)
- 详细的故障排查 (→ 引用troubleshooting文档)

### 维护原则
- 保持简洁，只包含核心信息
- 优先使用引用，避免内容重复
- 确保所有链接有效
- 定期检查信息准确性

---

**模板版本**: v1.0
**最后更新**: 2025-11-07
**维护者**: 架构团队