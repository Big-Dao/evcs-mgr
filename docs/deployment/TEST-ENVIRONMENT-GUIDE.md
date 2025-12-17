# 测试环境部署指南

> 一句话说明：用于 CI/CD 自动化测试与人工集成测试的 Docker Compose 测试环境，入口以脚本为准。

**最后更新**: 2025-12-17  \
**维护者**: DevOps 团队  \
**状态**: 已发布

---

## 适用范围

本测试环境用于：

- 在本机/CI 上快速拉起“基础设施 + 少量业务服务”进行集成验证
- 运行健康检查与冒烟测试

不适用：需要完整微服务形态（Eureka/Config/Gateway + 全业务服务）时，请使用 [DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md) 中的完整部署方案。

---

## 单一入口（SSOT）

- 启动入口脚本：scripts/start-test.sh
- 编排文件：docker-compose.test.yml
- 健康检查脚本：scripts/health-check.sh
- 停止清理脚本：scripts/stop-test.sh

说明：CI 工作流也使用同样的脚本与 Compose（见 .github/workflows/test-environment.yml）。

---

## 环境准备

- Java 21（用于构建 JAR）
- Docker + Docker Compose v2（推荐使用 `docker compose`）
- Gradle Wrapper（仓库根目录的 `./gradlew`）

端口占用（默认）：

- PostgreSQL：5432
- Redis：6379
- RabbitMQ：5672 / 15672
- Tenant Service：8081
- Station Service：8082
- Adminer：8090

---

## 一键启动（推荐）

在仓库根目录执行：

```bash
./scripts/start-test.sh
```

脚本会：

1) 构建应用 JAR（默认会 `./gradlew clean build -x test`）
2) `docker compose -f docker-compose.test.yml down` 清理旧环境
3) `docker compose -f docker-compose.test.yml up --build -d` 拉起测试环境

---

## 启动验证

```bash
./scripts/health-check.sh
```

健康检查包含：

- 容器运行状态（postgres/redis/rabbitmq/tenant/station）
- 基础设施连通性（PostgreSQL/Redis/RabbitMQ）
- 应用健康端点：

  - [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
  - [http://localhost:8082/actuator/health](http://localhost:8082/actuator/health)

---

## 常用操作

查看容器：

```bash
docker compose -f docker-compose.test.yml ps
```

查看日志：

```bash
docker compose -f docker-compose.test.yml logs -f
docker compose -f docker-compose.test.yml logs -f tenant-service
docker compose -f docker-compose.test.yml logs -f station-service
```

停止与清理：

```bash
./scripts/stop-test.sh
```

---

## 默认凭据

测试环境的默认凭据请参考 docs/operations/DEFAULT-CREDENTIALS.md。

提示：测试环境的数据库/Redis/RabbitMQ 凭据在 docker-compose.test.yml 中以环境变量形式配置，便于 CI 使用。

---

## 多租户与异步（提醒）

测试环境涉及消息队列或异步执行时，租户上下文必须显式传播与清理，参考：

- docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md

---

## 常见问题

1) 启动失败或镜像构建失败

- 先执行：`./gradlew clean build -x test --no-daemon`
- 再执行：`./scripts/start-test.sh`

1) 健康检查失败

- 先看容器状态：`docker compose -f docker-compose.test.yml ps`
- 再看日志：`docker compose -f docker-compose.test.yml logs --tail=200 tenant-service station-service`
