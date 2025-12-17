# EVCS Manager 快速启动指南

版本：v1.0｜最后更新：2025-12-16｜维护：DevOps 团队｜状态：活跃

目标：用最少步骤在本机启动 EVCS（开发/演示）。

## 环境要求

- Docker + Docker Compose v2（推荐使用 `docker compose`）
- 可用端口：`8080/8081/8761/8888/5432/6379/15672`（不同 compose 组合会更多）

## 一键启动（推荐）

### 最小配置（2–4GB 内存）

```bash
docker compose -f docker-compose.minimal.yml up -d
```

### 优化配置（6–8GB 内存，生产推荐）

```bash
docker compose -f docker-compose.optimized.yml up -d
```

### 核心开发环境（网关/认证/注册/配置 + 基础设施）

```bash
docker compose -f docker-compose.core-dev.yml up -d
```

## 启动验证

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

## 常用访问入口

请参考文档总索引（部署章节）：[docs/DOCUMENTATION-INDEX.md](../DOCUMENTATION-INDEX.md)。

## 下一步

- 完整部署与环境变量：`docs/deployment/DEPLOYMENT-GUIDE.md`
- 文档总索引：`docs/DOCUMENTATION-INDEX.md`
