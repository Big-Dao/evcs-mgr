# 变更日志

## [未发布] - 2025-10-30

### 文档 (Documentation)
- **文档审查与修复**: 全面审查并修复文档冲突、错误和过时内容 (042d08b)
  - refactor: 完全重写 DOCUMENTATION-INDEX.md - 去除所有重复内容，统一格式
  - fix: 修复 README.md 中错误的文件路径引用（README-TENANT-ISOLATION.md）
  - feat: 补充 TECHNICAL-DESIGN.md 认证架构详细描述（Gateway JWT 验证流程）
  - feat: 更新 DEVELOPER-GUIDE.md 添加 Spring Cloud Config 规范引用
  - feat: 更新 OPERATIONS-MANUAL.md 添加配置管理详细指导
  - docs: 创建 DOCUMENTATION-REVIEW-REPORT.md 完整审查报告
  - 影响: +1046/-131 行，文档质量从混乱/过时提升到清晰/准确

### 配置 (Configuration)
- **Spring Cloud Config 规范化**: 完成配置管理标准化 (5a35fb9)
  - feat: 创建 application-local.yml 全局配置（JWT、Eureka、Actuator）
  - refactor: 所有服务配置移除重复的 JWT、Eureka 配置
  - feat: 创建 SPRING-CLOUD-CONFIG-CONVENTIONS.md 完整规范文档（1605行）
  - fix: 修复 Gateway 路由配置（orders→order, payments→payment）
  - refactor: 所有非 Auth 服务排除 SecurityAutoConfiguration
  - fix: Station 服务依赖调整（仅保留 spring-security-core）
  - 影响: 16 files changed, 1956 insertions(+), 180 deletions(-)

---

## [P4 Week 2] - 2025-10-28

### 重构 (Refactored)
- **Charger 实体重构**: 修复 MyBatis Plus 重复 @TableId 问题 (3cb3a30)
  - 重构 Charger 实体继承 BaseEntity，使用 `@TableId(value="charger_id")` 映射主键
  - 统一使用 `getId()`/`setId()` 替代 `getChargerId()`/`setChargerId()`
  - 影响文件: Charger.java, ChargerServiceImpl.java, ChargerController.java, BillingAssignController.java 及所有测试

### 修复 (Fixed)
- **H2 数据库兼容性**: 修复测试环境 SQL 语法问题
  - fix: 将 PostgreSQL 的 `INSERT...ON CONFLICT` 改为 H2 的 `MERGE INTO` 语法
  - fix: StationMapper.selectNearbyStations 使用子查询统计充电桩数量字段
- **租户编码检查逻辑**: 修复 SysTenantServiceImpl.checkTenantCodeExists 使用错误的主键字段
  - fix: 将 `tenant_id` 改为正确的主键 `id`

### 测试 (Testing)
- ✅ 所有模块测试通过 (97 个任务)
  - evcs-auth: 6 tests passed
  - evcs-common: 28 tests passed
  - evcs-gateway: 13 tests passed
  - evcs-integration: 18 tests passed
  - evcs-order: 17 tests passed
  - evcs-payment: 12 tests passed
  - evcs-protocol: 5 tests passed
  - evcs-station: 27 tests passed
  - evcs-tenant: 42 tests passed

---

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

