# IDE 错误修复指南

## 🎯 已完成的自动修复步骤

✅ **步骤 1**: 清理 Gradle 缓存  
✅ **步骤 2**: 清理 bin 目录  
✅ **步骤 3**: 清理 Eclipse 配置文件  
✅ **步骤 4**: 重新构建项目（BUILD SUCCESSFUL）  
✅ **步骤 5**: 创建 VS Code 配置文件  
   - `.vscode/settings.json` - Java 编译器和 Gradle 配置
   - `.vscode/launch.json` - 调试配置
   - `.vscode/tasks.json` - Gradle 任务
   - `.vscode/extensions.json` - 推荐扩展

## 📋 需要手动完成的步骤

### 第一步：清理 Java Language Server 工作空间

1. 在 VS Code 中按 **`Ctrl + Shift + P`**
2. 输入并选择：**`Java: Clean Java Language Server Workspace`**
3. 在弹出的对话框中选择：**`Reload and delete`**
4. 等待 VS Code 自动重新加载

### 第二步：等待项目同步

重新加载后，请耐心等待：
- 在 VS Code 右下角会显示同步进度（如 "Building workspace..."）
- 这个过程可能需要 **3-5 分钟**
- 完成后会显示类似 "Java: Ready" 的提示

### 第三步：验证修复结果

同步完成后，检查问题面板（`Ctrl + Shift + M`）：
- 如果编译错误大幅减少或消失 → ✅ 修复成功！
- 如果仍有大量错误 → 继续下面的步骤

### 第四步：强制完整编译（如果仍有错误）

1. 按 **`Ctrl + Shift + P`**
2. 输入并选择：**`Java: Force Java Compilation - Full`**
3. 等待编译完成（右下角会显示进度）

### 第五步：重启 VS Code（最后手段）

如果上述步骤都无效：
1. 完全关闭 VS Code
2. 重新打开项目文件夹
3. 等待项目自动同步

## 🔍 验证修复成功

打开任意 Java 文件（例如 `evcs-station/src/main/java/com/evcs/station/StationServiceApplication.java`），检查：

- ✅ 没有红色波浪线（编译错误）
- ✅ 可以看到代码提示（自动完成）
- ✅ 可以跳转到定义（`F12`）
- ✅ 可以看到方法签名提示

## 🛠️ 配置说明

### settings.json 关键配置

- **Java 版本**: Java 21 (Temurin)
- **JDK 路径**: `C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot`
- **构建工具**: Gradle（自动检测）
- **自动构建**: 已启用
- **更新配置**: 自动模式

### 推荐的 VS Code 扩展

已在 `.vscode/extensions.json` 中配置，VS Code 会自动提示安装：

1. **Java Extension Pack** (vscjava.vscode-java-pack)
2. **Spring Boot Extension Pack** (vmware.vscode-spring-boot)
3. **Gradle for Java** (vscjava.vscode-gradle)
4. **Docker** (ms-azuretools.vscode-docker)
5. **GitLens** (eamodio.gitlens)

## ⚡ 快速调试

现在可以直接在 VS Code 中调试各个服务：

1. 按 **`F5`** 或点击左侧调试图标
2. 选择要调试的服务（如 "Debug evcs-station"）
3. 设置断点并开始调试

## 📝 可用的 Gradle 任务

按 **`Ctrl + Shift + P`**，输入 `Tasks: Run Task`，可以看到：

- `Gradle: Build (skip tests)` - 快速构建
- `Gradle: Build with tests` - 完整构建
- `Gradle: Test` - 运行测试
- `Gradle: Test with Coverage` - 测试覆盖率
- `Run evcs-station` - 运行站点服务
- `Run evcs-order` - 运行订单服务
- `Docker: Start local env` - 启动本地环境

## 🐛 常见问题排查

### 问题 1: "Cannot resolve symbol"

**原因**: Java Language Server 未完成同步  
**解决**: 等待同步完成（右下角进度条）

### 问题 2: "The declared package does not match"

**原因**: 这是 IDE 误报，Gradle 构建是成功的  
**解决**: 执行 `Java: Clean Java Language Server Workspace`

### 问题 3: 导入未生效

**原因**: 缓存问题  
**解决**: 
```powershell
./gradlew clean build
```
然后在 VS Code 中 `Java: Force Java Compilation - Full`

### 问题 4: 内存不足

**解决**: 已在 `settings.json` 中配置：
- JVM 参数：`-Xmx2G`
- Language Server：`-Xmx2G`

如果仍不足，可以增加到 `-Xmx4G`

## 📞 需要帮助？

如果问题仍未解决，请检查：

1. **Gradle 构建状态**:
   ```powershell
   ./gradlew build -x test
   ```
   应该显示 `BUILD SUCCESSFUL`

2. **Java 版本**:
   ```powershell
   java -version
   ```
   应该显示 `openjdk version "21.0.8"`

3. **VS Code 输出**:
   - 打开输出面板：`Ctrl + Shift + U`
   - 选择 "Java" 或 "Gradle for Java"
   - 查看错误日志

## ✅ 修复完成检查清单

- [ ] 执行了 `Java: Clean Java Language Server Workspace`
- [ ] 等待项目同步完成（3-5 分钟）
- [ ] 编译错误大幅减少或消失
- [ ] 可以正常使用代码提示和跳转
- [ ] 可以使用 F5 调试服务
- [ ] Gradle 构建成功：`BUILD SUCCESSFUL`

---

**最后更新**: 2025-10-19  
**状态**: ✅ 配置文件已就绪，等待手动触发 Java Language Server 清理

