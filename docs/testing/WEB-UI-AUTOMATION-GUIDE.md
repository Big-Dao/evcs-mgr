# Web UI 自动化测试指南（evcs-admin）

> 用途：说明 evcs-admin Web UI 自动化测试的本地与 CI 执行方式。

**最后更新**: 2026-02-15  
**维护者**: 技术负责人  
**状态**: 已发布

---

## 1. 适用范围

- 本文档仅适用于 `evcs-admin` 的 Playwright Web UI 自动化测试。
- 后端依赖服务通过根目录 `docker-compose.yml` 启动与停止。

## 2. 前置条件

- 已安装并可用：Docker、Docker Compose（`docker compose`）、Node.js 20+、npm。
- 已具备 Java 21 环境（用于本地构建后端 jar，供 Docker 镜像使用）。
- 在仓库根目录执行脚本和 CI 相关命令。

## 3. 本地执行步骤

```bash
# 1) 构建后端 jar（供 docker-compose 启动依赖服务）
./gradlew \
  :evcs-common:build \
  :evcs-eureka:build \
  :evcs-config:build \
  :evcs-gateway:build \
  :evcs-auth:build \
  :evcs-tenant:build \
  :evcs-station:build \
  :evcs-order:build \
  :evcs-payment:build \
  :evcs-protocol:build \
  :evcs-monitoring:build \
  -x test

# 2) 安装前端依赖与 Playwright 浏览器
cd evcs-admin
npm ci
npm run e2e:install
cd ..

# 3) 启动环境并执行测试
E2E_HEALTH_TIMEOUT_SECONDS=900 ./scripts/start-e2e-admin.sh
cd evcs-admin && npm run e2e && cd ..

# 4) 停止环境（可选清理卷）
./scripts/stop-e2e-admin.sh
# 或清理卷
E2E_CLEAN_VOLUMES=1 ./scripts/stop-e2e-admin.sh
```

## 4. 环境变量

- `E2E_BASE_URL`：Playwright 访问的前端地址（默认通常为 `http://localhost:3000`）。
- `EVCS_E2E_IDENTIFIER`：登录账号标识（用户名/手机号/邮箱，取决于测试用例约定）。
- `EVCS_E2E_PASSWORD`：登录密码。
- `E2E_HEALTH_TIMEOUT_SECONDS`：启动脚本健康检查超时时间（秒）。

## 5. CI 工作流

- 工作流文件：`.github/workflows/ui-e2e-admin.yml`。
- 触发方式：`pull_request`（`main`、`develop`）与 `workflow_dispatch`。
- 工作流执行内容：构建后端 jar、安装 Playwright Chromium 依赖、启动环境、执行 `evcs-admin` E2E、上传 `playwright-report` 与 `test-results`、最后清理环境。

## 6. 常见问题排查

- 启动超时：增大 `E2E_HEALTH_TIMEOUT_SECONDS`，并检查 `docker compose -f docker-compose.yml ps`。
- 浏览器依赖安装失败：在 `evcs-admin` 下重新执行 `npm run e2e:install`。
- 登录失败：检查 `EVCS_E2E_IDENTIFIER` 与 `EVCS_E2E_PASSWORD` 是否为测试环境有效凭据。
- 环境清理不完整：执行 `E2E_CLEAN_VOLUMES=1 ./scripts/stop-e2e-admin.sh` 以清理容器与卷。
