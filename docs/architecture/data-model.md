# 数据模型设计（摘要）

版本：v2.2｜最后更新：2025-12-05｜维护：数据架构组｜状态：活跃

用途：提供架构视角的数据模型摘要与指引；详细的 DDL/索引/约束与 ER 图请参见归档与 SSOT。

## 单一来源
- 数据库设计规范（SSOT）：`docs/development/DATABASE-DESIGN-STANDARDS.md`
- 完整 DDL 与历史设计文档：`docs/archive/documentation-docs-cleanup-2025-12-05/data-model.md`

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

备注：此页仅保留概要与原则，规范变更请在 SSOT 更新；详细 DDL 与索引请查阅归档文档。
