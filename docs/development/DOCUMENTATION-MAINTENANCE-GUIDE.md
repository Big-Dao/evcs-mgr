# 文档维护指南（统一入口策略)

版本：v1.1｜最后更新：2025-11-10｜维护：文档协作组｜状态：活跃

本指南定义 EVCS 项目文档的维护流程与标准，并明确“单一来源（SSOT）”策略。

## 概述
- 目标：降低维护成本，避免内容漂移，提高可发现性与一致性。
- 方法：统一入口 + 指针文件；规范正文只在 SSOT 维护，不在多处复制。

## 文档分类与责任
### 核心规范（高优先级）
| 文档 | 频率 | 负责人 | 审核 |
|---|---|---|---|
| `docs/overview/PROJECT-CODING-STANDARDS.md` | 每月 | 技术负责人 | 架构师 |
| `docs/development/API-DESIGN-STANDARDS.md` | 每月 | API 负责人 | 技术负责人 |
| `docs/development/DATABASE-DESIGN-STANDARDS.md` | 每月 | DBA | 技术负责人 |
| `docs/testing/unified-testing-guide.md` | 每月 | 测试负责人 | 技术负责人 |

### 操作指南（中优先级）
| 文档 | 频率 | 负责人 | 审核 |
|---|---|---|---|
| `docs/deployment/DEPLOYMENT-GUIDE.md` | 每季度 | DevOps | 运维负责人 |
| `docs/deployment/docker-configuration-guide.md` | 每季度 | DevOps | 运维负责人 |
| `docs/development/CODE-QUALITY-CHECKLIST.md` | 每月 | 质量负责人 | 技术负责人 |

### 助手配置（高优先级）
| 指针文件 | 频率 | 负责人 | 审核 |
|---|---|---|---|
| `.codex/project-context.md` | 每月 | 技术负责人 | 架构师 |
| `.claude/project-instructions.md` | 每月 | 技术负责人 | 架构师 |
| `.github/*`（Copilot 说明） | 每月 | 技术负责人 | 架构师 |

## 维护原则
- 统一入口与单一来源：
  - 编码与架构规范：`docs/overview/PROJECT-CODING-STANDARDS.md`
  - 助手统一配置：`docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md`
  - 项目共享上下文：`.ai-assistant-config/SHARED-PROJECT-CONTEXT.md`
- 目录 README：仅保留简短索引与关键链接，不写冗长清单。
- 历史/阶段性文档：迁移至 `docs/archive/`，在相关指南中留一个“档案入口”链接即可。
- 指针文件：只保留引用路径，不复制正文。

## 文档命名规范（新增）
- 文件名风格：统一使用小写加连字符（kebab-case），示例：`deployment-guide.md`、`technical-design.md`
- 语言与字符：仅使用 ASCII 字符；避免空格、中文标点、emoji、混合编码；必要中文内容放在正文而非文件名
- 语义化简洁：文件名不超过 4 个词；用明确主题词，如 `quick-start.md`、`unified-testing-guide.md`
- 目录前缀（可选）：按域组织如 `architecture/`、`deployment/`、`testing/`、`operations/`、`development/`
- 归档命名：批次目录使用 `docs/archive/<topic>-<YYYY-MM-DD>/` 或 `<domain>-docs-cleanup-<YYYY-MM-DD>/`，归档内文件名同样使用小写连字符
- 链接更新：重命名后必须同步更新所有引用链接与索引；在 `docs/references/CHANGELOG.md` 记录命名统一变更

## 文档更新流程
### 1. 日常维护（持续）
- 触发条件：
  - 代码架构更新、API 更新、数据库结构更新
  - 新增功能模块、发现文档错误或过时信息
- 更新步骤：
  1. 识别变更需求并创建任务（项目管理工具）
  2. 更新文档与引用，检查格式与链接
  3. 提交评审（PR 指定审核人）
  4. 合并与发布（更新版本信息并通知相关人员）

### 2. 定期审查（每月/每季度）
- 月度清单：
  - 检查核心规范文档时效性；验证代码示例；检查链接有效性
  - 确认技术版本信息为最新；检查助手配置是否需更新；更新文档版本与日期
- 季度清单：
  - 全面检查文档架构合理性与覆盖度；收集反馈与使用情况
  - 制定改进计划；清理过时与重复文档

## 文档质量标准
- 内容质量：准确性、完整性、清晰性、一致性、时效性
- 格式规范：
  - 统一标题与版本信息；代码块标注语言；路径使用仓库相对地址
  - 命令示例：
    ```bash
    # 命令行示例
    docker compose -f docker-compose.core-dev.yml up -d
    ```

## 文档维护工具
- 链接检查脚本建议：`scripts/check-doc-links.sh`（内部/外部链接校验）
- 格式检查脚本建议：`scripts/check-doc-format.sh`（标题、版本信息、代码块语言）
- 自动生成示例：OpenAPI、Schema 文档生成等（置于各模块的构建脚本中）

## 档案索引（新增）
- 档案根目录：`docs/archive/README.md`
- 分类示例：
  - 文档重组与清理：`docs/archive/duplicate-docs-cleanup-*`、`docs/archive/documentation-docs-cleanup-*`
  - 部署文档历史：`docs/archive/deployment-docs-cleanup-*`
  - 测试文档历史：`docs/archive/testing-docs-cleanup-*`
  - 过时文档归档：`docs/archive/obsolete-docs-*`
- 维护要求：归档文件保留原始时间戳与说明，不在主目录重复引用；在对应指南的 README 添加“档案入口”链接。
