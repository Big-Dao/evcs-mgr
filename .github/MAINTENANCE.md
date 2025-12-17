# .github 目录维护指南

> **最后更新**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 已发布

---

## 📂 目录结构和作用

```
.github/
├── copilot-instructions.md           ⭐ GitHub Copilot 项目级指令
├── pull-request-template-deployment.md  PR 模板
│
├── instructions/                      🎯 模块级 Copilot 指令
│   ├── common.instructions.md         - evcs-common 模块规范
│   ├── station.instructions.md        - evcs-station 模块规范
│   ├── test.instructions.md           - 测试编写规范
│   └── documentation.instructions.md  - 文档管理规范
│
└── workflows/                         ⚙️ GitHub Actions CI/CD
    ├── build.yml                      - 构建流水线
    └── test-environment.yml           - 测试环境部署
```

---

## 🎯 文件维护优先级

### P0 - 关键文件（必须保持最新）

#### 1. copilot-instructions.md ⭐
**作用**: GitHub Copilot AI 助手的核心指令文档
- 定义项目架构、技术栈、开发规范
- AI 代码生成的主要参考依据
- 影响所有使用 Copilot 的开发工作

**维护时机**:
- ✅ 技术栈升级时（如 Spring Boot 版本）
- ✅ 架构调整时（如新增模块）
- ✅ 重要规范变更时（如测试规范、命名规范）
- ✅ 每季度检查一次，确保与实际一致

**当前状态**: ✅ 最新 (2025-10-14)

#### 2. instructions/*.instructions.md ⭐
**作用**: 模块级和路径级的细粒度指令
- 为特定代码路径提供专门的规范
- 补充 copilot-instructions.md 的通用规范
- 支持更精确的 AI 辅助编码

**维护时机**:
- ✅ 模块规范变更时
- ✅ 发现 AI 生成代码不符合规范时
- ✅ 新增重要开发模式时

**当前状态**: ✅ 看起来是最新的

**文件说明**:
- `common.instructions.md` - evcs-common 模块（多租户框架、工具类）
- `station.instructions.md` - evcs-station 模块（站点桩管理）
- `test.instructions.md` - 测试代码规范（AAA 模式、命名约定）

#### 3. workflows/*.yml ⚙️
**作用**: GitHub Actions 自动化流水线
- 自动构建和测试
- CI/CD 流程定义

**维护时机**:
- ✅ 构建命令变更时
- ✅ 测试环境配置变更时
- ✅ 部署流程调整时

**当前状态**: ✅ 功能正常（待验证）

---

### P1 - 重要文件（定期更新）

#### 4. pull-request-template-deployment.md
**作用**: 部署相关 PR 的模板
- 标准化 PR 描述格式
- 确保必要信息完整

**维护时机**:
- ✅ PR 流程变更时
- ✅ 如果不再使用，可以删除

**当前状态**: ❓ 待确认是否在使用

---

## 🗑️ 已清理的内容

### java-upgrade/ 目录 ✅ 已删除
- **原内容**: Java 升级过程的临时日志
- **删除原因**: 升级已完成，日志已过时
- **删除时间**: 2025-10-20
- **备注**: 如需查看历史，可在 Git 历史中找回

---

## 📝 维护清单

### 每次重大变更后
- [ ] 更新 copilot-instructions.md 反映架构变更
- [ ] 检查 instructions/*.md 是否需要调整
- [ ] 更新 workflows/*.yml 适应新的构建流程

### 每季度检查
- [ ] copilot-instructions.md 是否与实际代码规范一致
- [ ] instructions/*.md 是否仍然准确
- [ ] workflows/*.yml 是否正常工作
- [ ] 清理不再使用的文件

### 新增模块时
- [ ] 考虑为新模块创建 instructions/[module].instructions.md
- [ ] 在 copilot-instructions.md 中添加模块说明

---

## 🔗 相关文档

### 项目文档
- [../README.md](../README.md) - 项目主 README
- [../docs/DOCUMENTATION-INDEX.md](../docs/DOCUMENTATION-INDEX.md) - 文档导航中心
- [../docs/development/DEVELOPER-GUIDE.md](../docs/development/DEVELOPER-GUIDE.md) - 开发者指南

### GitHub 官方文档
- [GitHub Copilot Custom Instructions](https://docs.github.com/en/copilot/customizing-copilot/adding-custom-instructions-for-github-copilot)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Creating Pull Request Templates](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/creating-a-pull-request-template-for-your-repository)

---

## ⚡ 快速参考

### 如何测试 Copilot 指令效果
1. 在 VS Code 中打开项目
2. 使用 Copilot Chat 询问项目相关问题
3. 检查 Copilot 的回答是否符合项目规范
4. 如不符合，更新相应的 instructions 文件

### 如何添加新的路径级指令
1. 在 `instructions/` 下创建新文件
2. 使用 YAML frontmatter 指定 `applyTo` 路径
3. 编写该路径下的特定规范
4. 提交并测试效果

示例:
```markdown
---
applyTo: "evcs-payment/**/*.java"
---

# Payment Module Instructions
- Always validate payment amounts
- Use BigDecimal for currency
```

---

## 📊 维护历史

| 日期 | 操作 | 说明 |
|------|------|------|
| 2025-10-20 | 清理 | 删除 java-upgrade/ 临时目录 |
| 2025-10-20 | 更新 | 更新 README.md 日期 |
| 2025-10-20 | 新建 | 创建本维护指南 |
| 2025-10-14 | 更新 | 更新 copilot-instructions.md |

---

**维护负责**: 项目开发团队  
**审查周期**: 每季度  
**最后审查**: 2025-10-20

