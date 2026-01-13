# 服务标识与端口规范（SSOT）

> 一句话说明：统一使用 Docker Compose 的 service key 作为“文档主标识”，并定义 Compose / Kubernetes / Spring 的命名映射与端口规范。

**最后更新**: 2026-01-13  \
**维护者**: 技术负责人 / 架构组  \
**状态**: 已发布

---

## 1. 使用方式（快速）

1.1. 文档、讨论、脚本中统一使用 Docker Compose 的 service key 作为服务主标识（例如 `auth-service`、`gateway`）。

1.2. 本地开发与演示场景下，端口与服务名以 [docker-compose.yml](../../docker-compose.yml) 为权威来源。

1.3. Kubernetes 场景下，Service/Deployment 名称以 `k8s/deployments/**/*.yaml` 为权威来源；其命名可能与 Compose service key 不一致，必须显式做映射。

1.4. 运行时观测（日志/指标）默认使用 `spring.application.name` 作为“应用标识”；其命名通常与 Kubernetes Service 名一致，但可能与 Compose service key 不一致。

---

## 2. 术语与标识类型

2.1. Docker Compose service key

- 定义位置：`docker-compose.yml` 的 `services:` 键名
- 作用范围：Compose 网络内 DNS 名称、`depends_on` 引用、脚本参数约定
- 本文定义：作为“文档主标识”（canonical identifier）

2.2. Gradle 模块目录名

- 定义位置：仓库根目录下的模块目录（例如 `evcs-auth`）以及 [settings.gradle](../../settings.gradle)
- 作用范围：构建与代码组织

2.3. Spring `spring.application.name`

- 定义位置：配置仓库 `config-repo/`（例如 `config-repo/evcs-auth-local.yml`）
- 作用范围：Eureka 注册发现、日志/指标标签、调用链展示

2.4. Kubernetes Service 名称

- 定义位置：`k8s/deployments/**/*.yaml`
- 作用范围：集群内 DNS 名称、服务发现与路由

---

## 3. Canonical 映射表（Docker Compose service key）

说明：本表以 Compose service key 为主键，其他标识均作为映射字段。端口信息来自 [docker-compose.yml](../../docker-compose.yml)。

| Compose service key（主标识） | 类别 | Gradle 模块目录 | Spring application name | Kubernetes Service 名 | Compose 端口（host:container） |
| --- | --- | --- | --- | --- | --- |
| `postgres` | Infra | 无 | 无 | `evcs-postgres` | `5432:5432` |
| `redis` | Infra | 无 | 无 | `evcs-redis` | `6379:6379` |
| `rabbitmq` | Infra | 无 | 无 | `evcs-rabbitmq` | `5672:5672`, `15672:15672` |
| `eureka` | Platform | `evcs-eureka` | `evcs-eureka` | `evcs-eureka` | `8761:8761` |
| `config-server` | Platform | `evcs-config` | `evcs-config` | `evcs-config` | `8888:8888` |
| `gateway` | Service | `evcs-gateway` | `evcs-gateway` | `evcs-gateway` | `8080:8080` |
| `auth-service` | Service | `evcs-auth` | `evcs-auth` | `evcs-auth` | `8081:8081` |
| `tenant-service` | Service | `evcs-tenant` | `evcs-tenant` | `evcs-tenant` | `8086:8086` |
| `station-service` | Service | `evcs-station` | `evcs-station` | `evcs-station` | `8082:8082` |
| `order-service` | Service | `evcs-order` | `evcs-order` | `evcs-order` | `8083:8083` |
| `payment-service` | Service | `evcs-payment` | `evcs-payment` | `evcs-payment` | `8084:8084` |
| `protocol-service` | Service | `evcs-protocol` | `evcs-protocol` | `evcs-protocol` | `8085:8085` |
| `monitoring-service` | Service | `evcs-monitoring` | `evcs-monitoring` | `evcs-monitoring` | `8087:8087` |
| `user-service` | Service | `evcs-user` | `evcs-user` | `evcs-user` | `8088:8088` |
| `admin-frontend` | Frontend | `evcs-admin` | 无 | `evcs-admin` | `3000:80` |

---

## 4. 跨环境差异说明（Compose 与 K8s）

4.1. Compose 与 K8s 的命名不一致是“允许的”，但必须被显式记录与遵守。

4.2. 当前项目的普遍规律

- Compose 主标识：以 `*-service` 结尾的业务服务（例如 `auth-service`）。
- K8s Service：更偏向 `evcs-<spring.application.name>`（例如 `evcs-auth`）。
- Spring application name：以 `evcs-<domain>` 命名（例如 `evcs-auth`）。

4.3. 网关相关约定

- Compose 下，网关通过环境变量（例如 `EVCS_AUTH_IP=auth-service`）引用下游服务。
- Kubernetes 下，网关通过 Service DNS（例如 `evcs-auth`）引用下游服务。

---

## 5. 端口与暴露规则

5.1. “内部端口”与“对外端口”

- Compose 网络内服务互访使用 container 端口。
- 宿主机访问使用 host 映射端口（`host:container`）。

5.2. 端口变更的约束

- 任何端口变更必须同步更新：
  - `docker-compose.yml`
  - 对应服务的配置（`config-repo/`）与文档
  - 若涉及 Kubernetes：`k8s/deployments/**/*.yaml`

---

## 6. 变更控制

6.1. 允许的变更

- 新增服务：必须先在 `docker-compose.yml` 增加 service key，并补齐本表映射。
- 端口调整：必须按 5.2 清单同步变更。

6.2. 合规检查清单

- Compose service key 是否唯一且可读（避免随意缩写）。
- `spring.application.name` 是否与模块语义一致。
- K8s Service/Deployment 是否与集群内路由策略一致。

---

## 7. 文档历史

7.1. 2026-01-12

- 建立本文档作为“服务标识与端口”的单一来源（SSOT）。
- 将历史运维映射表从运维文档中剥离，避免与实际配置漂移。
