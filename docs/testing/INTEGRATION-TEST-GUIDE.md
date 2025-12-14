# 集成测试指南（Integration Test Guide）

> 用途：说明 `evcs-integration` 的集成测试运行方式、测试 profile 策略、H2 初始化方式与常见排障点。

**最后更新**: 2025-12-14  
**维护者**: 技术负责人  
**状态**: 已发布

---

## 适用范围

- 适用于：`evcs-integration` 模块内的集成测试（跨模块端到端链路验证）。
- 目标：让集成测试“可重复、可稳定运行”，不依赖本机 Redis/RabbitMQ/Flyway/Config Server/Eureka。

---

## 如何运行

### 运行单个用例（推荐）

```bash
./gradlew :evcs-integration:test --tests com.evcs.integration.test.FullFlowIntegrationTest
```

### 运行该模块全部测试

```bash
./gradlew :evcs-integration:test
```

---

## 测试 profile 与外部依赖策略

集成测试默认使用 `evcs-integration/src/test/resources/application-test.yml` 配置：

- 关闭/排除外部依赖自动配置：
  - RabbitMQ：排除 `org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration`
  - Redis：排除 `org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration`
  - Flyway：排除 `org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration`
  - 注册/配置中心：禁用 Eureka/Config Client
- 数据库使用 H2 内存库：
  - `spring.datasource.url=jdbc:h2:mem:testdb...`
  - `spring.sql.init.schema-locations=classpath:schema-h2.sql`

测试专用 Bean/Mock 统一放在：

- `evcs-integration/src/test/java/com/evcs/integration/config/TestConfig.java`

原则：

- 能用“生产代码可选依赖/条件化装配”解决的，不要继续加 test 侧补丁。
- 只有在确实需要隔离第三方 SDK/中间件时，才在 `TestConfig` 中提供 mock。

---

## H2 初始化（schema-h2.sql）

集成测试通过 `schema-h2.sql` 聚合各模块关键表结构，保证 Context 启动时可建表。

- 文件位置：`evcs-integration/src/test/resources/schema-h2.sql`
- 加表/改表建议：
  - 只放“集成测试必须”的表与索引，避免把生产全量 schema 复制进来
  - 修改后优先跑 `FullFlowIntegrationTest` 验证

---

## 常见问题与排障

### 1) Context 启动卡死/超时

优先检查是否误连外部依赖（例如真实 Redis）。

- 看 `application-test.yml` 的 `spring.autoconfigure.exclude` 是否覆盖到了对应 auto-config。
- 看是否有生产侧配置未加 `@Profile("!test")` / 未做条件化装配。

### 2) Bean 冲突（多模块 classpath 常见）

表现：同名 Controller/Bean 重复定义。

处理原则：

- 通过显式 Bean 命名区分（不要依赖默认 beanName）。
- 测试侧 `allow-bean-definition-overriding` 只能作为兜底，不作为长期方案。

### 3) @MockBean 歧义（同类型多个 Bean）

表现：`@MockBean RestTemplate` 报 multiple candidates。

处理原则：

- 用 `@MockBean(name = "...")` 精确指定要替换的 Bean。

### 4) Flyway 迁移冲突

表现：同版本 migration 重复导致启动失败。

处理原则（集成测试）：

- 默认在 test profile 禁用 Flyway，使用 `schema-h2.sql` 初始化。
- 如后续要启用 Flyway，需要将 test 的 `flyway.locations` 收敛到“测试专用目录”，并避免与各模块生产迁移混跑。

### 5) 第三方 SDK（例如支付渠道）导致签名/密钥异常

处理原则：

- 测试侧 mock SDK client/factory，返回固定结果。
- 不在仓库中硬编码任何密钥或沙箱证书。

---

## 相关文档

- [统一测试指南](UNIFIED-TESTING-GUIDE.md)
- [测试修复指南](TEST-FIX-GUIDE.md)
