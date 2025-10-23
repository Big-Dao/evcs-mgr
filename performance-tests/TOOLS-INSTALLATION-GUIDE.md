# 性能测试工具安装指南

> **目标**: 安装 Week 2 性能优化所需的工具  
> **平台**: Windows 10/11  
> **预计时间**: 30-45 分钟

---

## 📋 工具清单

| 工具 | 版本 | 用途 | 必须？ |
|------|------|------|--------|
| Apache JMeter | 5.6.3+ | 性能压测 | ✅ 必须 |
| JDK Mission Control | 9.0+ | JFR 分析 | ✅ 必须 |
| VisualVM | 2.1.8+ | JVM 监控 | 🟡 可选 |
| Arthas | 3.7+ | 实时诊断 | 🟡 可选 |

---

## 1. 安装 Apache JMeter

### 方式 1: Scoop 包管理器（推荐）

```powershell
# 1.1 如果未安装 Scoop，先安装
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# 1.2 安装 JMeter
scoop bucket add java
scoop install jmeter

# 1.3 验证安装
jmeter --version
```

### 方式 2: 手动安装

```powershell
# 2.1 下载 JMeter
# 访问: https://jmeter.apache.org/download_jmeter.cgi
# 下载: apache-jmeter-5.6.3.zip

# 2.2 解压到目录
Expand-Archive -Path "C:\Downloads\apache-jmeter-5.6.3.zip" -DestinationPath "C:\Tools"

# 2.3 添加到环境变量
$env:PATH += ";C:\Tools\apache-jmeter-5.6.3\bin"
[Environment]::SetEnvironmentVariable("PATH", $env:PATH, "User")

# 2.4 验证安装
cd C:\Tools\apache-jmeter-5.6.3\bin
.\jmeter.bat --version
```

**预期输出**:
```
    _    ____   _    ____ _   _ _____       _ __  __ _____ _____ _____ ____
   / \  |  _ \ / \  / ___| | | | ____|     | |  \/  | ____|_   _| ____|  _ \
  / _ \ | |_) / _ \| |   | |_| |  _|    _  | | |\/| |  _|   | | |  _| | |_) |
 / ___ \|  __/ ___ \ |___|  _  | |___  | |_| | |  | | |___  | | | |___|  _ <
/_/   \_\_| /_/   \_\____|_| |_|_____|  \___/|_|  |_|_____| |_| |_____|_| \_\

Copyright (c) 1999-2024 The Apache Software Foundation

Version 5.6.3
```

---

## 2. 安装 JDK Mission Control

### 方式 1: 使用已有 JDK 21

```powershell
# 2.1 检查 JDK 位置
where java

# 2.2 JDK Mission Control 通常在 JDK 目录的 bin 下
# 例如: C:\Program Files\Eclipse Adoptium\jdk-21.0.8.7-hotspot\bin\jmc.exe

# 2.3 创建快捷方式（可选）
$WshShell = New-Object -comObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut("$Home\Desktop\JMC.lnk")
$Shortcut.TargetPath = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.7-hotspot\bin\jmc.exe"
$Shortcut.Save()
```

### 方式 2: 独立安装

```powershell
# 2.1 下载 JDK Mission Control
# 访问: https://www.oracle.com/java/technologies/javase/products-jmc8-downloads.html
# 下载: jmc-9.0.0_windows-x64.zip

# 2.2 解压到目录
Expand-Archive -Path "C:\Downloads\jmc-9.0.0_windows-x64.zip" -DestinationPath "C:\Tools"

# 2.3 运行 JMC
cd C:\Tools\jmc-9.0.0_windows-x64\JDK Mission Control
.\jmc.exe
```

---

## 3. 安装 VisualVM（可选）

### 使用 Scoop 安装

```powershell
# 3.1 安装 VisualVM
scoop install visualvm

# 3.2 验证安装
visualvm --version
```

### 手动安装

```powershell
# 3.1 下载 VisualVM
# 访问: https://visualvm.github.io/download.html
# 下载: visualvm_218.zip

# 3.2 解压并运行
Expand-Archive -Path "C:\Downloads\visualvm_218.zip" -DestinationPath "C:\Tools"
cd C:\Tools\visualvm_218\bin
.\visualvm.exe
```

---

## 4. 安装 Arthas（可选）

### 使用脚本安装

```powershell
# 4.1 下载 Arthas
Invoke-WebRequest -Uri "https://arthas.aliyun.com/arthas-boot.jar" -OutFile "$Home\arthas-boot.jar"

# 4.2 运行 Arthas（连接到 Java 进程）
# 首先找到 Java 进程 PID
jps -l

# 然后连接
java -jar $Home\arthas-boot.jar <PID>
```

---

## 5. 验证安装

### 验证脚本

```powershell
# 5.1 创建验证脚本
@"
Write-Host "=== 性能测试工具安装验证 ===" -ForegroundColor Cyan
Write-Host ""

# 检查 JMeter
Write-Host "检查 JMeter..." -ForegroundColor Yellow
try {
    `$jmeterVersion = jmeter --version 2>&1 | Select-String "Version"
    if (`$jmeterVersion) {
        Write-Host "✅ JMeter 已安装: `$jmeterVersion" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ JMeter 未安装或未配置到 PATH" -ForegroundColor Red
}

# 检查 Java
Write-Host "检查 Java..." -ForegroundColor Yellow
try {
    `$javaVersion = java -version 2>&1 | Select-String "version"
    if (`$javaVersion) {
        Write-Host "✅ Java 已安装: `$javaVersion" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Java 未安装" -ForegroundColor Red
}

# 检查 JMC
Write-Host "检查 JDK Mission Control..." -ForegroundColor Yellow
`$jmcPath = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.7-hotspot\bin\jmc.exe"
if (Test-Path `$jmcPath) {
    Write-Host "✅ JMC 已安装: `$jmcPath" -ForegroundColor Green
} else {
    Write-Host "❌ JMC 未找到，请手动检查 JDK 安装目录" -ForegroundColor Red
}

# 检查 VisualVM（可选）
Write-Host "检查 VisualVM..." -ForegroundColor Yellow
try {
    `$visualvmVersion = visualvm --version 2>&1
    if (`$visualvmVersion) {
        Write-Host "✅ VisualVM 已安装" -ForegroundColor Green
    }
} catch {
    Write-Host "🟡 VisualVM 未安装（可选工具）" -ForegroundColor Yellow
}

# 检查 Arthas
Write-Host "检查 Arthas..." -ForegroundColor Yellow
if (Test-Path "`$Home\arthas-boot.jar") {
    Write-Host "✅ Arthas 已下载: `$Home\arthas-boot.jar" -ForegroundColor Green
} else {
    Write-Host "🟡 Arthas 未下载（可选工具）" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 验证完成 ===" -ForegroundColor Cyan
"@ | Out-File -FilePath ".\verify-tools.ps1" -Encoding UTF8

# 5.2 运行验证
.\verify-tools.ps1
```

---

## 6. 常见问题

### Q1: JMeter 无法启动，提示找不到 Java

**解决方案**:
```powershell
# 确保 JAVA_HOME 环境变量已设置
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.7-hotspot", "User")
```

### Q2: JMeter 报错 "Could not find or load main class"

**解决方案**:
```powershell
# 检查 JMeter 目录结构是否完整
Get-ChildItem "C:\Tools\apache-jmeter-5.6.3\lib" -Recurse
```

### Q3: JFR 文件无法打开

**解决方案**:
- 确保使用 JDK 21 生成的 JFR 文件用 JDK 21 的 JMC 打开
- 如果版本不匹配，尝试使用较新版本的 JMC

---

## 7. 下一步

工具安装完成后，参考以下文档：
- [JMeter Test Plan Design](JMeter-Test-Plan-Design.md) - 测试场景设计
- [Performance Optimization Plan](../docs/PERFORMANCE-OPTIMIZATION-PLAN.md) - 完整优化计划

---

**安装状态**: 
- [ ] JMeter ✅
- [ ] JDK Mission Control ✅
- [ ] VisualVM 🟡
- [ ] Arthas 🟡

**下一步**: 创建 JMeter 测试脚本
