# 按枪口（Connector）控制与会话落库

> 一句话说明：定义“按枪口启动/停止”在站点服务与管理后台的边界、接口与数据落库口径（仅落库会话字段，协议侧真实启动/停止另行对接）。

**最后更新**: 2025-12-21  \
**维护者**: 平台研发组  \
**状态**: 已发布

---

## 1. 背景与术语

- **充电站（Station）**：站点（场站）维度的运营对象
- **充电桩（Charger / Charging Station）**：一台设备，可能包含多个枪口
- **充电枪（Connector / EVSE Connector）**：真实充电接口；OCPP/云快充通常以“桩标识 + 枪口号”定位

本项目选择将“启停控制、状态、告警、会话字段”下沉到 **枪口（Connector）** 维度，避免“整桩启停”的歧义。

## 2. 需求边界（当前实现）

### 2.1 已实现

- 管理后台按枪口展示：枪口状态/故障/心跳/会话字段
- 站点服务提供按枪口的 start/stop 接口：**集成协议服务下发指令 + 更新 `charger_connector` 会话字段**
- 协议服务集成：通过 Feign Client 调用 `evcs-protocol` 下发 RemoteStart/RemoteStop 指令
- 整桩（charger-level）的 start/stop 控制：**已移除**（UI + 后端接口都不再提供）

### 2.2 未实现（后续工作）

- start/stop 的协议侧 ACK/失败原因回传与 UI 展示（目前仅同步返回指令下发结果）
- 与订单/计费/支付的强一致联动（当前刻意避免触发 order-service 的副作用）

## 3. 数据落库口径（摘要）

核心表：`charger_connector`（枪口维度）

- 主键/唯一性：建议按 `(tenant_id, charger_id, connector_no)` 定位
- 主要字段（按业务用途分组）：
  - 状态与告警：`status`、`fault_code`、`fault_description`、`last_heartbeat`
  - 会话字段：`current_session_id`、`current_user_id`、`charging_start_time`、`charged_energy`、`charged_duration`

数据模型规范与字段基线见：
- `docs/development/DATABASE-DESIGN-STANDARDS.md`
- `docs/architecture/data-model.md`（摘要页）

## 4. 后端接口（evcs-station）

接口实现位置：evcs-station 的 ChargerController（充电桩管理）

### 4.1 查询枪口列表

- `GET /charger/{chargerId}/connectors`
- 行为：返回指定桩下所有枪口（必要时会“确保”枪口记录存在）

### 4.2 按枪口开始充电（仅落库）

- `POST /charger/{chargerId}/connectors/{connectorNo}/start`
- Query Params：
  - `sessionId`：必填
  - `userId`：必填
  - `initialEnergy`：可选（kWh）
- 权限：`charger:charge`
- 行为：写入枪口的会话起始字段（当前时间作为开始时间）

### 4.3 按枪口结束充电（仅落库）

- `POST /charger/{chargerId}/connectors/{connectorNo}/stop`
- Query Params：
  - `sessionId`：可选（存在时用于校验）
  - `energy`：可选（kWh，缺省记 0）
  - `duration`：可选（分钟，缺省记 0）
- 权限：`charger:charge`
- 行为：写入枪口的结束字段，并清理当前会话（以服务实现为准）

## 5. 管理后台 UI（evcs-admin）

页面：充电桩详情页（枪口表格）

- 每行一个枪口：展示 `currentSessionId/currentUserId/chargedEnergy/chargedDuration` 等字段
- 操作：行级“启动/停止”按钮
  - 启动：使用当前登录用户（由请求头 `X-User-Id`/本地存储获取）
  - 停止：弹窗输入 `energy/duration`，一次提交

## 6. 注意事项

- 当前 start/stop 接口 **只做落库**：用于演示/联调 UI 与会话字段，不代表真实设备已启动
- 若启用异步/消息消费更新枪口状态，必须显式传播租户上下文（见 `docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`）

