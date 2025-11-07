# EVCS文档维护指南

> **版本**: v1.0 | **更新日期**: 2025-11-07
>
> 📋 **本文档定义EVCS项目文档的维护流程和标准**

## 🎯 概述

本文档建立了EVCS充电站管理系统的文档维护机制，确保项目文档始终保持最新、准确和一致的状态。

## 📋 文档分类和维护责任

### 核心规范文档（高优先级）
| 文档名称 | 维护频率 | 负责人 | 审核人 |
|----------|----------|--------|--------|
| [PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md) | 每月 | 技术负责人 | 架构师 |
| [API-DESIGN-STANDARDS.md](API-DESIGN-STANDARDS.md) | 每月 | API负责人 | 技术负责人 |
| [DATABASE-DESIGN-STANDARDS.md](DATABASE-DESIGN-STANDARDS.md) | 每月 | DBA | 技术负责人 |
| [UNIFIED-TESTING-GUIDE.md](../testing/UNIFIED-TESTING-GUIDE.md) | 每月 | 测试负责人 | 技术负责人 |

### 操作指南文档（中优先级）
| 文档名称 | 维护频率 | 负责人 | 审核人 |
|----------|----------|--------|--------|
| [DEPLOYMENT-GUIDE.md](../deployment/DEPLOYMENT-GUIDE.md) | 每季度 | DevOps工程师 | 运维负责人 |
| [DOCKER-CONFIGURATION-GUIDE.md](../deployment/DOCKER-CONFIGURATION-GUIDE.md) | 每季度 | DevOps工程师 | 运维负责人 |
| [CODE-QUALITY-CHECKLIST.md](CODE-QUALITY-CHECKLIST.md) | 每月 | 质量负责人 | 技术负责人 |

### AI助手配置（高优先级）
| 配置文件 | 维护频率 | 负责人 | 审核人 |
|----------|----------|--------|--------|
| [.claude/project-instructions.md](../../.claude/project-instructions.md) | 每月 | 技术负责人 | 架构师 |
| [.github/copilot-instructions.md](../../.github/copilot-instructions.md) | 每月 | 技术负责人 | 架构师 |
| [.codex/project-context.md](../../.codex/project-context.md) | 每月 | 技术负责人 | 架构师 |

## 🔄 文档更新流程

### 1. 日常维护（持续）

#### 触发条件
- 代码架构变更
- API接口变更
- 数据库结构变更
- 新增功能模块
- 发现文档错误或过时信息

#### 更新步骤
```markdown
1. **识别变更需求**
   - 代码审查时发现文档需要更新
   - 测试失败时发现文档不匹配
   - 部署问题时发现配置文档不准确

2. **创建更新任务**
   - 在项目管理工具中创建文档更新任务
   - 标记优先级和预计完成时间

3. **更新文档内容**
   - 修改相关文档
   - 更新相关引用
   - 检查格式和链接

4. **验证更新效果**
   - 检查文档准确性
   - 验证链接有效性
   - 确认格式正确

5. **提交审核**
   - 提交Pull Request
   - 指定审核人员
   - 等待审核通过

6. **合并和发布**
   - 合并到主分支
   - 更新版本信息
   - 通知相关人员
```

### 2. 定期审查（月度/季度）

#### 月度审查清单
```markdown
- [ ] 检查所有核心规范文档的时效性
- [ ] 验证文档中的代码示例是否正确
- [ ] 检查文档链接是否有效
- [ ] 确认技术版本信息是否最新
- [ ] 检查AI助手配置是否需要更新
- [ ] 更新文档版本和更新日期
```

#### 季度审查清单
```markdown
- [ ] 全面检查文档架构合理性
- [ ] 评估文档覆盖度是否完整
- [ ] 收集开发人员反馈
- [ ] 分析文档使用情况
- [ ] 制定文档改进计划
- [ ] 清理过时和重复文档
```

## 📊 文档质量标准

### 内容质量要求
- **准确性**: 信息与实际实现保持一致
- **完整性**: 覆盖所有必要的信息
- **清晰性**: 表达清楚，易于理解
- **一致性**: 术语、格式、风格统一
- **时效性**: 及时更新，反映最新状态

### 格式规范
```markdown
# 文档标题

> **版本**: v1.0 | **更新日期**: 2025-11-07 | **状态**: 活跃

## 🎯 概述
简要说明文档目的和范围

## 📋 主要内容
使用清晰的章节结构
适当的标题层级
代码示例和表格

## 📚 相关文档
列出相关的参考文档
```

### 代码示例规范
```markdown
```java
// ✅ 好的代码示例
@Service
@Transactional
@Slf4j
public class OrderService {

    @Cacheable(value = "orders", key = "#id")
    public OrderDTO getById(Long id) {
        // 实现代码
    }
}
```

```bash
# ✅ 命令行示例
docker-compose -f docker-compose.core-dev.yml up -d
```
```

## 🔧 文档维护工具

### 自动化检查工具

#### 链接检查
```bash
#!/bin/bash
# scripts/check-doc-links.sh

echo "🔍 检查文档链接..."

# 查找所有markdown文件
find . -name "*.md" -not -path "./.git/*" -not -path "./docs/archive/*" | while read file; do
    echo "检查文件: $file"

    # 检查内部链接
    grep -o '\[.*\]([^)]*)' "$file" | while read link; do
        target=$(echo "$link" | sed 's/\[.*\](\([^)]*\))/\1/')
        if [[ $target == http* ]]; then
            # 外部链接检查
            if ! curl -s --head "$target" > /dev/null; then
                echo "❌ 外部链接失效: $target in $file"
            fi
        else
            # 内部链接检查
            if [[ ! -f "$target" ]]; then
                echo "❌ 内部链接失效: $target in $file"
            fi
        fi
    done
done
```

#### 格式检查
```bash
#!/bin/bash
# scripts/check-doc-format.sh

echo "📝 检查文档格式..."

# 检查markdown格式
find . -name "*.md" -not -path "./.git/*" -not -path "./docs/archive/*" | while read file; do
    # 检查是否有标题
    if ! grep -q "^# " "$file"; then
        echo "❌ 缺少标题: $file"
    fi

    # 检查是否有版本信息
    if ! grep -q "版本\|version" "$file"; then
        echo "⚠️ 缺少版本信息: $file"
    fi

    # 检查代码块格式
    if grep -q '```$' "$file"; then
        echo "⚠️ 存在未指定语言的代码块: $file"
    fi
done
```

### 文档生成工具

#### API文档自动生成
```yaml
# build.gradle
openapi3 {
    setServer("http://localhost:8080")
    title = "EVCS API Documentation"
    description = "EVCS充电站管理系统API文档"
    version = "1.0.0"

    outputFileName = "evcs-api.yaml"
    outputDir = "${buildDir}/api-docs"
}
```

#### 数据库文档生成
```bash
# 生成数据库文档脚本
scripts/generate-db-docs.sh

# 使用SchemaSpy生成数据库文档
java -jar schemaspy.jar \
  -t pgsql \
  -db evcs \
  -host localhost \
  -port 5432 \
  -u evcs \
  -p evcs123 \
  -o docs/database-schema
```

## 📈 文档使用监控

### 访问统计
```yaml
# GitHub Pages配置（如果使用）
google_analytics:
  tracking_id: GA_MEASUREMENT_ID
```

### 反馈收集
```markdown
## 📞 反馈和建议

如果您在使用文档过程中发现问题或有改进建议，请：

1. **提交Issue**: [创建文档问题](https://github.com/your-org/evcs-mgr/issues/new?template=documentation.md)
2. **提交PR**: 直接修改文档并提交Pull Request
3. **联系维护者**: 通过团队沟通工具联系文档维护者

### 反馈内容模板
```markdown
## 文档反馈

**文档名称**: [文档名称和链接]
**问题描述**: [详细描述发现的问题]
**建议改进**: [如果有改进建议，请详细说明]
**联系方式**: [可选，方便后续沟通]
```

## 🚨 文档应急处理

### 文档损坏或丢失
```bash
# 1. 立即从Git历史恢复
git log --oneline -- docs/important-doc.md
git checkout <commit-hash> -- docs/important-doc.md

# 2. 检查归档目录
ls -la docs/archive/

# 3. 从备份恢复
# 从云存储或本地备份恢复
```

### 文档冲突处理
```bash
# 1. 查看冲突内容
git status
git diff docs/conflicting-doc.md

# 2. 手动解决冲突
# 编辑文件，保留正确内容

# 3. 验证解决结果
# 检查文档格式和内容

# 4. 提交解决方案
git add docs/conflicting-doc.md
git commit -m "解决文档冲突"
```

## 📚 文档模板

### 新文档模板
```markdown
# [文档标题]

> **版本**: v1.0 | **更新日期**: 2025-11-07 | **状态**: 活跃
>
> 📋 **[文档简介]**

## 🎯 概述

[简要说明文档目的、范围和目标读者]

## 📋 主要内容

[使用清晰的章节结构组织内容]

### 章节示例
[章节内容，包含必要的代码示例、图表等]

## 📚 相关文档

- [相关文档1](../path/to/doc1.md)
- [相关文档2](../path/to/doc2.md)

---

**维护信息**:
- **负责人**: [姓名]
- **审核人**: [姓名]
- **更新频率**: [月度/季度/按需]
- **相关标签**: [标签1, 标签2]
```

### 文档更新记录模板
```markdown
## 📝 更新记录

| 版本 | 更新日期 | 更新内容 | 更新人 | 审核人 |
|------|----------|----------|--------|--------|
| v1.0 | 2025-11-07 | 初始版本 | 张三 | 李四 |
| v1.1 | 2025-12-01 | 添加API示例 | 王五 | 李四 |
| v1.2 | 2026-01-15 | 更新部署流程 | 张三 | 赵六 |
```

## 🔗 自动化集成

### GitHub Actions工作流
```yaml
name: Documentation Check

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  doc-check:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Check document links
      run: ./scripts/check-doc-links.sh

    - name: Check document format
      run: ./scripts/check-doc-format.sh

    - name: Generate API docs
      run: ./gradlew generateApiDocs

    - name: Check for broken images
      run: |
        find . -name "*.md" -not -path "./.git/*" | xargs grep -o '\!\[.*\]([^)]*)' | \
        grep -E '\(https?://[^)]+\)' | cut -d'(' -f2 | cut -d')' -f1 | \
        xargs -I{} curl -s --head "{}" | grep "HTTP/.* 404"
```

### 预提交钩子
```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "📋 执行文档预提交检查..."

# 检查链接
./scripts/check-doc-links.sh

# 检查格式
./scripts/check-doc-format.sh

# 检查新增文档是否有标题
if git diff --cached --name-only --diff-filter=ACM | grep -E '\.md$'; then
    for file in $(git diff --cached --name-only --diff-filter=ACM | grep -E '\.md$'); do
        if ! grep -q "^# " "$file"; then
            echo "❌ 新文档缺少标题: $file"
            exit 1
        fi
    done
fi

echo "✅ 文档检查通过"
```

## 📊 文档KPI指标

### 质量指标
- **文档覆盖率**: 核心模块文档覆盖率 ≥ 95%
- **链接有效率**: 文档内链接有效率 ≥ 99%
- **更新及时性**: 代码变更后文档更新时间 ≤ 7天
- **准确率**: 文档信息准确率 ≥ 98%

### 使用指标
- **访问量**: 月均文档访问量
- **反馈量**: 月均文档反馈数量
- **解决率**: 文档问题解决率 ≥ 90%
- **满意度**: 开发人员对文档的满意度评分

## 🎯 持续改进

### 定期评估
- 每季度进行文档质量评估
- 收集开发人员使用反馈
- 分析文档使用数据
- 制定改进计划

### 培训和推广
- 新员工文档使用培训
- 最佳实践分享会
- 文档写作技能培训
- 工具使用培训

## 🚫 文档创建和书写的强制性规范

### ⚠️ 严禁不遵守文档规范的任何行为

**所有AI助手（Claude、GitHub Copilot、CodeX等）和开发人员必须严格遵守以下文档规范，严禁违规创建、命名或书写任何文档文件。**

### 📋 文档创建的强制性规定

#### 1. 严禁违规创建文件
```markdown
❌ 严禁行为：
- 严禁在非指定目录创建任何文档文件
- 严禁使用不符合命名规范的文件名
- 严禁创建重复或内容相似的文档
- 严禁在根目录创建技术文档（仅保留README.md）
- 严禁在archive/目录创建新文档（仅用于归档）

✅ 正确做法：
- 只能在docs/下的指定分类目录创建文档
- 必须使用语义化的小写+连字符命名
- 创建前检查是否已存在相似文档
- 技术文档必须放在合适的分类目录中
```

#### 2. 严禁违规命名
```markdown
❌ 严禁的命名方式：
- 严禁使用数字编号：01-guide.md, 02-api.md
- 严禁使用大写字母：API-Guide.md, User_Manual.md
- 严禁使用空格或特殊字符：user guide.md, api@doc.md
- 严禁使用中文标点符号：用户指南.md，开发文档.md
- 严禁使用无意义的缩写：ug.md, dev-doc.md

✅ 正确的命名方式：
- 使用小写字母+连字符：user-guide.md, api-documentation.md
- 语义化命名：清晰表达文档内容和用途
- 功能导向：按文档功能分类命名
- 简洁明了：避免过长的文件名
```

#### 3. 严禁违规书写
```markdown
❌ 严禁的书写行为：
- 严禁不包含文档标题和元信息
- 严禁使用不一致的格式和样式
- 严禁包含过时或错误的信息
- 严禁不更新相关文档引用
- 严禁不遵循文档模板规范

✅ 必须的书写要求：
- 必须包含标准的文档标题和版本信息
- 必须遵循统一的Markdown格式规范
- 必须确保信息的准确性和时效性
- 必须更新所有相关的文档链接引用
- 必须使用项目文档模板
```

### 📁 目录使用的强制性规定

#### 1. 严禁误用目录
```markdown
❌ 严禁的目录使用：
- 严禁在overview/存放技术细节文档
- 严禁在architecture/存放部署指南
- 严禁在development/存放运维文档
- 严禁在testing/存放架构设计文档
- 严禁在troubleshooting/存放正常流程文档

✅ 正确的目录使用：
- overview/: 项目概览、规范标准
- architecture/: 系统架构、设计文档
- development/: 开发指南、编程规范
- deployment/: 部署运维、环境配置
- operations/: 监控管理、运维手册
- testing/: 测试质量、测试指南
- troubleshooting/: 问题排查、故障处理
- references/: 参考资料、API文档
- archive/: 历史归档（严禁新建）
```

#### 2. 严禁破坏目录结构
```markdown
❌ 严禁的行为：
- 严禁在docs/根目录直接创建文档
- 严禁创建新的顶级目录（需团队讨论决定）
- 严禁删除或重命名现有分类目录
- 严禁在archive/目录创建非归档文件
- 严禁跨目录移动大量文档而不更新引用

✅ 正确的做法：
- 新文档必须放在合适的分类子目录中
- 如需新目录需提交团队讨论和审批
- 保持现有目录结构的稳定性
- archive/仅用于存放过时文档
- 移动文档必须同步更新所有引用链接
```

### 🔍 AI助手执行的强制性检查

#### 创建文档前的必检项
```markdown
每次创建新文档前，AI助手必须执行以下检查：

1. 📂 目录检查
   [ ] 确认文档放在正确的分类目录中
   [ ] 确认不会破坏现有目录结构
   [ ] 确认符合项目文档组织规范

2. 📝 命名检查
   [ ] 确认文件名使用小写+连字符格式
   [ ] 确认名称语义化且无歧义
   [ ] 确认不存在相似名称的文档

3. 📋 内容检查
   [ ] 确认遵循文档模板规范
   [ ] 确认包含必要的标题和元信息
   [ ] 确认内容准确且无过时信息

4. 🔗 引用检查
   [ ] 确认所有内部链接有效
   [ ] 确认相关文档已正确引用
   [ ] 确认不会产生循环引用
```

### ⚡ 违规处理机制

#### 发现违规的处理流程
```markdown
1. 立即停止违规行为
2. 删除或重命名违规创建的文件
3. 修复所有相关的引用链接
4. 提交违规报告给文档维护者
5. 在团队中分享违规案例，避免重复
```

#### AI助手违规的纠正措施
```markdown
- Claude Code: 更新project-instructions.md，加强规范约束
- GitHub Copilot: 更新copilot-instructions.md，明确禁止事项
- CodeX: 更新project-context.md，强化执行要求
- 在所有AI配置中添加文档规范的强制性条款
```

---

**通过建立完善的文档维护机制和严格的执行规范，确保EVCS项目文档始终保持高质量状态，为团队协作和项目成功提供坚实保障。所有AI助手和开发人员必须严格遵守上述规范，违规行为将立即纠正。**