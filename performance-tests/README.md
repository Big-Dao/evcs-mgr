# JMeter 测试使用指南

> **目标**: 执行 JVM 性能基线测试，为 Week 2 优化提供数据支撑  
> **前置条件**: Docker 服务运行，EVCS 所有服务已启动  
> **预计时长**: 快速验证 1 分钟，完整测试 25 分钟

---

## 📋 测试场景概览

| 场景 | 接口 | 目标 TPS | 并发用户 | 持续时间 | 用途 |
|------|------|----------|----------|----------|------|
| 场景1 | POST /orders/start | 500 | 100 | 10 分钟 | 订单创建压测 |
| 场景2 | GET /orders/page | 1000 | 200 | 10 分钟 | 查询性能测试 |
| 场景3 | POST /orders/{id}/to-pay | 2000 | 500 | 5 分钟 | 状态更新压测 |

---

## 🚀 快速开始

### 步骤 1: 启动 EVCS 服务

```powershell
# 1.1 启动所有 Docker 服务
cd c:\Users\andyz\Projects\evcs-mgr
docker-compose up -d

# 1.2 等待服务启动完成（约 2 分钟）
.\scripts\health-check.sh

# 1.3 检查服务状态
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | Select-String "evcs-"
```

**预期输出**：所有 evcs-* 服务状态为 `Up`

---

### 步骤 2: 快速验证环境

```powershell
cd performance-tests

# 执行快速验证测试（5用户 x 5循环 = 25请求）
.\quick-verify.ps1
```

**预期结果**：
```
=== 验证完成 ===

总请求数: 25
成功请求: 25
失败请求: 0

✅ 环境验证成功！可以执行完整性能测试
```

---

### 步骤 3: 执行完整性能测试

```powershell
# 方式 1: 执行所有场景（推荐用于基线测试）
.\run-test.ps1

# 方式 2: 仅执行单个场景
.\run-test.ps1 -Scenario scenario1  # 订单创建
.\run-test.ps1 -Scenario scenario2  # 订单查询
.\run-test.ps1 -Scenario scenario3  # 状态更新

# 方式 3: 自定义目标地址
.\run-test.ps1 -BaseUrl "http://192.168.1.100:8080"
```

**测试时长**：
- 全部场景：~25 分钟（场景 1: 10分钟 + 场景 2: 10分钟 + 场景 3: 5分钟）
- 单个场景：5-10 分钟

---

## 📊 Week 2 JVM 性能基线测试

### 测试文件

- **jvm-tuning-test.jmx** - JMeter 测试计划（3 个场景）
- **run-test.ps1** - 完整测试执行脚本
- **quick-verify.ps1** - 快速环境验证脚本

### 快速执行

```powershell
cd performance-tests

# 1. 快速验证（1分钟）
.\quick-verify.ps1

# 2. 完整测试（25分钟）
.\run-test.ps1
```

### 关键指标

| 指标 | 目标值 |
|------|--------|
| 响应时间 P99 | < 500ms |
| 错误率 | < 1% |
| GC 暂停时间 | < 200ms |

详见：[JMeter-Test-Plan-Design.md](JMeter-Test-Plan-Design.md)

---

## 原 Week 4 文档

### 3. Charger Status Update Test
- **Endpoint**: PUT /api/chargers/{id}/status
- **Target TPS**: 2000 transactions per second
- **Duration**: 5 minutes
- **Script**: jmeter/charger-status-update-test.jmx

## Performance Targets

| Metric | Target |
|--------|--------|
| Order Creation TPS | >= 500 |
| Order Query TPS | >= 1000 |
| Charger Status Update TPS | >= 2000 |
| P99 Response Time | < 200ms |
| Error Rate | < 0.1% |

## Running Tests

```bash
# Run in non-GUI mode
jmeter -n -t order-creation-test.jmx -l results/order-creation.jtl -e -o results/order-creation-report
```

For detailed instructions, see the full documentation in this directory.

