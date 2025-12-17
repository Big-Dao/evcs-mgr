# 部署文档索引（统一入口）

> 一句话说明：根据你的目标（本地开发 / 联调验证 / CI 测试环境 / 生产部署），从这里进入对应的权威文档与启动方式。

**最后更新**: 2025-12-17  \
**维护者**: DevOps 团队  \
**状态**: 已发布

---

## 你要做什么？（按场景选择入口）

### 1) 本地开发 / 演示（最快启动）

- 快速启动（推荐起步）：[QUICK-START.md](QUICK-START.md)
- 完整部署与环境变量说明：[DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md)

建议：

- 资源紧张（2–4GB）用 `docker-compose.minimal.yml`
- 需要更多功能/更贴近生产用 `docker-compose.optimized.yml` 或 `docker-compose.yml`

### 2) 测试环境（CI/CD 或人工集成测试）

- 测试环境指南（推荐）：[TEST-ENVIRONMENT-GUIDE.md](TEST-ENVIRONMENT-GUIDE.md)
- CI 工作流参考：.github/workflows/test-environment.yml

该方案以 `docker-compose.test.yml` 为准，通常不启用 Eureka / Config / Gateway，重点验证服务与基础设施集成与冒烟。

### 3) 生产部署 / 完整联调

- 统一部署指南（生产/完整栈）：[DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md)
- Docker 与镜像构建说明：[DOCKER-CONFIGURATION-GUIDE.md](DOCKER-CONFIGURATION-GUIDE.md)

历史的优化细节已归档：

- docs/archive/deployment-docs-cleanup-2025-12-05/RESOURCE-OPTIMIZATION-GUIDE.md
- docs/archive/deployment-docs-cleanup-2025-12-05/DOCKER-OPTIMIZATION.md

---

## Compose / 脚本对照表（SSOT）

| 目标 | 推荐入口 | 说明 |
| --- | --- | --- |
| 本地最小启动 | `docker compose -f docker-compose.minimal.yml up -d` | 最少服务、最小资源占用 |
| 本地核心开发环境 | `docker compose -f docker-compose.core-dev.yml up -d` | 核心基础设施 + 网关/认证/注册/配置 |
| 本地完整栈 | `docker compose -f docker-compose.yml up -d` | 全服务栈（更贴近生产） |
| CI/测试环境启动 | `./scripts/start-test.sh` | 使用 `docker-compose.test.yml`，并自动构建必要镜像 |
| CI/测试健康检查 | `./scripts/health-check.sh` | 验证容器状态 + 基础设施 + 应用健康 |
| CI/测试停止清理 | `./scripts/stop-test.sh` | 停止服务，可选删除数据卷 |

---

## 常见入口（默认端口）

- 网关：[http://localhost:8080](http://localhost:8080)
- Eureka：[http://localhost:8761](http://localhost:8761)
- Config：[http://localhost:8888](http://localhost:8888)
- RabbitMQ 管理台：[http://localhost:15672](http://localhost:15672)

备注：不同 compose 组合会启用不同服务与端口，详见各 compose 文件中的 ports。
