# 统一测试指南（统一入口）

> 用途：统一说明本仓库测试分层、推荐运行方式与排障入口。

**最后更新**: 2025-12-14  
**维护者**: 技术负责人  
**状态**: 已发布

---

## 测试分层

- 单元测试（Unit Test）：聚焦单个类/方法逻辑，不依赖 Spring 容器。
- 服务测试（Service Test）：使用 Spring Test 但尽量不触发外部中间件（Redis/RabbitMQ/Flyway/Config/Eureka）。
- 集成测试（Integration Test）：跨模块端到端链路验证，当前主要在 `evcs-integration` 中落地。

集成测试的详细约束与运行方式见：

- [集成测试指南](INTEGRATION-TEST-GUIDE.md)

常见失败修复入口：

- [测试修复指南](TEST-FIX-GUIDE.md)

---

## 推荐运行方式

### 运行某个集成测试用例

```bash
./gradlew :evcs-integration:test --tests com.evcs.integration.test.FullFlowIntegrationTest
```

### 运行 integration 模块所有测试

```bash
./gradlew :evcs-integration:test
```

---

## 集成测试稳定性原则（P0）

- 不连接真实外部依赖：测试 profile 下通过 `spring.autoconfigure.exclude` 关闭自动配置，并在必要处提供 mock bean。
- 优先改生产代码为“可选依赖/条件装配”：尽量减少 `evcs-integration` 中“补丁式”Bean。
- 数据初始化可控：集成测试使用 H2 + `schema-h2.sql` 进行初始化，避免 Flyway 资源冲突导致 Context 启动失败。
