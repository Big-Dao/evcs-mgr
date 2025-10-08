# 变更日志

## [P1 完成] - 2025-10-04
- evcs-order: 新增 ReconciliationService 与 /reconcile/run-daily 接口
- evcs-order: 暴露 actuator 与 Prometheus 指标
- 计费：修正 BillingServiceImpl 结构与健壮性，保持最小改动

## [P0 完成] - 2025-10-04
- 分时计费计划：每日最多 96 段，站点最多 16 组，桩可绑定计划
- 订单：新增 /orders/start、/orders/stop 幂等流程，结束触发计费
- 多租：@DataScope 与 TenantContext 贯穿
