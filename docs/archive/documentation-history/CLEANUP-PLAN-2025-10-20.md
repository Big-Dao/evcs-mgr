# 📋 项目文档清理计划

**制定日期**: 2025-10-20  
**执行原则**: 
- 保留最全面、最新的版本
- 删除过时和临时进度报告
- 合并重复内容
- 优化文档结构

---

## 🎯 清理目标

当前问题：
- ❌ 根目录文档过多（10+个）
- ❌ TEST 相关文档重复（3个类似文档）
- ❌ DEPLOYMENT 文档重复（根目录 + docs/）
- ❌ DOCUMENTATION 元文档应归档
- ❌ 临时进度报告未归档

清理后：
- ✅ 根目录保留核心文档（README、快速开始、索引）
- ✅ 按主题分类到 docs/ 子目录
- ✅ 临时报告归档到 docs/archive/
- ✅ 更新文档索引

---

## 📁 重复内容分析

### 1. TEST 相关文档（重复度：高）

| 文件 | 位置 | 内容重点 | 建议 |
|------|------|----------|------|
| `TEST-FRAMEWORK-SUMMARY.md` | 根目录 | 测试框架完善总结 | ✅ **保留** - 移到 docs/testing/ |
| `TEST-ENVIRONMENT-QUICKSTART.md` | 根目录 | Docker测试环境快速启动 | ✅ **保留** - 保持根目录快速访问 |
| `docs/TESTING-QUICKSTART.md` | docs/ | 测试框架代码快速上手 | ⚠️ **合并** - 整合到 TEST-FRAMEWORK-SUMMARY.md |
| `docs/TESTING-GUIDE.md` | docs/ | 完整测试指南 | ✅ **保留** - 移到 docs/testing/ |
| `docs/TESTING-IMPROVEMENTS.md` | docs/ | 测试改进计划 | ❌ **归档** - 已完成，移到 archive/ |
| `docs/TEST-ENVIRONMENT-GUIDE.md` | docs/ | 测试环境详细指南 | ✅ **保留** - 移到 docs/deployment/ |

**结论**: 
- 保留 TEST-ENVIRONMENT-QUICKSTART.md 在根目录（快速开始）
- 合并 TEST-FRAMEWORK-SUMMARY.md + TESTING-QUICKSTART.md → `docs/testing/TESTING-FRAMEWORK-GUIDE.md`
- 保留 TESTING-GUIDE.md 在 docs/testing/
- 归档 TESTING-IMPROVEMENTS.md

### 2. DEPLOYMENT 相关文档（重复度：中）

| 文件 | 位置 | 内容重点 | 建议 |
|------|------|----------|------|
| `DOCKER-DEPLOYMENT.md` | 根目录 | Docker 快速开始 | ✅ **保留** - 根目录快速访问 |
| `docs/DEPLOYMENT-GUIDE.md` | docs/ | 完整部署指南（含K8s） | ✅ **保留** - 移到 docs/deployment/ |
| `docs/deployment/DOCKER-DEPLOYMENT-STATUS.md` | docs/deployment/ | 部署状态报告 | ❌ **归档** - 临时报告 |
| `docs/deployment/DEPLOYMENT-TESTING-SUMMARY.md` | docs/deployment/ | 部署测试总结 | ❌ **归档** - 临时报告 |
| `docs/deployment/DEPLOYMENT-TEST-REPORT.md` | docs/deployment/ | 部署测试报告 | ❌ **归档** - 临时报告 |

**结论**: 
- 保留 DOCKER-DEPLOYMENT.md 在根目录（快速开始）
- 移动 DEPLOYMENT-GUIDE.md 到 docs/deployment/
- 归档所有临时部署报告

### 3. DOCUMENTATION 元文档（应归档）

| 文件 | 位置 | 内容 | 建议 |
|------|------|------|------|
| `DOCUMENTATION-INDEX.md` | 根目录 | 文档导航 | ✅ **保留** - 需更新内容 |
| `DOCUMENTATION-REFRESH-2025-10-20.md` | 根目录 | 第一轮刷新记录 | ❌ **归档** - 临时记录 |
| `DOCUMENTATION-ORGANIZATION-2025-10-20.md` | 根目录 | 第二轮整理记录 | ❌ **归档** - 临时记录 |

**结论**: 归档刷新记录，保留并更新索引文档

### 4. NEXT-STEP 文档（重复度：高）

| 文件 | 位置 | 内容 | 建议 |
|------|------|------|------|
| `NEXT-STEPS-QUICK-GUIDE.md` | 根目录 | 12周行动计划 | ✅ **保留** - 主要计划文档 |
| `NEXT-STEP-PROGRESS.md` | 根目录 | 每日进度更新 | ✅ **保留** - 动态追踪 |
| `docs/NEXT-STEP-PLAN.md` | docs/ | 详细计划 | ⚠️ **检查** - 可能与 QUICK-GUIDE 重复 |
| `docs/下一步行动计划.md` | docs/ | 中文计划（可能过时） | ⚠️ **检查** - 与英文版重复 |
| `docs/下一步计划速览.md` | docs/ | 中文速览（可能过时） | ⚠️ **检查** - 与 QUICK-GUIDE 重复 |

**结论**: 检查中文版本是否过时，整合为一个主计划文档

### 5. 过时的进度报告（应归档）

docs/ 目录中的临时报告：
- `DAY4-MORNING-SUMMARY.md`
- `WEEK1-DAY3-AFTERNOON-PROGRESS.md`
- `WEEK1-DAY3-FINAL-REPORT.md`
- `TASK-COMPLETION-REPORT.md`
- `P2-P3-FINAL-SUMMARY.md`
- `P3每周行动清单.md`
- `P3阶段甘特图.md`
- `P3阶段规划总览.md`

**结论**: 全部归档到 docs/archive/progress-reports/

---

## 🚀 执行计划

### 阶段一：分析和验证（10分钟）
1. ✅ 识别所有重复文档
2. ⏳ 比较重复文档内容差异
3. ⏳ 确定保留/合并/删除决策

### 阶段二：合并和移动（20分钟）
1. 合并 TEST 相关文档
2. 移动 DEPLOYMENT 文档到正确位置
3. 整合 NEXT-STEP 文档
4. 归档临时进度报告

### 阶段三：清理和更新（10分钟）
1. 删除归档后的原文件
2. 更新 DOCUMENTATION-INDEX.md
3. 更新 README.md 中的文档链接
4. 提交更改

---

## 📋 详细执行清单

### 1. 合并 TEST 文档

- [ ] 创建 `docs/testing/TESTING-FRAMEWORK-GUIDE.md`
  - 整合 TEST-FRAMEWORK-SUMMARY.md 内容
  - 整合 TESTING-QUICKSTART.md 快速开始部分
  - 添加清晰的章节结构
- [ ] 移动 TESTING-GUIDE.md 到 docs/testing/
- [ ] 移动 TEST-ENVIRONMENT-GUIDE.md 到 docs/deployment/
- [ ] 归档 TESTING-IMPROVEMENTS.md 到 docs/archive/

### 2. 整理 DEPLOYMENT 文档

- [ ] 移动 DEPLOYMENT-GUIDE.md 到 docs/deployment/
- [ ] 归档临时部署报告：
  - DOCKER-DEPLOYMENT-STATUS.md
  - DEPLOYMENT-TESTING-SUMMARY.md
  - DEPLOYMENT-TEST-REPORT.md
- [ ] 在 DOCKER-DEPLOYMENT.md 中添加指向详细指南的链接

### 3. 归档元文档

- [ ] 移动到 docs/archive/documentation-history/：
  - DOCUMENTATION-REFRESH-2025-10-20.md
  - DOCUMENTATION-ORGANIZATION-2025-10-20.md
  - 本文档 CLEANUP-PLAN-2025-10-20.md（执行后）

### 4. 整合 NEXT-STEP 文档

- [ ] 比较并合并：
  - NEXT-STEPS-QUICK-GUIDE.md（保留）
  - docs/NEXT-STEP-PLAN.md
  - docs/下一步行动计划.md
  - docs/下一步计划速览.md
- [ ] 删除重复和过时版本
- [ ] 更新 NEXT-STEP-PROGRESS.md 链接

### 5. 归档进度报告

创建 `docs/archive/progress-reports/2025-10/` 并移动：
- [ ] DAY4-MORNING-SUMMARY.md
- [ ] WEEK1-DAY3-AFTERNOON-PROGRESS.md
- [ ] WEEK1-DAY3-FINAL-REPORT.md
- [ ] TASK-COMPLETION-REPORT.md
- [ ] P2-P3-FINAL-SUMMARY.md
- [ ] P3每周行动清单.md
- [ ] P3阶段甘特图.md
- [ ] P3阶段规划总览.md
- [ ] PLAN-KANBAN.md（如果过时）

### 6. 更新索引和链接

- [ ] 更新 DOCUMENTATION-INDEX.md：
  - 更新文件路径
  - 删除已归档文档
  - 添加新合并的文档
- [ ] 更新 README.md 中的文档链接
- [ ] 更新 docs/README.md

### 7. 验证和提交

- [ ] 检查所有链接有效性
- [ ] 验证文档内容完整性
- [ ] Git 提交并推送

---

## 📊 预期成果

### 清理前（根目录）
```
c:\Users\andyz\Projects\evcs-mgr\
├── DOCKER-DEPLOYMENT.md
├── DOCUMENTATION-INDEX.md
├── DOCUMENTATION-REFRESH-2025-10-20.md     ❌ 临时
├── DOCUMENTATION-ORGANIZATION-2025-10-20.md ❌ 临时
├── NEXT-STEPS-QUICK-GUIDE.md
├── NEXT-STEP-PROGRESS.md
├── README-TENANT-ISOLATION.md
├── README.md
├── TEST-ENVIRONMENT-QUICKSTART.md
└── TEST-FRAMEWORK-SUMMARY.md               ❌ 应移动
```

### 清理后（根目录）
```
c:\Users\andyz\Projects\evcs-mgr\
├── DOCKER-DEPLOYMENT.md                    ✅ 快速开始
├── DOCUMENTATION-INDEX.md                  ✅ 文档导航
├── NEXT-STEPS-QUICK-GUIDE.md              ✅ 主计划
├── NEXT-STEP-PROGRESS.md                  ✅ 进度追踪
├── README-TENANT-ISOLATION.md             ✅ 技术说明
├── README.md                              ✅ 项目主页
└── TEST-ENVIRONMENT-QUICKSTART.md         ✅ 测试快速开始
```

### docs/ 结构优化
```
docs/
├── deployment/
│   ├── README.md
│   ├── DEPLOYMENT-GUIDE.md                ✅ 从根移入
│   ├── TEST-ENVIRONMENT-GUIDE.md          ✅ 从根移入
│   └── docker-troubleshooting.md
├── testing/
│   ├── README.md
│   ├── TESTING-FRAMEWORK-GUIDE.md         ✅ 新合并
│   ├── TESTING-GUIDE.md                   ✅ 已存在
│   ├── TEST-COVERAGE-REPORT.md
│   └── TEST-FIX-GUIDE.md
├── development/
│   ├── README.md
│   └── IDE-FIX-GUIDE.md
├── archive/
│   ├── documentation-history/
│   │   ├── DOCUMENTATION-REFRESH-2025-10-20.md
│   │   ├── DOCUMENTATION-ORGANIZATION-2025-10-20.md
│   │   └── CLEANUP-PLAN-2025-10-20.md
│   └── progress-reports/
│       └── 2025-10/
│           ├── DAY4-MORNING-SUMMARY.md
│           ├── WEEK1-DAY3-*.md
│           ├── P2-P3-FINAL-SUMMARY.md
│           └── P3阶段*.md
└── [其他核心文档]
```

---

## ⚠️ 注意事项

1. **备份**: 执行前确保所有更改已提交到 Git
2. **链接**: 更新所有文档间的引用链接
3. **验证**: 归档后验证重要内容未丢失
4. **团队**: 通知团队成员文档结构变更

---

**执行状态**: 🟡 计划制定完成，等待执行  
**预计耗时**: 40分钟  
**下一步**: 开始执行阶段一

