# EVCS项目CodeX使用指南

## 🤖 OpenAI CodeX配置

### 📋 项目上下文
请先读取仓库统一入口：[AGENTS.md](../../AGENTS.md)

其次读取统一的项目配置：[AI-ASSISTANT-UNIFIED-CONFIG.md](../../docs/development/AI-ASSISTANT-UNIFIED-CONFIG.md)

该文档包含完整的项目架构、微服务模块、技术栈和编码标准。

### 💡 CodeX使用建议

1. **API调用**: 将统一配置内容作为prompt的一部分
2. **代码生成**: 严格遵循 `docs/overview/PROJECT-CODING-STANDARDS.md` 中的规范
3. **批量处理**: 利用CodeX的强大生成能力处理复杂功能
4. **质量检查**: 人工审查生成的代码质量和安全性

### Python集成示例
```python
def generate_evcs_code(requirement):
    # 读取统一项目配置
    with open("AI-ASSISTANT-UNIFIED-CONFIG.md", "r") as f:
        project_config = f.read()

    prompt = f"""
    请基于以下项目配置生成代码：

    {project_config}

    需求：{requirement}
    """

    # 调用OpenAI API
    return generate_code(prompt)
```

---

**使用 CodeX 时请以 `AGENTS.md` 为索引、`AI-ASSISTANT-UNIFIED-CONFIG.md` 为统一配置，避免在本文件重复规范。**
