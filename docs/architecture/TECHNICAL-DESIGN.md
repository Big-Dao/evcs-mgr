# 技术设计（摘要）

版本：v2.2｜最后更新：2025-12-05｜维护：架构团队｜状态：活跃

用途：提供关键技术选型与设计原则的简要指引；详细技术方案与历史版本请见归档与 SSOT。

- 单一来源（SSOT）：`docs/overview/PROJECT-CODING-STANDARDS.md`
- 历史与详细设计：`docs/archive/documentation-docs-cleanup-2025-12-05/TECHNICAL-DESIGN.md`

要点：
- 分层与依赖：Controller → Service → Domain/Repository；禁止跨边界访问
- 多租户：上下文传播与仓储过滤（详见 `docs/architecture/TENANT-CONTEXT-ASYNC-RFC.md`）
- 缓存与性能：明确 TTL 与监控指标；避免 N+1 查询
- 异步与消息：统一封装（Kafka/RabbitMQ），禁止裸线程
