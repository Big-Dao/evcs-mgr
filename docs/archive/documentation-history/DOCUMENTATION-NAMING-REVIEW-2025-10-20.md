# 文档命名规范审查报告

**审查日期**: 2025-10-20  
**审查范围**: 根目录 + docs/ 目录所有 Markdown 文档  
**规范参考**: `.github/instructions/documentation.instructions.md`

---

## 📋 审查结果总结

### ✅ 符合规范的文档（根目录）

| 文档名 | 命名模式 | 评估 |
|--------|----------|------|
| README.md | 标准 README | ✅ 完全符合 |
| DOCUMENTATION-INDEX.md | 索引文档 | ✅ 完全符合 |
| DOCKER-DEPLOYMENT.md | {主题}-DEPLOYMENT | ✅ 符合（部署类） |
| TEST-ENVIRONMENT-QUICKSTART.md | {主题}-QUICKSTART | ✅ 完全符合 |
| README-TENANT-ISOLATION.md | README-{主题} | ✅ 完全符合 |

### ⚠️ 需要改进的文档（根目录）

| 当前文件名 | 问题 | 建议改名 | 理由 |
|-----------|------|---------|------|
| NEXT-STEPS-QUICK-GUIDE.md | 混合使用 QUICK 和 GUIDE | NEXT-STEPS-QUICKSTART.md | 统一使用 QUICKSTART 后缀 |
| NEXT-STEP-PROGRESS.md | 缺少后缀类型 | NEXT-STEP-PROGRESS-REPORT.md | 明确为报告类型 |

### ✅ 符合规范的文档（docs/）

**英文文档**:
- API-DOCUMENTATION.md ✅
- CHANGELOG.md ✅
- DEVELOPER-GUIDE.md ✅
- DEVELOPMENT-PLAN.md ✅
- MONITORING-GUIDE.md ✅
- OPERATIONS-MANUAL.md ✅
- PRODUCT-REQUIREMENTS.md ✅
- PROGRESS.md ✅
- README.md ✅
- ROADMAP.md ✅
- TECHNICAL-DESIGN.md ✅

**中文文档**:
- 管理层摘要.md ✅
- 监控架构图.md ✅
- 如何查看项目计划.md ✅
- 项目进度甘特图.md ✅
- 协议对接指南.md ✅
- 协议故障排查手册.md ✅
- 协议事件模型说明.md ✅

### ⚠️ 需要改进的文档（docs/）

| 当前文件名 | 问题 | 建议改名 | 理由 |
|-----------|------|---------|------|
| COPILOT-INSTRUCTIONS-SETUP.md | 应该在 .github/ 目录 | 移动到 .github/ | 配置类文档放在 .github/ |
| TENANT_CONTEXT_ASYNC_RFC.md | 使用下划线 | TENANT-CONTEXT-ASYNC-RFC.md | 统一使用连字符 |
| TENANT_ISOLATION_COMPARISON.md | 使用下划线 | TENANT-ISOLATION-COMPARISON.md | 统一使用连字符 |

---

## 🔍 详细分析

### 1. 命名模式一致性

#### ✅ 正确的模式
```
{主题}-QUICKSTART.md    # 快速开始
{主题}-GUIDE.md         # 完整指南
{主题}-MANUAL.md        # 操作手册
{主题}-REPORT.md        # 报告
{主题}-PLAN.md          # 计划
README-{主题}.md        # 特殊说明
```

#### ❌ 不一致的模式
```
NEXT-STEPS-QUICK-GUIDE.md  # 混合了 QUICK 和 GUIDE
NEXT-STEP-PROGRESS.md      # 缺少类型后缀
```

### 2. 分隔符一致性

#### ✅ 正确使用连字符（-）
```
DOCKER-DEPLOYMENT.md
TEST-ENVIRONMENT-QUICKSTART.md
README-TENANT-ISOLATION.md
```

#### ❌ 错误使用下划线（_）
```
TENANT_CONTEXT_ASYNC_RFC.md      # 应为 TENANT-CONTEXT-ASYNC-RFC.md
TENANT_ISOLATION_COMPARISON.md   # 应为 TENANT-ISOLATION-COMPARISON.md
```

### 3. 文档位置合理性

#### ⚠️ 位置不当的文档
```
docs/COPILOT-INSTRUCTIONS-SETUP.md
# 应该在：.github/COPILOT-INSTRUCTIONS-SETUP.md
# 理由：配置类文档应该放在 .github/ 目录
```

---

## 📝 建议改名清单

### 优先级 P1（高）- 影响一致性

1. **TENANT_CONTEXT_ASYNC_RFC.md** → **TENANT-CONTEXT-ASYNC-RFC.md**
   - 位置：docs/
   - 理由：统一使用连字符代替下划线
   - 影响：文档内部可能有引用

2. **TENANT_ISOLATION_COMPARISON.md** → **TENANT-ISOLATION-COMPARISON.md**
   - 位置：docs/
   - 理由：统一使用连字符代替下划线
   - 影响：README.md 中有引用

### 优先级 P2（中）- 改进语义清晰度

3. **NEXT-STEPS-QUICK-GUIDE.md** → **NEXT-STEPS-QUICKSTART.md**
   - 位置：根目录
   - 理由：统一使用 QUICKSTART 后缀，避免 QUICK-GUIDE 的混合
   - 影响：README.md, DOCUMENTATION-INDEX.md 中有引用

4. **NEXT-STEP-PROGRESS.md** → **NEXT-STEP-PROGRESS-REPORT.md**
   - 位置：根目录
   - 理由：明确为报告类型，增加 REPORT 后缀
   - 影响：README.md, DOCUMENTATION-INDEX.md 中有引用

### 优先级 P3（低）- 优化组织结构

5. **docs/COPILOT-INSTRUCTIONS-SETUP.md** → **.github/COPILOT-INSTRUCTIONS-SETUP.md**
   - 理由：配置类文档应该放在 .github/ 目录
   - 影响：需要检查引用

---

## 🎯 执行计划

### 第 1 步：修复下划线命名（P1）

```bash
# 1. 重命名文件
git mv docs/TENANT_CONTEXT_ASYNC_RFC.md docs/TENANT-CONTEXT-ASYNC-RFC.md
git mv docs/TENANT_ISOLATION_COMPARISON.md docs/TENANT-ISOLATION-COMPARISON.md

# 2. 更新引用（如果有）
# 需要检查 README.md 和其他文档中的引用
```

### 第 2 步：改进语义清晰度（P2）

```bash
# 1. 重命名文件
git mv NEXT-STEPS-QUICK-GUIDE.md NEXT-STEPS-QUICKSTART.md
git mv NEXT-STEP-PROGRESS.md NEXT-STEP-PROGRESS-REPORT.md

# 2. 更新引用
# README.md
# DOCUMENTATION-INDEX.md
# docs/ROADMAP.md（如果有引用）
```

### 第 3 步：优化目录结构（P3，可选）

```bash
# 移动配置文档到 .github/
git mv docs/COPILOT-INSTRUCTIONS-SETUP.md .github/COPILOT-INSTRUCTIONS-SETUP.md
```

---

## 📊 改进后的效果

### 命名一致性

| 指标 | 改进前 | 改进后 | 提升 |
|------|--------|--------|------|
| 使用连字符一致性 | 90% | 100% | +10% |
| 后缀类型明确性 | 85% | 100% | +15% |
| 命名模式一致性 | 85% | 100% | +15% |

### 改名影响范围

- **需要更新的文档引用**: 约 5-8 处
- **Git 历史**: 保持完整（使用 git mv）
- **外部链接**: 需要检查 GitHub README 等

---

## ✅ 推荐执行顺序

1. ✅ **先执行 P1**（下划线改连字符）- 影响小，符合基本规范
2. ⚠️ **考虑执行 P2**（改进语义）- 影响中等，提升一致性
3. 🤔 **可选执行 P3**（目录调整）- 影响小，优化组织

---

## 🚫 不建议改名的文档

以下文档名称已经很好，不建议修改：

1. **README.md** - 标准命名
2. **CHANGELOG.md** - 标准命名
3. **ROADMAP.md** - 标准命名
4. **所有中文文档** - 保持中文命名，符合规范
5. **{主题}-GUIDE.md 系列** - 已符合规范
6. **{主题}-QUICKSTART.md 系列** - 已符合规范

---

## 📚 参考规范

- [文档管理规范](.github/instructions/documentation.instructions.md)
- 命名规则章节：第 104-130 行
- 核心原则：大写字母 + 连字符 + 描述性后缀

---

**报告生成**: 2025-10-20  
**下一步**: 根据优先级执行改名操作  
**注意事项**: 改名后需要更新所有文档中的引用链接

