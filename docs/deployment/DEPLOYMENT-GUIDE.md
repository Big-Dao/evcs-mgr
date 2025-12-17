# 生产部署规划（SSOT）

> 一句话说明：EVCS Manager 的生产部署与发布基线（Day-0/Day-1），以现有 Compose/K8s 资产为准，本文件只做规划与验收口径。

**最后更新**: 2025-12-17  \
**维护者**: DevOps 团队  \
**状态**: 已发布

---

## 适用范围

本规划覆盖：

- 生产环境部署拓扑与组件清单
- 配置来源与注入方式（含多环境）
- 发布/回滚流程与最低验收基线

不覆盖：CI 集成测试环境的详细说明（见 [TEST-ENVIRONMENT-GUIDE.md](TEST-ENVIRONMENT-GUIDE.md)）。

## 目标与原则

- **SSOT**：部署的权威实现以仓库内现存资产为准（Compose 文件、脚本、k8s manifests、配置仓库）。本文件只定义“怎么做/做到什么程度”。
- **最小变更发布**：发布过程默认“可回滚”，避免一次变更跨越过多组件。
- **安全默认**：敏感配置只走环境变量/密钥管理，禁止写入文档或仓库。

## 生产环境拓扑（参考基线）

### 组件分层

- **入口层**：API Gateway（统一入口与鉴权）
- **平台能力**：Auth / Config / Eureka
- **业务域**：Tenant / Station / Order / Payment / Protocol / Monitoring 等
- **基础设施**：PostgreSQL / Redis / RabbitMQ

### 部署形态

- **Docker Compose**：适用于单机/小规模环境或演示型生产（以 [docker-compose.yml](../../docker-compose.yml) 与变体文件为准）。
- **Kubernetes**：适用于需要弹性扩缩容、滚动升级与更强隔离的生产环境（以 [k8s/](../../k8s/) 为准）。

说明：本仓库当前同时存在多套 Compose 变体（如 minimal/optimized/core-dev/monitoring），生产环境建议优先使用“optimized + 必要的 monitoring 叠加”，以减少资源浪费与误配置概率。

## 配置来源与环境分层

### 配置来源（优先级从高到低）

1) **运行时环境变量 / Secret**（生产必须）
2) **配置中心（evcs-config）** 读取的配置仓库（参考 [config-repo/](../../config-repo/)）
3) 应用内默认配置（仅用于开发/默认值）

### 关键配置域（示例口径）

- 数据库连接：URL/用户名/密码/连接池基线
- Redis：地址/密码/超时
- RabbitMQ：地址/账号/密码/虚拟主机
- JWT/认证：JWT 密钥、Token 过期时间、允许的 CORS/回调域名
- 观测：日志级别、指标采集端点、告警阈值（告警规则本身请落在 operations）

提示：生产环境请使用密钥管理服务（或 K8s Secret/External Secret）注入敏感值；本仓库中出现的默认凭据仅用于演示/测试，见 [docs/operations/DEFAULT-CREDENTIALS.md](../operations/DEFAULT-CREDENTIALS.md)。

## 发布流程（生产基线）

### 发布前检查（必须）

- 目标版本构建产物可复现（同一 git commit）
- 数据库变更已评审，必要时已进行演练（含回滚预案）
- 关键配置变更已纳入变更单（含影响面与回滚方式）

### 标准发布步骤（Compose 形态参考）

1) 拉取/构建镜像（按环境策略决定在 CI 构建还是在目标机构建）
2) 先升级“平台能力层”（config/eureka/auth/gateway），再升级业务域服务
3) 发布后执行验收基线（见下文）

### 回滚策略（必须）

- **镜像回滚**：保留上一稳定版本镜像 tag；回滚以“服务级”为单位执行。
- **配置回滚**：配置仓库与 Secret 变更必须可追溯；回滚优先级高于应用回滚。
- **数据库回滚**：默认采用“向前兼容 + 可降级”的迁移策略；如需强回滚，必须提供恢复脚本与数据保护策略。

## 观测与验收基线

### 健康检查

- 每个服务必须暴露健康端点并可达（具体路径以服务 actuator 配置为准）。
- 网关对外入口健康必须优先确认（示例）：

```bash
curl -fsS http://localhost:8080/actuator/health
```

### 最低验收清单（发布后 5-15 分钟内完成）

- Gateway 健康检查为 UP
- Auth 登录接口可用（返回 token）
- 关键业务域至少完成一次“读+写”链路验证（建议 tenant + station 的最小链路）
- 数据库/缓存/消息队列连接正常（通过日志或指标验证）
- 关键告警处于绿色状态（Prometheus/Grafana/日志聚合以现有运维方案为准）

## 安全基线（生产必须）

- 生产环境禁用默认口令与演示账号；所有凭据走 Secret 注入。
- 对外暴露端口应最小化；基础设施端口仅内网可达。
- 任何跨租户访问能力必须受权限控制（多租户/异步上下文要求参见 [docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md](../architecture/TENANT-CONTEXT-ASYNC-RFC.md)）。

## 本地/预发验证（用于发布演练）

> 说明：以下仅用于“发布演练与验收基线验证”。生产部署以你的目标平台与网络隔离策略为准。

### 常用 Compose 组合（参考）

```bash
# 生产资源优化组合
docker compose -f docker-compose.optimized.yml up -d

# 叠加监控（可选）
docker compose -f docker-compose.optimized.yml -f docker-compose.monitoring.yml up -d
```

### 演示数据导入（可选，仅用于本地/预发）

> 注意：生产环境是否允许导入演示数据由你的数据治理策略决定；默认不建议在生产导入。

脚本位置：

- [sql/demo-order-data.sql](../../sql/demo-order-data.sql)

导入示例（容器名与数据库名以你的 Compose/K8s 实际为准）：

```bash
cat sql/demo-order-data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
```

## 参考

- 文档总索引：[docs/DOCUMENTATION-INDEX.md](../DOCUMENTATION-INDEX.md)
- 运维参考：[docs/operations/SERVICES-REFERENCE.md](../operations/SERVICES-REFERENCE.md)
- 监控指南：[docs/operations/MONITORING-GUIDE.md](../operations/MONITORING-GUIDE.md)
- 故障排查：[docs/troubleshooting/ERROR_PREVENTION_CHECKLIST.md](../troubleshooting/ERROR_PREVENTION_CHECKLIST.md)

