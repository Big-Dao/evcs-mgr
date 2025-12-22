# EVCS 部署工作流（摘要指南）

> **本文件旨在梳理部署阶段的执行顺序与脚本映射，所有权威规范仍以 `docs/overview/PROJECT-CODING-STANDARDS.md` 与 `docs/deployment/DEPLOYMENT-GUIDE.md` 为准。**

最后更新：2025-12-22 ｜ 维护：DevOps 团队

---

## 1. 适用范围与单一来源

- 本指南聚焦“怎么串起来做”与“哪些脚本负责哪一段”。
- 环境约束、架构要求、凭据策略等细节请回看：
  - `docs/overview/PROJECT-CODING-STANDARDS.md`
  - `docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`
  - `docs/deployment/DEPLOYMENT-GUIDE.md`
  - `docs/deployment/TEST-ENVIRONMENT-GUIDE.md`

---

## 2. 典型部署场景

| 场景 | 目标 | 推荐入口 | 核心脚本 |
| ---- | ---- | -------- | -------- |
| 本地集成测试 | 在单机验证 DB/Redis/RabbitMQ + Tenant/Station | `./scripts/start-test.sh` 或 `workflow.sh test-env` | `scripts/start-test.sh`、`docker-compose.test.yml` |
| Compose 全量环境 | 拉起生产拓扑的完整镜像组合 | `workflow.sh compose-up` | `scripts/deploy.sh`、`docker-compose.yml`、`docker-compose.monitoring.yml` |
| K8s 内网集群 | 在 K3s/自建集群完成上传、渲染与发布 | `workflow.sh k8s-push` + `workflow.sh k8s-deploy` | `k8s/push-images-from-local.sh`、`k8s/deploy.sh` |
| 本地私有 Registry | 在无公网/需缓存镜像时准备 registry | `workflow.sh local-registry` | `scripts/registry/start-registry.sh` |

> 提示：`workflow.sh` 为新的总入口脚本，位于 `scripts/deploy/`，可通过 `workflow.sh plan` 查看完整步骤。

---

## 3. 标准阶段与顺序

1. **准备工具与目录**
   - 确保已安装 Docker、Docker Compose v2、kubectl（K8s 场景需要）。
   - 运行 `workflow.sh prepare-dirs` 创建 `.local/registry-data`、监控数据、日志目录，避免旧的 `build/registry-data` 权限问题。

2. **构建后端 / 前端产物**
   - 后端：`./gradlew :evcs-tenant:build :evcs-station:build -x test --no-daemon`（测试环境最小集）
   - 全量镜像：`./gradlew clean build`（Compose）或 `./gradlew pushK8sImages -Devcs.k8s.registry=<REG> -Devcs.k8s.tag=<TAG>`
   - 前端镜像：`EVCS_PUSH_JAVA_IMAGES=false bash k8s/push-images-from-local.sh`

3. **镜像归档与分发**
   - 需要离线缓存时：`workflow.sh local-registry` → 推送至 `127.0.0.1:5000`
   - 与 K3s 同机时：`kubectl -n evcs port-forward deploy/registry 5000:5000` 后执行 `workflow.sh k8s-push`

4. **部署执行**
   - **测试环境**：`workflow.sh test-env`（封装 `scripts/start-test.sh`）
   - **Compose**：`workflow.sh compose-up --with-monitoring` 叠加监控 profile
   - **K8s**：`workflow.sh k8s-deploy --registry 192.168.20.235:5000 --tag dev`

5. **健康检查与验收**
   - `workflow.sh health` 触发 `scripts/health-check.sh` 与 `scripts/smoke-test.sh`
   - 关键命令：`docker compose ps`、`kubectl get pods -n evcs`、`curl http://<gateway>/actuator/health`

---

## 4. 新脚本索引

| 脚本 | 功能摘要 |
| ---- | -------- |
| `scripts/deploy/workflow.sh` | 统一调度入口：子命令涵盖计划、目录准备、测试环境、Compose、K8s 推镜像/部署、健康检查。 |
| `scripts/registry/start-registry.sh` | 本地私有 registry 启动器，默认数据目录迁移至 `.local/registry-data`。 |
| `scripts/start-test.sh` | 测试环境编排（CI 入口），支持按需构建最小服务。 |
| `k8s/push-images-from-local.sh` | 前端 + 后端镜像打包并推送到指定 registry。 |
| `k8s/deploy.sh` | 渲染占位符并 apply K8s manifests，并自动补全 ConfigMap。 |

---

## 5. 建议的日常流程

```bash
# 1) 确认当前部署计划
./scripts/deploy/workflow.sh plan

# 2) 初始化目录（只需执行一次/目录被清理后）
./scripts/deploy/workflow.sh prepare-dirs

# 3) 本地测试环境
./scripts/deploy/workflow.sh test-env

# 4) Compose 全量环境（含监控）
./scripts/deploy/workflow.sh compose-up --with-monitoring

# 5) K8s（示例：dev tag 推送 + 部署）
EVCS_IMAGE_TAG=dev EVCS_K8S_REGISTRY=192.168.20.235:5000 \
  ./scripts/deploy/workflow.sh k8s-push
EVCS_IMAGE_TAG=dev EVCS_K8S_REGISTRY=192.168.20.235:5000 \
  ./scripts/deploy/workflow.sh k8s-deploy

# 6) 验证链路
./scripts/deploy/workflow.sh health
```

---

## 6. 后续优化方向

- 将 `workflow.sh` 集成到 CI（GitHub Actions 或 Jenkins），保证 PR 也能沿用相同步骤。
- 为 `workflow.sh` 添加 `--dry-run` 与日志归档功能，方便复盘部署历史。
- 在各脚本中统一 `.local/` 数据目录策略，避免 Docker root 权限残留。

> 如需扩展某个子模块的特殊部署策略，请在对应子目录新增 `AGENTS.md` 并说明差异化约束，基础规则继续回退至本文件引用的 SSOT。
