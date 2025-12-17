# 测试部署规划（SSOT）

> 一句话说明：EVCS Manager 的测试环境部署与集成验收基线（CI/CD 与本地集成测试），入口以脚本与 compose 文件为准。

**最后更新**: 2025-12-17  \
**维护者**: DevOps 团队  \
**状态**: 已发布

---

## 适用范围

本测试环境用于：

- 在 CI/CD 或开发机上快速拉起“基础设施 + 少量业务服务”做集成验证
- 运行健康检查与冒烟测试，给 PR/合入提供可重复的验收口径

不适用：需要完整微服务拓扑（Eureka/Config/Gateway + 全业务服务）时，请参考 [DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md)。

## 单一入口（SSOT）

以下为权威入口与实现载体：

- 编排文件：[docker-compose.test.yml](../../docker-compose.test.yml)
- 启动脚本：scripts/start-test.sh
- 健康检查脚本：scripts/health-check.sh
- 冒烟测试脚本：scripts/smoke-test.sh
- 停止清理脚本：scripts/stop-test.sh
- CI 工作流：.github/workflows/test-environment.yml

说明：文档只定义流程与验收口径；任何“如何启动/如何连通”的最终解释以脚本与 compose 为准。

## 前置条件

- Java 21（构建 JAR）
- Docker + Docker Compose v2（推荐使用 `docker compose`）
- Gradle Wrapper（仓库根目录的 `./gradlew`）

## 端口与访问（默认）

- PostgreSQL：5432
- Redis：6379
- RabbitMQ：5672 / 15672
- Tenant Service：8081
- Station Service：8082
- Adminer：8090

提示：端口占用冲突是最常见失败原因之一，启动前建议先检查占用。

## 一键启动（推荐）

在仓库根目录执行：

```bash
./scripts/start-test.sh
```

默认流程（以脚本实现为准）：

1) 构建应用 JAR（常见为 `./gradlew clean build -x test --no-daemon`）
2) 清理旧环境（`docker compose -f docker-compose.test.yml down`）
3) 拉起测试环境（`docker compose -f docker-compose.test.yml up --build -d`）

## 验收基线

### 健康检查（必须）

```bash
./scripts/health-check.sh
```

健康检查应覆盖：

- 容器运行状态（postgres/redis/rabbitmq/tenant/station）
- 基础设施连通性（PostgreSQL/Redis/RabbitMQ）
- 应用健康端点可达：

  - <http://localhost:8081/actuator/health>
  - <http://localhost:8082/actuator/health>

### 冒烟测试（建议在 CI 必跑）

```bash
./scripts/smoke-test.sh
```

冒烟测试建议最少覆盖：

- 关键服务健康与最小链路
- 数据库表结构/初始化是否符合预期
- 服务依赖是否可用（例如 tenant -> db，station -> tenant/db）

## 常用操作

查看容器：

```bash
docker compose -f docker-compose.test.yml ps
```

查看日志：

```bash
docker compose -f docker-compose.test.yml logs -f
```

停止与清理：

```bash
./scripts/stop-test.sh
```

## 失败排查（最短路径）

1) 看容器状态

```bash
docker compose -f docker-compose.test.yml ps
```

1) 看关键服务日志（优先 tenant/station/postgres）

```bash
docker compose -f docker-compose.test.yml logs --tail=200 tenant-service station-service postgres
```

1) 再跑一次健康检查，确认失败点

```bash
./scripts/health-check.sh
```

## 多租户与异步（提醒）

测试环境涉及消息队列或异步执行时，租户上下文必须显式传播与清理，参考：

- [docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md](../architecture/TENANT-CONTEXT-ASYNC-RFC.md)

## 参考

- 文档总索引：[docs/DOCUMENTATION-INDEX.md](../DOCUMENTATION-INDEX.md)
- 默认凭据（演示/测试）：[docs/operations/DEFAULT-CREDENTIALS.md](../operations/DEFAULT-CREDENTIALS.md)
