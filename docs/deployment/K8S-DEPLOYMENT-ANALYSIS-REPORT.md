# Kubernetes 部署分析报告

> 基于当前仓库资产的 Kubernetes 部署实现现状、风险与改进优先级分析。

**最后更新**: 2026-02-15  
**维护者**: DevOps 团队  
**状态**: 已发布

---

## 1. 范围与依据

### 1.1 评估范围

- 评估对象限定为仓库内 Kubernetes 部署相关资产与其直接依赖配置。
- 分析重点包括：部署入口、运行拓扑、配置与密钥、发布回滚、可观测性、风险与改进路径。
- Docker Compose 仅作为对比上下文，不作为主部署方案进行展开。

### 1.2 依据文件

- `k8s/deploy.sh`
- `k8s/deployments/00-base.yaml` 至 `k8s/deployments/05-frontend.yaml`（含 `01b-rabbitmq-service.yaml`）
- `config-repo/application-k8s.yml`
- `config-repo/evcs-gateway-local.yml`
- `docs/deployment/DEPLOYMENT-GUIDE.md`
- `.github/workflows/test-environment.yml`
- `docker-compose.monitoring.yml`
- `monitoring/` 目录结构（`prometheus/`、`grafana/`、`elk/`）

### 1.3 评估边界

- 本报告基于代码与脚本静态事实，不包含生产集群实测指标。
- 风险评级以可重复部署与可运维性为主，不替代安全审计与容量评估。

## 2. 部署模式与入口（聚焦K8s）

### 2.1 主入口与执行顺序

- `k8s/deploy.sh` 是当前 K8s 主入口，包含 `envsubst` 渲染、`kubectl apply`、ConfigMap patch、滚动重启与 rollout 等待。
- 部署顺序为：`00-base` → `config-repo` ConfigMap → `01-infrastructure` → `02-discovery-config` → ClusterIP patch → `03-gateway` → `04-services` → `05-frontend`。
- 脚本内置 `--dry-run`、`--no-wait`、`EVCS_ROLLOUT_TIMEOUT_SECONDS`，具备最小化发布前校验能力。

### 2.2 清单分层结构

- `00-base.yaml`：命名空间、公共 ConfigMap、公共 Secret。
- `01-infrastructure.yaml`：PostgreSQL、Redis、RabbitMQ、Registry。
- `02-discovery-config.yaml`：Eureka 与 Config Server。
- `03-gateway.yaml`：Gateway 与对外入口 Service。
- `04-services.yaml`：Auth、Tenant、Station、Order、Payment、Protocol、Monitoring 服务。
- `05-frontend.yaml`：管理端前端与 NodePort 暴露。

### 2.3 与 Compose 的关系（对比上下文）

- `docs/deployment/DEPLOYMENT-GUIDE.md` 将 K8s 定位为更适用于弹性伸缩与滚动升级的部署形态。
- Compose 资产保留了监控与日志栈定义，但其配置路径与 K8s 清单并非一一映射。

## 3. K8s运行拓扑与依赖链

### 3.1 运行拓扑

- 所有核心 Deployment 当前均为 `replicas: 1`。
- 多数工作负载通过 `nodeSelector: kubernetes.io/hostname=${EVCS_NODE_NAME}` 固定到单节点。
- 基础设施（PostgreSQL/Redis/RabbitMQ/Registry）使用 `hostPath` 持久化到 `/data/evcs/*`。

### 3.2 依赖链与启动门控

- 配置中心与注册中心位于平台依赖链前段；业务服务依赖其可达性。
- Gateway、Payment 等通过 `initContainer(wait-config-server)` 等待 `CONFIG_SERVER_URL/actuator/health`。
- `deploy.sh` 在基础服务创建后 patch `evcs-common-config`，将数据库、缓存、消息队列、注册中心与服务路由地址写入运行时配置。

### 3.3 网络暴露与访问入口

- Gateway Service 类型为 `LoadBalancer`，固定 `nodePort: 30080`。
- Admin 前端 Service 类型为 `NodePort`，固定 `nodePort: 30090`。
- 其余微服务默认 ClusterIP，仅供集群内调用。

### 3.4 DNS 兼容逻辑

- `config-repo/evcs-gateway-local.yml` 路由 `uri` 采用 `${EVCS_*_IP}` 变量并提供 `127.0.0.1` 默认值。
- `deploy.sh` 显式解析 Service ClusterIP 并 patch 到 ConfigMap，作为 CoreDNS 异常时的兼容路径。

## 4. 配置与密钥策略

### 4.1 配置模型

- `00-base.yaml` 设置 `SPRING_PROFILES_ACTIVE=k8s` 与 `SPRING_CLOUD_CONFIG_PROFILE=local,k8s`，配置加载路径为“local 基线 + k8s 覆盖”。
- `application-k8s.yml` 提供公共 K8s 覆盖项（Eureka、数据源、Redis、RabbitMQ、Actuator info、部分支付轮询配置）。
- `deploy.sh` 将 `config-repo/` 整体打包为 ConfigMap 供 Config Server `native` 模式读取。

### 4.2 密钥与敏感配置现状

- `00-base.yaml` 的 `evcs-secrets` 使用 `stringData` 提交了默认账号密码、JWT secret、内部 API token。
- `config-repo/application-k8s.yml` 仍包含 PostgreSQL 与 RabbitMQ 默认口令字段。
- 现状符合测试便利性，但不满足生产密钥治理最小暴露原则。

### 4.3 覆盖完整性

- `config-repo/` 中仅发现 `application-k8s.yml`、`evcs-order-k8s.yml`、`evcs-tenant-k8s.yml` 三个 k8s profile 文件。
- `auth/gateway/monitoring/payment/protocol/station` 仍以 `*-local.yml` 为主，K8s 专项覆盖存在不均衡。

## 5. 发布与回滚流程分析

### 5.1 发布流程现状

- 发布脚本已形成可执行主路径：模板渲染、依赖部署、运行时 patch、有序 rollout 检查。
- `DEPLOYMENT-GUIDE.md` 明确推荐“本地构建镜像 → 推送集群内 registry → 执行 `k8s/deploy.sh`”。
- 配置变更与镜像发布在同一脚本路径内完成，减少人工步骤但提升脚本耦合度。

### 5.2 回滚能力现状

- 当前脚本未提供一键回滚命令；回滚主要依赖 `kubectl rollout undo` 与镜像 tag 回退。
- ConfigMap patch 属于命令式更新，缺少版本化快照与自动回滚挂钩。
- 单副本部署下，发布窗口中的瞬时不可用风险高于多副本滚动策略。

### 5.3 CI 与 CD 对齐度

- `.github/workflows/test-environment.yml` 已覆盖测试、构建与推送（Tenant/Station）步骤。
- `deploy-to-test` 仅输出镜像信息并保留 `# Add actual deployment commands here`，未形成真实 CD 闭环。

## 6. 可观测性与运维基线

### 6.1 观测入口

- Gateway 配置暴露 `management.endpoints.web.exposure.include=health,info,prometheus`。
- `application-k8s.yml` 开启 `management.info.env.enabled=true`，支持版本识别字段通过 `/actuator/info` 输出。
- `DEPLOYMENT-GUIDE.md` 已定义最小验收动作：Pods/Services 状态与网关健康检查。

### 6.2 监控与日志资产一致性

- `docker-compose.monitoring.yml` 声明 Prometheus、Grafana、ELK、Jaeger 组件。
- Compose 中 Logstash 挂载路径为 `./monitoring/logstash/...`，但仓库目录为 `monitoring/elk/`，存在路径不一致。
- `monitoring/` 当前结构可用于静态资产管理，但与 Compose 运行清单需校准映射关系。

### 6.3 运维基线成熟度

- 基础健康与版本识别能力已具备。
- 告警规则、SLO、容量阈值与故障自动化处置尚未在现有 K8s 清单中体现为统一基线。

## 7. 风险清单（P0/P1/P2）

| 级别 | 风险项 | 文件证据 | 影响 | 建议方向 |
|---|---|---|---|---|
| P0 | 默认密钥已提交到仓库（Secret 与配置文件） | `k8s/deployments/00-base.yaml`、`config-repo/application-k8s.yml` | 密钥泄露、环境漂移、审计不合规 | 迁移到外部密钥管理与 K8s Secret 注入，移除仓库默认值 |
| P1 | 通过 ClusterIP patch 规避 DNS 的方案脆弱 | `k8s/deploy.sh`、`config-repo/evcs-gateway-local.yml` | 服务重建/IP 变化导致配置失效，依赖命令式重启 | 以 Service DNS 为主路径，IP patch 仅作为应急开关 |
| P1 | 监控编排路径与目录布局不一致 | `docker-compose.monitoring.yml`、`monitoring/` | Logstash 启动失败或配置丢失，监控/日志链路不可用 | 统一目录约定并补充启动前校验 |
| P1 | CI 的 deploy job 为占位实现，不是可执行 CD | `.github/workflows/test-environment.yml` | 构建成功不等于可发布，环境漂移依赖人工操作 | 增加真实部署步骤与发布验收门禁 |
| P1 | K8s profile 覆盖不完整，local,k8s 混用面偏大 | `config-repo/` 文件集、`00-base.yaml` | 环境差异不可控，问题定位成本上升 | 补齐服务级 `*-k8s.yml` 并收敛 local 依赖 |
| P2 | 多服务单副本 + 单节点固定调度 | `k8s/deployments/*.yaml`、`k8s/deploy.sh` | 节点故障与发布窗口可用性波动 | 关键服务逐步转双副本并引入反亲和 |
| P2 | 回滚流程缺少脚本化与配置快照 | `k8s/deploy.sh`、`DEPLOYMENT-GUIDE.md` | 回滚时间与准确性依赖人工经验 | 增加版本化发布清单与一键回滚脚本 |

## 8. 优先改进建议（分阶段）

### 8.1 0-2 weeks

- 移除仓库中的默认敏感值，改为环境注入占位并补齐示例模板。
- 修正 `docker-compose.monitoring.yml` 与 `monitoring/` 目录映射，确保监控栈可直接启动。
- 在 CI 中将 `deploy-to-test` 从占位改为可执行部署最小闭环（至少覆盖 test 命名空间）。
- 为 `deploy.sh` 增加部署前检查（必需变量、关键路径存在性、ConfigMap/Secret 差异提示）。

### 8.2 2-6 weeks

- 补齐 `auth/gateway/monitoring/payment/protocol/station` 的 `*-k8s.yml`，减少 local profile 叠加依赖。
- 为关键服务引入双副本与 PDB，降低滚动发布影响面。
- 将 ClusterIP patch 降级为应急策略，默认恢复 Service DNS 解析路径。
- 为发布流程增加版本化清单（镜像 tag、配置版本、回滚点）。

### 8.3 6+ weeks

- 建立 GitOps 化发布路径（声明式变更审计、自动回滚触发、环境漂移检测）。
- 建立统一 SLO 与告警分级体系，覆盖网关、核心业务链路、基础设施依赖。
- 推进多节点容灾与存储策略升级，降低 `hostPath + 单节点` 约束带来的风险。

## 9. 结论

- 当前仓库已具备可执行的 K8s 部署主路径，部署脚本与清单层次清晰，能够支撑内测环境持续发布。
- 主要短板集中在密钥治理、DNS 兼容策略、配置覆盖完整性、CD 闭环与监控资产一致性。
- 按“先安全与可执行闭环、再可靠性与治理升级”的节奏推进，可在不大幅重构的前提下显著提升部署稳定性与可运维性。