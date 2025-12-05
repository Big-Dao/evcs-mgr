# 代码质量检查清单（收敛版）

版本：v1.2｜最后更新：2025-12-05｜维护：技术负责人｜状态：活跃

用途：统一代码审核、质量保障与自动化验收要点。规范性条目（命名、异常、日志、分层、禁止模式等）集中维护于 `docs/overview/PROJECT-CODING-STANDARDS.md`（SSOT），此清单仅保留检查项与执行方式。

## 强制要求（必须满足）
- 架构与分层：无跨服务直接数据访问；Controller → Service → Repository → Entity 分层清晰
- 安全与配置：输入校验（`@Valid`）、鉴权（`@PreAuthorize`）、敏感配置走环境变量，JWT 密钥不硬编码
- 事务与一致性：更新操作使用 `@Transactional`；缓存失效（`@CacheEvict`）与审计字段一致化
- 异常与日志：`@ControllerAdvice` 统一异常；关键路径记录结构化日志，避免空 `catch`
- 命名与格式：遵循项目命名与格式规范；公共 API 含 JavaDoc；避免魔法数字/字符串

## 重要要求（强烈推荐）
- 性能优化：避免 N+1 查询；合理使用 Spring Cache；大数据查询进行分页与过滤
- 测试覆盖：Service 单元测试 ≥ 80%；Controller 集成测试 ≥ 70%；包含边界与异常用例
- 监控与健康：关键操作含性能指标；配置 Actuator 健康检查；接口错误码与日志字段一致

## 建议要求（推荐遵循）
- 可维护性：方法与类长度适中；参数数量合理；复杂逻辑拆分
- 文档与变更：重要功能更新 README/API 文档；重大变更记录在 CHANGELOG

## 执行方式（示例）
- 静态检查：SpotBugs/Checkstyle 均通过
- 安全扫描：依赖漏洞扫描（如 OWASP Dependency-Check）通过
- 测试执行：`./gradlew test` 通过，覆盖率达标（本项目阈值以团队约定为准）
- 日志审计：检查敏感信息未出现在 `INFO` 级别；DEBUG 受配置门控

备注：规范新增或调整只在 SSOT 文档维护；此清单随规范更新同步调整。
