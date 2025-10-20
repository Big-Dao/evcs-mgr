# 开发文档

本目录包含 EVCS Manager 项目的开发工具和问题修复相关文档。

## 📄 文档列表

### 开发工具
- **IDE-FIX-GUIDE.md** - IDE 错误修复指南
  - VS Code 配置
  - Gradle 集成
  - Java 21 环境配置
  - 常见 IDE 问题解决方案

## 🔗 相关文档

开发指南和规范请参考：
- [DEVELOPER-GUIDE.md](../DEVELOPER-GUIDE.md) - 开发者指南 ⭐
- [../../.github/copilot-instructions.md](../../.github/copilot-instructions.md) - GitHub Copilot 使用指南
- [COPILOT-INSTRUCTIONS-SETUP.md](../COPILOT-INSTRUCTIONS-SETUP.md) - Copilot 配置说明

### 模块开发规范
- [../../.github/instructions/common.instructions.md](../../.github/instructions/common.instructions.md) - evcs-common 模块规范
- [../../.github/instructions/station.instructions.md](../../.github/instructions/station.instructions.md) - evcs-station 模块规范
- [../../.github/instructions/test.instructions.md](../../.github/instructions/test.instructions.md) - 测试编写规范

## 🛠️ 快速链接

### 配置开发环境
1. 安装 Java 21
2. 安装 VS Code + 推荐扩展
3. 参考 [IDE-FIX-GUIDE.md](IDE-FIX-GUIDE.md) 配置 VS Code
4. 运行 `./gradlew build` 验证环境

### 常见问题
- **3908 个编译错误**: 参考 IDE-FIX-GUIDE.md 的自动修复步骤
- **Gradle 构建失败**: 清理缓存后重新构建
- **代码提示不工作**: 检查 Java 语言服务器配置

---

**目录创建**: 2025-10-20  
**文档数量**: 1 个

