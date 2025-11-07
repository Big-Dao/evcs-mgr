# EVCS项目 - AI编程助手规范索引

> **版本**: v1.0 | **更新日期**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 活跃

## 🎯 统一规范说明

为了确保所有AI编程助手（Claude、GitHub Copilot、CodeX等）都能获得一致、完整的项目规范，我们已经建立了统一的规范体系。

## 📋 核心规范文档

### 🌟 [PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md)
**🔥 所有AI助手的必读文档！**

这是项目的**完整编程规范总览**，包含了所有AI编程助手生成高质量代码所需的全部信息：

- ✅ **强制架构规范**: 微服务分层、DDD架构、多租户隔离
- ✅ **完整代码模板**: Controller、Service、Entity标准模板
- ✅ **必须注解清单**: @Service、@Transactional、@Cacheable等
- ✅ **严格禁止模式**: 跨服务访问、硬编码、空catch块等
- ✅ **质量检查清单**: 安全、性能、测试要求
- ✅ **单元测试模板**: 完整的测试代码示例
- ✅ **Docker配置标准**: 统一的容器化规范

## 🤖 各AI助手配置

### 1. Claude Code
- **配置文件**: [../../.claude/project-instructions.md](../../.claude/project-instructions.md)
- **自动读取**: Claude Code会自动读取.claude/目录下的配置文件
- **引用规范**: 配置文件指向PROJECT-CODING-STANDARDS.md

### 2. GitHub Copilot
- **配置文件**: [../../.github/copilot-instructions.md](../../.github/copilot-instructions.md)
- **自动读取**: Copilot会自动读取.github/目录下的copilot-instructions.md
- **引用规范**: 配置文件指向PROJECT-CODING-STANDARDS.md

### 3. OpenAI CodeX/ChatGPT
- **配置目录**: [../../.codex/](../../.codex/)
- **使用方式**: 通过API调用时包含项目上下文
- **核心文件**:
  - [../../.codex/project-context.md](../../.codex/project-context.md) - 项目上下文
  - [../../.codex/QUICK-REFERENCE.md](../../.codex/QUICK-REFERENCE.md) - 快速参考
  - [../../.codex/examples/station-service-example.md](../../.codex/examples/station-service-example.md) - 完整示例

## 📚 相关文档

### 详细规范文档
- [AI助手详细指南](AI-ASSISTANT-GUIDELINES.md)
- [代码质量检查清单](CODE-QUALITY-CHECKLIST.md)
- [CodeX集成指南](CODEX-INTEGRATION-GUIDE.md)

### 技术文档
- [项目结构说明](../operations/PROJECT-STRUCTURE.md)
- [部署指南](../deployment/DEPLOYMENT-GUIDE.md)
- [文档导航中心](../README.md)

## 🔧 使用方法

### Claude Code用户
1. 确保项目根目录有.claude/目录
2. Claude Code会自动读取项目规范
3. 生成的代码将遵循统一标准

### GitHub Copilot用户
1. 确保项目根目录有.github/copilot-instructions.md
2. Copilot会自动读取并应用规范
3. 在IDE中获得的代码建议符合项目标准

### ChatGPT/CodeX用户
```python
# 方法1：直接引用完整规范
context = open("PROJECT-CODING-STANDARDS.md").read()

# 方法2：使用CodeX配置
with open(".codex/project-context.md") as f:
    context = f.read()

# 发送给AI
response = openai.Completion.create(
    engine="code-davinci-002",
    prompt=context + "\n\n请为[需求]生成符合EVCS项目规范的代码"
)
```

## 🎯 关键特性

### 统一性保证
- 所有AI助手都引用同一个核心规范文档
- 确保不同助手生成的代码风格一致
- 避免规范冲突和不一致问题

### 完整性覆盖
- 架构规范、代码模板、质量要求全覆盖
- 从Controller到Entity的完整分层示例
- 异常处理、测试、部署等全流程规范

### 易维护性
- 核心规范集中在PROJECT-CODING-STANDARDS.md
- 更新规范只需修改一个文件
- 所有AI助手自动获得最新规范

## 🎉 质量保证

通过这套统一的规范体系，所有AI编程助手都能：

1. **遵循微服务架构**: 严格分层，杜绝跨服务访问
2. **保证代码质量**: 包含完整注解、异常处理、日志记录
3. **符合安全标准**: JWT认证、参数验证、租户隔离
4. **优化性能**: 合理缓存、避免N+1查询
5. **提供测试覆盖**: 完整的单元测试模板和示例

## 📞 支持和反馈

如果在使用过程中发现：
- 规范不清晰或缺失
- AI助手生成的代码不符合规范
- 需要新增特定场景的规范

请：
1. 检查PROJECT-CODING-STANDARDS.md是否包含相应规范
2. 更新核心规范文档
3. 验证所有AI助手配置文件正确引用

通过这套体系，确保所有AI编程助手都能为EVCS项目生成高质量、一致性的代码！