# GitHub 配置目录

> **最后更新**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 已发布

本目录包含 EVCS Manager 项目的 GitHub 专用配置文件。

## 📄 文件说明

### copilot-instructions.md
GitHub Copilot AI 助手的自定义指令。这些指令帮助 Copilot 理解：
- 项目架构（多租户系统）
- 开发模式和最佳实践
- 构建和测试命令
- 代码质量标准
- 常见陷阱和注意事项

**最后更新**: 2025-10-20

### MAINTENANCE.md
`.github` 目录的维护指南，包括：
- 各文件的作用和维护优先级
- 维护时机和清单
- 维护历史记录

**最后更新**: 2025-10-20

### pull-request-template-deployment.md
部署相关 Pull Request 的模板。

### instructions/
模块级和路径级的 Copilot 指令：
- `common.instructions.md` - evcs-common 模块开发规范
- `station.instructions.md` - evcs-station 模块开发规范
- `test.instructions.md` - 测试代码编写规范
- `documentation.instructions.md` - **文档管理规范**（⭐ 新增）

**最后更新**: 2025-10-20

### workflows/
GitHub Actions 工作流定义，用于 CI/CD 流水线：
- `build.yml` - 自动构建流程
- `test-environment.yml` - 测试环境部署流程

---

## 🎯 路径级指令说明

路径级指令可以为特定代码路径提供更细粒度的规范控制。

### 指令格式

```markdown
---
applyTo: "evcs-station/**/*.java"
---

# 站点模块指令
- 关注多租户数据隔离
- 始终验证站点所有权
```

### 已配置的指令

```
.github/instructions/
├── common.instructions.md        # evcs-common 模块规范
├── station.instructions.md       # evcs-station 模块规范
├── test.instructions.md          # 测试代码规范
└── documentation.instructions.md # 文档管理规范 ⭐ 新增
```

### 指令作用域

| 指令文件 | 应用路径 | 主要约束 |
|---------|---------|---------|
| `common.instructions.md` | `evcs-common/**/*.java` | 向后兼容、多租户框架、零业务逻辑 |
| `station.instructions.md` | `evcs-station/**/*.java` | 站点层级、多租户隔离、实时更新 |
| `test.instructions.md` | `**/src/test/**/*.java` | 测试框架、AAA 模式、租户上下文 |
| `documentation.instructions.md` | `**/*.md` | 文档结构、命名规范、生命周期管理 |

### 添加新指令

如需为新模块添加指令：
1. 在 `instructions/` 目录下创建 `[module].instructions.md`
2. 使用 YAML frontmatter 指定 `applyTo` 路径
3. 编写该模块的特定开发规范

---

## 📚 参考资源

### GitHub 官方文档
- [GitHub Copilot 自定义指令文档](https://docs.github.com/zh/copilot/customizing-copilot/adding-custom-instructions-for-github-copilot)
- [GitHub Copilot 最佳实践](https://docs.github.com/zh/copilot/using-github-copilot/best-practices-for-using-github-copilot)
- [GitHub Actions 文档](https://docs.github.com/zh/actions)

### 项目相关文档
- [../DOCUMENTATION-INDEX.md](../DOCUMENTATION-INDEX.md) - 项目文档导航
- [MAINTENANCE.md](MAINTENANCE.md) - 维护指南
- [../docs/DEVELOPER-GUIDE.md](../docs/DEVELOPER-GUIDE.md) - 开发者指南

---

**目录创建**: 2025-10-14  
**最后更新**: 2025-10-20

