# 数据模型设计（摘要）

版本：v2.3｜最后更新：2026-01-13｜维护：数据架构组｜状态：活跃

用途：提供架构视角的数据模型摘要与指引；详细的 DDL/索引/约束与 ER 图请参见归档与 SSOT。

## 单一来源
- 数据库设计规范（SSOT）：`docs/development/DATABASE-DESIGN-STANDARDS.md`
- 完整 DDL 与历史设计文档：`docs/archive/documentation-docs-cleanup-2025-12-05/data-model.md`
- **C端用户模块设计**：`docs/architecture/EVCS-USER-MODULE-RFC.md`

## 设计原则（摘要）
- 多租户：所有业务表包含 `tenant_id`，服务与仓储层严格按租户过滤。
- 审计字段：`create_by/create_time/update_by/update_time/deleted` 一致化。
- 命名规范：小写下划线，统一主键策略；软删除用 `deleted`。
- 性能：合理索引、分页查询；避免 N+1；按访问路径设计复合索引。

## 关键实体（概要）
- 租户（`sys_tenant`）：租户编码/名称、层级与状态，按租户隔离。
- 用户（`sys_user`）：账号、加密口令与状态；租户内唯一用户名。
- 角色与关联（`sys_role`、`sys_user_role`）：租户内权限模型与映射。
- 充电站（`charging_station`）：站点基础信息、地理位置与运营信息。
- 订单/支付（`evcs_order`、`evcs_payment` 等）：订单与支付流水的核心表。
- 枪口（`charger_connector`）：枪口维度状态/告警/会话字段（按枪口启停相关边界见 `docs/features/CHARGER-CONNECTOR-CONTROL.md`）。
- **C端用户核心表组（v2.0 拆分设计）**：
  - `charging_user`：主表（~25字段），高频访问的核心信息
  - `user_identity`：证件信息（1:1）
  - `user_contact`：联系方式、紧急联系人（1:1）
  - `user_address`：收货/常用地址（1:N）
  - `user_profile_ext`：职业信息、客服备注（1:1）
  - `user_preference`：偏好设置、快捷引用（1:1）
  - `user_stats`：统计数据（1:1，高频更新）
  - `user_lifecycle`：生命周期节点（1:1）
- **用户OAuth绑定（`user_oauth`）**：第三方账号绑定（微信/支付宝/Apple）。
- **积分/优惠券（`points_transaction`、`user_coupon`）**：积分流水与优惠券管理。
- **用户群组（`user_group`、`user_group_member`）**：企业用户群组与权益。
- **用户画像（`user_profile`、`user_tag`）**：用户标签与画像数据。

详见 [evcs-user 模块 RFC v2.0](EVCS-USER-MODULE-RFC.md)。

备注：此页仅保留概要与原则，规范变更请在 SSOT 更新；详细 DDL 与索引请查阅归档文档。
