# 测试环境部署指南
> 最后更新: 2025-12-05 | 维护者: 技术负责人

本指南为现行测试环境的统一入口。历史 Quickstart 请参见 `docs/archive/TEST-ENVIRONMENT-QUICKSTART.md`。

## 环境准备
- Java `21`
- Node.js `>=18`（如需前端构建）
- Docker 与 Docker Compose（本地集成测试）
- Gradle Wrapper：使用项目内的 `./gradlew`

## 快速验证
1. 本地构建：`./gradlew clean build`
2. 运行核心服务（示例）：`docker compose -f docker-compose.test.yml up -d`
3. 健康检查：访问各服务的 `/actuator/health`

## 配置与凭据
- 所有敏感信息通过环境变量或配置 Profile 下发，禁止硬编码。
- 默认凭据参考：`docs/operations/DEFAULT-CREDENTIALS.md`

## 多租户与上下文
- 测试中涉及异步或线程池时，需显式传递租户/请求ID/追踪信息。
- 推荐实现参考：`docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`

## 常见故障排查
- 构建失败：清理缓存后重试 `./gradlew clean build`
- 依赖拉取慢：配置国内镜像源或使用企业制品库
- API 文档：`docs/references/API-DOCUMENTATION.md`

## 关联文档
- 部署总览：`docs/deployment/DEPLOYMENT-GUIDE.md`
- 历史 Quickstart：`docs/archive/TEST-ENVIRONMENT-QUICKSTART.md`
- 统一编码规范：`docs/overview/PROJECT-CODING-STANDARDS.md`