# 变更日志

## [P4 Week 1] - 2025-10-20

### 新增 (Added)
- **Docker 部署**: 完成13个服务的容器化部署
  - 业务服务: gateway, auth, tenant, station, order, payment, protocol, monitoring
  - 基础设施: config-server, eureka, postgresql, redis, rabbitmq
  - Docker Compose 配置: docker-compose.local.yml
  - 健康检查配置: 所有服务支持健康检查

### 修复 (Fixed)
- **测试基础设施**: 修复H2测试数据库相关问题
  - fix: H2 PostgreSQL模式下DOUBLE类型错误 (6709f5b)
  - fix: H2测试schema表结构问题 (167f288)
  - fix: 添加数据清理语句防止主键冲突 (eceb088)
  - fix: H2测试数据SQL中文乱码问题 (2571366)
- **Docker部署冲突**: 解决合并冲突，完善测试基础设施 (53c62d4)

### 改进 (Improved)
- **工作流规范**: 建立"先测试再提交"的开发流程
- **文档更新**: 更新项目状态、进度文档和README

### 已知问题 (Known Issues)
- evcs-integration 模块有12个测试失败（租户上下文相关）
- evcs-payment, evcs-station, evcs-tenant 部分测试失败
- 测试覆盖率约30%，需要提升到80%

---

## [P3 完成] - 2025-10-12

### 里程碑达成
- ✅ M1: 环境与安全加固
- ✅ M2: 协议对接完成
- ✅ M3: 支付集成完成
- ✅ M4: 性能优化完成
- ✅ M5: 前端原型完成
- ✅ M6: 代码质量提升
- ✅ M7: 监控体系建设
- ✅ M8: 文档体系完善

---

## [P1 完成] - 2025-10-04
- evcs-order: 新增 ReconciliationService 与 /reconcile/run-daily 接口
- evcs-order: 暴露 actuator 与 Prometheus 指标
- 计费：修正 BillingServiceImpl 结构与健壮性，保持最小改动

## [P0 完成] - 2025-10-04
- 分时计费计划：每日最多 96 段，站点最多 16 组，桩可绑定计划
- 订单：新增 /orders/start、/orders/stop 幂等流程，结束触发计费
- 多租：@DataScope 与 TenantContext 贯穿

