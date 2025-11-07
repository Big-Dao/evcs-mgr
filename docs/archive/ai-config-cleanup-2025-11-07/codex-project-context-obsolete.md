# EVCS充电站管理系统 - CodeX项目上下文

## 🤖 OpenAI CodeX专用配置

### 📋 项目上下文
请首先阅读共享的项目上下文：[SHARED-PROJECT-CONTEXT.md](../.ai-assistant-config/SHARED-PROJECT-CONTEXT.md)

该文档包含完整的项目架构、微服务模块、技术栈和编码标准。

### 🔧 CodeX使用方式

#### 1. API调用配置
```python
import openai

def generate_evcs_code(requirement):
    # 读取共享项目上下文
    with open("../.ai-assistant-config/SHARED-PROJECT-CONTEXT.md", "r") as f:
        project_context = f.read()

    prompt = f"""
    你正在为EVCS充电站管理系统编写代码。请严格遵循以下规范：

    {project_context}

    现在请为以下需求生成代码：{requirement}
    """

    response = openai.Completion.create(
        engine="code-davinci-002",
        prompt=prompt,
        temperature=0.1,  # 低温度确保代码一致性
        max_tokens=2000,
        stop=["\n\n"]
    )

    return response.choices[0].text
```

#### 2. 集成到IDE
```json
// VS Code settings.json
{
    "openai.apiKey": "your-api-key",
    "openai.model": "code-davinci-002",
    "openai.temperature": 0.1,
    "openai.maxTokens": 2000,
    "openai.contextFile": "../.ai-assistant-config/SHARED-PROJECT-CONTEXT.md"
}
```

#### 3. 项目启动
```bash
# 设置API密钥
export OPENAI_API_KEY="your-api-key"

# 使用CodeX生成代码
python scripts/generate-code.py "创建充电站管理服务"
```

### 🎯 CodeX特有能力

#### 强大的代码生成
- 支持复杂业务逻辑生成
- 多语言代码生成能力
- 智能代码补全和建议

#### 上下文理解
- 理解大型项目结构
- 跨文件的依赖关系分析
- 架构模式识别

#### 代码转换
- 代码重构建议
- 性能优化建议
- 语言转换支持

### 📝 CodeX使用技巧

#### 1. 结构化提示
```python
prompt = """
你正在为EVCS充电站管理系统编写Java代码。

项目信息：
- Spring Boot 3.2.10 + Java 21
- 微服务架构，多租户隔离
- 使用MyBatis Plus + PostgreSQL
- JWT认证 + RBAC权限控制

请为以下需求生成完整的Service类代码：
需求：[具体需求描述]

要求：
1. 包含完整的异常处理
2. 使用@Transactional注解
3. 包含日志记录
4. 遵循项目编码规范
"""
```

#### 2. 分步骤生成
```python
# 第一步：生成接口定义
generate_evcs_code("生成充电站服务的接口定义")

# 第二步：生成实现类
generate_evcs_code("生成充电站服务的实现类")

# 第三步：生成单元测试
generate_evcs_code("生成充电站服务的单元测试")
```

#### 3. 代码审查模式
```python
def review_code(code_snippet):
    prompt = f"""
    请审查以下代码，检查是否符合EVCS项目规范：

    项目规范：
    {project_context}

    代码：
    {code_snippet}

    请检查：
    1. 架构合规性
    2. 安全性
    3. 性能问题
    4. 代码质量
    """
    return openai.Completion.create(engine="code-davinci-002", prompt=prompt)
```

### 🚀 CodeX高级用法

#### 1. 批量代码生成
```python
services = [
    "充电站管理服务",
    "订单管理服务",
    "支付服务",
    "协议处理服务"
]

for service in services:
    code = generate_evcs_code(f"创建{service}的完整实现")
    save_code_to_file(code, f"{service}.java")
```

#### 2. 数据库代码生成
```python
def generate_database_code(table_definition):
    prompt = f"""
    根据以下表定义生成MyBatis Plus相关的代码：

    表定义：{table_definition}

    请生成：
    1. Entity实体类
    2. Mapper接口
    3. Service服务类
    4. Controller控制器类
    """
    return generate_evcs_code(prompt)
```

#### 3. 测试代码生成
```python
def generate_test_code(service_class):
    prompt = f"""
    为以下服务类生成完整的单元测试：

    服务类：{service_class}

    要求：
    1. 使用JUnit 5 + Mockito
    2. 覆盖所有public方法
    3. 包含边界条件测试
    4. 包含异常情况测试
    """
    return generate_evcs_code(prompt)
```

### 🔧 CodeX集成工具

#### 1. CLI工具
```bash
#!/bin/bash
# scripts/codex-cli.sh

requirement="$1"
output_file="$2"

python scripts/generate-code.py "$requirement" > "$output_file"
echo "代码已生成到: $output_file"
```

#### 2. IDE插件集成
```javascript
// CodeX插件配置
const codeXConfig = {
    apiKey: process.env.OPENAI_API_KEY,
    contextFile: '.ai-assistant-config/SHARED-PROJECT-CONTEXT.md',
    temperature: 0.1,
    maxTokens: 2000,
    language: 'java'
};
```

#### 3. CI/CD集成
```yaml
# .github/workflows/codex-review.yml
name: CodeX Code Review

on: [pull_request]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Run CodeX Review
      run: |
        python scripts/codex-review.py ${{ github.event.pull_request.diff }}
```

### 🚨 CodeX限制

#### 1. API限制
- 请求频率限制
- Token数量限制
- 成本考虑

#### 2. 上下文窗口
- 单次请求的上下文有限
- 大型项目需要分批处理
- 建议使用模块化方法

#### 3. 实时信息
- 无法访问运行时环境
- 不能执行实际代码
- 需要本地验证

### 📊 质量保证

#### 1. 代码验证
```python
def validate_generated_code(code):
    # 编译检查
    if not compile_java(code):
        return False

    # 代码规范检查
    if not check_code_standards(code):
        return False

    # 安全检查
    if not check_security(code):
        return False

    return True
```

#### 2. 测试生成
```python
def generate_comprehensive_tests(service_code):
    prompt = f"""
    为以下服务代码生成完整的测试套件：

    {service_code}

    包含：
    1. 单元测试（覆盖率>80%）
    2. 集成测试
    3. 性能测试
    4. 安全测试
    """
    return generate_evcs_code(prompt)
```

### 📚 相关资源

#### OpenAI文档
- [OpenAI API文档](https://platform.openai.com/docs)
- [CodeX使用指南](https://platform.openai.com/docs/guides/code)

#### 项目文档
- [统一部署指南](../../DEPLOYMENT-GUIDE.md)
- [API设计规范](../../docs/02-development/API-DESIGN-STANDARDS.md)
- [数据库设计规范](../../docs/02-development/DATABASE-DESIGN-STANDARDS.md)

#### 开发工具
- [统一测试指南](../../docs/testing/UNIFIED-TESTING-GUIDE.md)
- [代码质量检查清单](../../docs/02-development/CODE-QUALITY-CHECKLIST.md)
- [Docker配置指南](../../DOCKER-CONFIGURATION-GUIDE.md)

---

**通过遵循本配置和共享项目上下文，OpenAI CodeX可以为EVCS项目生成高质量、符合架构规范的代码。**