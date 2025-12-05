# 开发者指南（收敛版）

版本：v1.2｜最后更新：2025-12-05｜维护：技术负责人｜状态：活跃

用途：描述项目开发流程、工具链与协作规范。通用编码/分层/异常/日志等规范集中维护于 `docs/overview/PROJECT-CODING-STANDARDS.md`（SSOT）。

## 快速开始
- 环境准备：JDK 21、Gradle、Docker、Git
- 克隆与构建：`./gradlew build`（可用 `-x test` 跳过测试）
- 运行服务：`./gradlew :evcs-gateway:bootRun` 等
- 演示数据导入：参见 `docs/deployment/DEPLOYMENT-GUIDE.md` 的示例脚本

## 项目结构
- 根构建：`build.gradle`、`settings.gradle`
- 服务模块与公共组件：按微服务拆分，遵循分层架构

## 编码规范（请参见 SSOT）
- 命名、异常、日志、参数校验、分层架构、禁止模式等统一参见：`docs/overview/PROJECT-CODING-STANDARDS.md`

## 提交流程
- 分支管理与 Commit 信息格式，代码评审与测试要求

## 工具链
- IDE（IntelliJ/Eclipse）、构建工具、静态检查、单元测试

## 提示
- 质量门禁、性能与缓存、监控与告警
