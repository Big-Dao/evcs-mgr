# EVCS Manager（充电站管理平台）

> 一句话说明：基于 Spring Boot 3.2.12 + Java 21 的微服务架构充电站管理系统，支持多租户隔离、站点/桩/订单/支付/协议与可观测性。

**最后更新**: 2026-01-13  \
**维护者**: 技术负责人 / 各领域维护团队  \
**状态**: 活跃

---

## 1. 快速入口

- 文档统一入口（推荐从这里开始）：[docs/DOCUMENTATION-INDEX.md](docs/DOCUMENTATION-INDEX.md)
- 编码与架构规范（SSOT，必读）：[docs/overview/PROJECT-CODING-STANDARDS.md](docs/overview/PROJECT-CODING-STANDARDS.md)
- 开发上手：[docs/development/DEVELOPER-GUIDE.md](docs/development/DEVELOPER-GUIDE.md)
- 部署（Day-0/Day-1 基线）：[docs/deployment/DEPLOYMENT-GUIDE.md](docs/deployment/DEPLOYMENT-GUIDE.md)
- API 文档：[docs/references/API-DOCUMENTATION.md](docs/references/API-DOCUMENTATION.md)
- 运维服务参考：[docs/operations/SERVICES-REFERENCE.md](docs/operations/SERVICES-REFERENCE.md)
- 常见故障预防清单：[docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md](docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md)

---

## 2. 架构概览

EVCS 采用微服务架构，遵循严格分层：

```text
Controller → Service → Domain/Repository
```

关键约束（以规范为准）：

- 多租户上下文必须传递与隔离；异步场景需要显式上下文传播（详见 RFC）
- 禁止跨服务直连数据库、禁止 Controller 堆业务逻辑、禁止直接返回 Entity

推荐阅读：

- 服务标识与端口规范（SSOT）：[docs/architecture/SERVICE-IDENTIFIERS-AND-PORTS.md](docs/architecture/SERVICE-IDENTIFIERS-AND-PORTS.md)
- 多租户异步上下文 RFC：[docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md](docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md)
- 总体架构/数据模型：[docs/architecture/architecture.md](docs/architecture/architecture.md)、[docs/architecture/data-model.md](docs/architecture/data-model.md)

---

## 3. 服务与模块（代码目录）

> 详细端口/依赖关系/健康检查口径以 SSOT 为准（见上方链接）。

```text
evcs-mgr/
├── evcs-common          # 公共组件（多租户/安全/工具类等）
├── evcs-gateway         # API 网关（统一入口、鉴权与路由）
├── evcs-auth            # 认证授权（JWT/RBAC）
├── evcs-tenant          # 租户管理（隔离与层级治理）
├── evcs-station         # 站点/设备管理（桩/枪口控制、状态同步）
├── evcs-order           # 订单域（计费方案、订单编排）
├── evcs-payment         # 支付域（回调、对账与幂等）
├── evcs-protocol        # 协议域（OCPP/云快充事件流）
├── evcs-monitoring      # 监控域（聚合版本/健康检查等）
├── evcs-config          # 配置中心（Spring Cloud Config）
├── evcs-eureka          # 服务注册中心
├── evcs-integration     # 外部系统集成
├── evcs-admin           # 管理后台前端（Vue 3 + Vite）
└── evcs-mobile          # C 端移动端（uni-app）
```

### 3.1 规划中的模块：C 端充电用户（evcs-user）

本项目的“C 端充电用户管理”规划为独立服务 `evcs-user`（不与 `evcs-auth`/`evcs-tenant` 混在一起），用于承载：用户注册/登录、资料聚合、第三方账号绑定、钱包、积分、优惠券、客诉、车辆、收藏与消息等能力。

权威设计与字段说明（SSOT）：

- C 端用户模块 RFC：[docs/architecture/EVCS-USER-MODULE-RFC.md](docs/architecture/EVCS-USER-MODULE-RFC.md)

接口参考：

- API 文档中 C 端用户章节：[docs/references/API-DOCUMENTATION.md](docs/references/API-DOCUMENTATION.md)

配置相关：

- 配置仓库（示例/本地覆盖）：[config-repo/](config-repo/)
- Compose 启动文件：`docker-compose*.yml`
- Kubernetes 脚本与清单：[k8s/](k8s/)

---

## 4. 快速开始（本地）

前置条件：

- Java 21
- Docker / Docker Compose

最小化启动（以仓库现有 Compose 资产为准）：

```bash
docker compose -f docker-compose.yml up -d
```

快速验证（示例）：

```bash
docker compose ps
curl -fsS http://localhost:8080/actuator/health
```

更完整的启动组合、验收口径与回滚流程：

- [docs/deployment/DEPLOYMENT-GUIDE.md](docs/deployment/DEPLOYMENT-GUIDE.md)
- [docs/deployment/TEST-ENVIRONMENT-GUIDE.md](docs/deployment/TEST-ENVIRONMENT-GUIDE.md)

---

## 5. 开发与质量门禁

常用命令：

```bash
# 运行测试
./gradlew test --continue

# 本地质量检查（包含静态检查与项目门禁）
./gradlew check
```

说明：仓库对“异步上下文丢失”“裸线程/线程池”等模式有门禁（默认 fail），以保证多租户与 traceId 上下文不被破坏。细节见：

- [docs/overview/PROJECT-CODING-STANDARDS.md](docs/overview/PROJECT-CODING-STANDARDS.md)

---

## 6. 文档与变更

本仓库文档遵循“单一入口 + SSOT 指向”的结构，避免同一事实在多处重复维护：

- 统一索引：[docs/DOCUMENTATION-INDEX.md](docs/DOCUMENTATION-INDEX.md)
- 版本历史：[docs/overview/VERSION-HISTORY.md](docs/overview/VERSION-HISTORY.md)
- 下一步计划（含待办跟踪）：[docs/overview/NEXT-PLAN.md](docs/overview/NEXT-PLAN.md)

---

## 7. 许可证

许可证信息以仓库声明为准。

### 6.1 开发计划分析（2026年2月最新）

针对"下一步开发计划"的深度分析已完成，包含详细的任务拆解、风险评估和时间规划：

- 📊 **执行摘要**（管理层）：[docs/overview/DEVELOPMENT-PLAN-EXECUTIVE-SUMMARY.md](docs/overview/DEVELOPMENT-PLAN-EXECUTIVE-SUMMARY.md)
- 📘 **深度分析**（技术团队）：[docs/overview/DEVELOPMENT-PLAN-ANALYSIS-2026-02.md](docs/overview/DEVELOPMENT-PLAN-ANALYSIS-2026-02.md)
- 🗓️ **可视化路线图**（全员）：[docs/overview/ROADMAP-VISUAL-2026.md](docs/overview/ROADMAP-VISUAL-2026.md)

**核心结论**：项目已完成 P0 风险修复，建议立即启动多层级租户测试与协议栈真实对接，4-6周内完成质量巩固与性能基线建立。
