# 性能测试工具安装指南

> **目标**: 安装性能测试与 JFR 分析所需的工具  
> **平台**: Linux  
> **预计时间**: 15-30 分钟

---

## Linux 快速安装（推荐）

### 1) 安装 Java 21

（示例：Ubuntu/Debian）

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk
java -version
```

### 2) 安装 Apache JMeter（推荐：官方发行包）

说明：发行版仓库的 `jmeter` 包可能版本较旧，建议下载官方 tar 包。

```bash
cd tmp
JMETER_VERSION="5.6.3"
curl -fL -o apache-jmeter.tgz "https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz"
tar -xzf apache-jmeter.tgz
export PATH="$PWD/apache-jmeter-${JMETER_VERSION}/bin:$PATH"
jmeter --version
```

### 3) JFR 分析工具

- **JDK Mission Control**：如果你使用的是带 JMC 的发行版/套件，直接使用即可。
- 或使用 IDE / `jfr print` 等工具进行基础分析。

---

## 📋 工具清单

| 工具 | 版本 | 用途 | 必须？ |
|------|------|------|--------|
| Apache JMeter | 5.6.3+ | 性能压测 | ✅ 必须 |
| JDK Mission Control | 9.0+ | JFR 分析 | ✅ 必须 |
| VisualVM | 2.1.8+ | JVM 监控 | 🟡 可选 |
| Arthas | 3.7+ | 实时诊断 | 🟡 可选 |

---

## 安装验证（Linux）

```bash
java -version
jmeter --version

# 可选：跑一次快速验证（需要已安装 JMeter，并且服务可访问）
cd performance-tests
./health-check.sh --help
./quick-verify.sh --help
```

---

## 下一步

工具安装完成后，参考以下文档：
- [JMeter Test Plan Design](JMeter-Test-Plan-Design.md) - 测试场景设计
- [Performance Optimization Plan（归档）](../docs/archive/obsolete-docs-2025-11-02/performance-reports/PERFORMANCE-OPTIMIZATION-PLAN.md) - 完整优化计划（历史快照）

---

**安装状态**: 
- [ ] JMeter ✅
- [ ] JDK Mission Control ✅
- [ ] VisualVM 🟡
- [ ] Arthas 🟡

**下一步**: 创建 JMeter 测试脚本
