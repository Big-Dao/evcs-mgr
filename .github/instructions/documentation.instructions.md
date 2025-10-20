---
applyTo: "**/*.md"
priority: high
---

# 文档管理规范

本规范适用于所有 Markdown 文档的创建、修改、组织和维护，遵循业界最佳实践。

---

## 🎯 核心原则

### 1. 单一职责原则（SRP）
- 每个文档只关注一个主题或功能领域
- 避免将不相关的内容混在一起
- 大型文档应拆分为多个专题文档

### 2. DRY 原则（Don't Repeat Yourself）
- 避免在多个文档中重复相同内容
- 使用交叉引用链接到权威文档
- 如发现重复内容，立即合并或重构

### 3. 最小惊讶原则
- 文档应放在用户期望找到的位置
- 命名应清晰表达文档内容
- 遵循项目既定的文档结构

---

## 📁 文档结构规范

### 根目录文档（仅限核心文档）

**允许的文档类型**：
- ✅ `README.md` - 项目主页和快速开始
- ✅ `DOCUMENTATION-INDEX.md` - 文档导航总索引
- ✅ `*-QUICKSTART.md` - 5-10 分钟快速开始指南
- ✅ `*-DEPLOYMENT.md` - 快速部署指南
- ✅ 关键技术说明（如 `README-TENANT-ISOLATION.md`）
- ✅ 项目当前进度追踪（如 `NEXT-STEP-PROGRESS-REPORT.md`）

**禁止的文档类型**：
- ❌ 临时进度报告（应放在 `docs/archive/progress-reports/`）
- ❌ 详细指南（应放在 `docs/` 相应主题目录）
- ❌ 会议记录、草稿、个人笔记
- ❌ 过时的文档版本

**规则**：根目录文档数量不应超过 **10 个**。

---

### docs/ 目录结构

```
docs/
├── README.md                    # docs 目录说明
├── deployment/                  # 🚢 部署相关
│   ├── README.md
│   ├── DEPLOYMENT-GUIDE.md      # 完整部署指南
│   └── TEST-ENVIRONMENT-GUIDE.md
│
├── testing/                     # 🧪 测试相关
│   ├── README.md
│   ├── TESTING-FRAMEWORK-GUIDE.md
│   ├── TESTING-GUIDE.md
│   └── TEST-*-REPORT.md         # 测试报告
│
├── development/                 # 👨‍💻 开发相关
│   ├── README.md
│   ├── DEVELOPER-GUIDE.md
│   └── IDE-FIX-GUIDE.md
│
├── architecture/                # 🏗️ 架构设计（可选）
│   ├── README.md
│   ├── TECHNICAL-DESIGN.md
│   └── *.puml / *.drawio        # 架构图
│
├── api/                         # 📡 API 文档（可选）
│   ├── README.md
│   └── API-DOCUMENTATION.md
│
├── archive/                     # 📦 归档区
│   ├── README.md
│   ├── documentation-history/   # 文档维护历史
│   │   └── YYYY-MM-DD-*.md
│   └── progress-reports/        # 进度报告归档
│       └── YYYY-MM/
│           └── *.md
│
├── ROADMAP.md                   # 项目路线图
├── CHANGELOG.md                 # 版本变更日志
├── PROGRESS.md                  # 项目总体进度
└── [其他核心文档]
```

**目录原则**：
- 每个子目录必须有 `README.md` 说明目录用途
- 按主题分类，不按时间或人员分类
- 归档目录按时间组织（YYYY-MM 格式）

---

## 📝 文档命名规范

### 命名模式

| 类型 | 命名格式 | 示例 |
|------|----------|------|
| **快速开始** | `{主题}-QUICKSTART.md` | `TEST-ENVIRONMENT-QUICKSTART.md` |
| **完整指南** | `{主题}-GUIDE.md` | `DEPLOYMENT-GUIDE.md` |
| **技术说明** | `README-{主题}.md` | `README-TENANT-ISOLATION.md` |
| **报告** | `{主题}-REPORT.md` | `TEST-COVERAGE-REPORT.md` |
| **总结** | `{主题}-SUMMARY.md` | `CLEANUP-SUMMARY-2025-10-20.md` |
| **状态** | `{主题}-STATUS.md` | `DEPLOYMENT-STATUS.md` |
| **计划** | `{主题}-PLAN.md` | `MIGRATION-PLAN.md` |

### 命名规则

1. **使用大写字母和连字符**：`DEPLOYMENT-GUIDE.md`（英文文档）
2. **保持中文文件名**：`协议对接指南.md`（中文文档）
3. **日期格式**：`YYYY-MM-DD` 格式，放在文件名末尾
   - ✅ `CLEANUP-SUMMARY-2025-10-20.md`
   - ❌ `2025-10-20-CLEANUP-SUMMARY.md`
4. **避免版本号**：使用 Git 管理版本，不在文件名中包含 `v1`、`v2`
5. **描述性命名**：文件名应清晰表达内容
   - ✅ `TESTING-FRAMEWORK-GUIDE.md`
   - ❌ `test.md` 或 `guide.md`

---

## ✍️ 文档内容规范

### 文档头部（必需）

每个文档必须包含以下元数据：

```markdown
# 文档标题

> **简短描述**（一句话说明文档用途）

**最后更新**: YYYY-MM-DD  
**维护者**: 团队名/负责人  
**状态**: [草稿/审核中/已发布/已归档]

---

## 目录（文档超过 200 行时必需）

- [章节1](#章节1)
- [章节2](#章节2)
```

### 内容质量标准

#### ✅ DO - 推荐做法

1. **使用清晰的层级结构**
   - H1 (`#`) - 文档标题（仅一个）
   - H2 (`##`) - 主要章节
   - H3 (`###`) - 子章节
   - 最多使用 4 级标题

2. **提供代码示例**
   ```java
   // ✅ 正确 - 包含注释和上下文
   @Service
   public class StationService extends ServiceImpl<StationMapper, Station> {
       // 查询时自动应用租户过滤
       @DataScope(DataScopeType.TENANT)
       public List<Station> list() {
           return list(Wrappers.lambdaQuery());
       }
   }
   ```

3. **使用表格对比信息**
   | 特性 | 方案A | 方案B |
   |------|-------|-------|
   | 性能 | 高 | 中 |
   | 复杂度 | 低 | 高 |

4. **添加视觉辅助**
   - 使用 emoji 增强可读性（适度）：✅ ❌ ⚠️ 📝 🚀
   - 使用引用块突出重要信息
   - 使用代码块标注语言：\`\`\`java, \`\`\`bash

5. **提供交叉引用**
   ```markdown
   详见 [部署指南](docs/deployment/DEPLOYMENT-GUIDE.md)
   ```

6. **包含具体示例**
   - 不要只说"配置数据库"，提供具体配置示例
   - 不要只说"运行测试"，提供具体命令

#### ❌ DON'T - 避免做法

1. **避免模糊描述**
   - ❌ "配置好环境"
   - ✅ "安装 Java 21 并设置 JAVA_HOME 环境变量"

2. **避免过时信息**
   - 文档中提到的版本号、命令、配置应保持最新
   - 发现过时内容立即更新或标注

3. **避免使用"我们"、"你"等口语**
   - ❌ "我们可以这样做..."
   - ✅ "使用以下步骤..."

4. **避免重复内容**
   - 不要复制粘贴大段内容到多个文档
   - 使用链接引用权威文档

5. **避免无意义的占位符**
   - ❌ "TODO: 待完善"、"敬请期待"
   - ✅ 要么写完整，要么不写

---

## 🔄 文档生命周期管理

### 创建新文档

**检查清单**：
- [ ] 确认没有现有文档覆盖相同主题
- [ ] 选择正确的目录位置
- [ ] 使用规范的命名格式
- [ ] 添加完整的文档头部（标题、描述、更新日期）
- [ ] 如果文档 > 200 行，添加目录
- [ ] 在 `DOCUMENTATION-INDEX.md` 中添加条目
- [ ] 提交时使用 `docs:` 前缀的 Conventional Commit

**Git 提交消息**：
```
docs: 添加 XXX 文档

- 创建 docs/testing/XXX.md
- 包含完整的 YYY 示例
- 更新文档索引
```

---

### 更新现有文档

**原则**：
- 小改动直接修改，大改动考虑重构
- 更新 "最后更新" 日期
- 在 CHANGELOG.md 中记录重大变更

**检查清单**：
- [ ] 验证所有链接仍然有效
- [ ] 验证代码示例可运行
- [ ] 验证版本号和配置正确
- [ ] 更新"最后更新"日期
- [ ] 如果是重大变更，更新 CHANGELOG.md

**Git 提交消息**：
```
docs: 更新 XXX 文档

- 更新部署步骤以支持 Java 21
- 修正错误的配置示例
- 添加故障排查章节
```

---

### 合并重复文档

**触发条件**：
- 发现两个文档 > 50% 内容重叠
- 多个文档讲述同一主题

**合并流程**：
1. 创建新的合并文档，整合所有有价值内容
2. 在新文档中添加"合并说明"
3. 删除原重复文档
4. 更新所有指向旧文档的链接
5. 记录在归档历史中

**合并说明示例**：
```markdown
## 文档历史

本文档由以下文档合并而成（2025-10-20）：
- `TEST-FRAMEWORK-SUMMARY.md` - 测试框架总结
- `TESTING-QUICKSTART.md` - 测试快速开始

合并理由：两个文档内容重复度高，整合后更便于维护。
```

---

### 归档过时文档

**归档条件**（满足任一）：
- 文档描述的功能已废弃
- 阶段性任务已完成（如 P3 规划）
- 临时性质的进度报告（超过 1 个月）
- 被更新版本替代

**归档流程**：
1. 移动到 `docs/archive/` 相应目录
   - 文档历史 → `documentation-history/`
   - 进度报告 → `progress-reports/YYYY-MM/`
2. 在原位置添加重定向说明（如果仍有价值）
3. 从 `DOCUMENTATION-INDEX.md` 中移除
4. Git 提交说明归档原因

**Git 提交消息**：
```
docs: 归档 P3 阶段文档

- 移动 P3 规划文档到 archive/progress-reports/2025-10/
- P3 阶段已完成，保留作为历史记录
- 更新文档索引
```

---

### 删除文档

**删除条件**（谨慎操作）：
- 文档包含错误信息且无历史价值
- 测试/实验性质的临时文档
- 个人笔记或草稿

**删除前必须**：
- 与团队确认无人需要
- 检查是否有其他文档引用
- 考虑归档而非删除

---

## 🔍 文档维护

### 定期审查（推荐频率）

| 频率 | 审查内容 | 负责人 |
|------|----------|--------|
| **每周** | 根目录文档数量（不超过 10 个） | AI Assistant |
| **每月** | 临时文档是否需要归档 | 项目经理 |
| **每季度** | 重复内容检查和合并 | 技术主管 |
| **每版本** | 版本相关信息更新 | 开发团队 |

### 质量检查清单

在提交文档变更前，检查：
- [ ] 所有链接可用（内部链接和外部链接）
- [ ] 代码示例经过验证
- [ ] 文件名符合命名规范
- [ ] 文档位置正确
- [ ] 已更新文档索引
- [ ] 没有拼写错误
- [ ] 格式一致（标题、列表、代码块）
- [ ] "最后更新"日期正确

---

## 📊 文档度量指标

### 健康指标

| 指标 | 健康值 | 警告值 |
|------|--------|--------|
| 根目录文档数 | ≤ 10 | > 10 |
| 平均文档长度 | 200-800 行 | > 1500 行 |
| 文档更新频率 | 每月 ≥ 1 次 | > 3 个月无更新 |
| 死链比例 | 0% | > 5% |
| 重复内容 | 0 | > 2 组 |

### 定期报告

每季度生成文档健康报告：
- 文档总数和分布
- 过时文档列表（> 6 个月未更新）
- 死链列表
- 重复内容检测
- 改进建议

---

## 🤖 AI 助手（GitHub Copilot）特殊规范

### 创建文档时

1. **必须先检查现有文档**
   ```
   我需要创建 XXX 文档前，先检查：
   - 是否已有类似文档？
   - 应该放在哪个目录？
   - 需要合并现有文档吗？
   ```

2. **必须遵循命名规范**
   - 不要创建 `doc.md`、`notes.md` 等模糊命名
   - 使用规范的命名格式

3. **必须更新索引**
   - 创建文档后立即更新 `DOCUMENTATION-INDEX.md`

### 修改文档时

1. **保持现有风格**
   - 保持文档现有的格式和结构
   - 不要大幅改变已有的组织方式

2. **增量更新**
   - 小改动直接修改
   - 大改动先询问用户

3. **验证链接**
   - 修改后验证所有相关链接
   - 更新"最后更新"日期

### 清理文档时

1. **必须先制定计划**
   - 创建 `CLEANUP-PLAN-YYYY-MM-DD.md`
   - 列出要归档/合并/删除的文档
   - 说明原因和影响

2. **执行前确认**
   - 向用户确认清理计划
   - 获得批准后再执行

3. **完成后总结**
   - 创建 `CLEANUP-SUMMARY-YYYY-MM-DD.md`
   - 量化清理效果
   - 归档计划和总结文档

### 禁止行为

- ❌ 不要创建未经讨论的新文档结构
- ❌ 不要删除看起来"过时"的文档（应归档）
- ❌ 不要大规模重命名文档（会破坏链接）
- ❌ 不要在未告知用户的情况下合并文档
- ❌ 不要创建临时文档在根目录

---

## 📖 参考资源

### 业界最佳实践

- [Google Technical Writing Courses](https://developers.google.com/tech-writing)
- [Write the Docs](https://www.writethedocs.org/guide/)
- [Microsoft Style Guide](https://learn.microsoft.com/en-us/style-guide/welcome/)
- [Docs as Code](https://www.writethedocs.org/guide/docs-as-code/)

### 项目相关

- [DOCUMENTATION-INDEX.md](../../DOCUMENTATION-INDEX.md) - 文档总索引
- [.github/MAINTENANCE.md](../MAINTENANCE.md) - .github 维护指南
- [docs/archive/documentation-history/](../../docs/archive/documentation-history/) - 文档维护历史

---

## 🔄 本规范的维护

- **版本**: v1.0
- **生效日期**: 2025-10-20
- **审查周期**: 每 6 个月
- **维护者**: 技术主管 + AI Assistant
- **反馈渠道**: GitHub Issues 或团队会议

### 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2025-10-20 | v1.0 | 初始版本，基于项目文档清理经验制定 |

---

**最后更新**: 2025-10-20  
**状态**: 已发布  
**适用范围**: 全项目所有 Markdown 文档

