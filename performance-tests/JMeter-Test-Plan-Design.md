# JMeter 性能测试脚本设计

> **创建日期**: 2025-10-23  
> **目的**: JVM 调优性能基线测试  
> **工具**: Apache JMeter 5.6+

---

## 测试环境要求

### 1. 软件依赖
- Apache JMeter 5.6+
- Java 21 (运行 JMeter)
- curl 或 Postman（用于验证 API）

### 2. 服务依赖
- PostgreSQL (5432)
- Redis (6379)
- RabbitMQ (5672)
- 所有 EVCS 微服务运行中

---

## 测试场景设计

### 场景 1: 订单创建高并发测试

**目标**: 模拟充电订单创建高并发场景，测试 GC 暂停对响应时间的影响

**测试参数**:
```
Thread Group:
  - Number of Threads (users): 100
  - Ramp-up Period: 30 秒
  - Loop Count: 无限循环
  - Duration: 600 秒 (10 分钟)

Target TPS: 500
Expected Response Time (P99): <200ms
```

**HTTP 请求配置**:
```
Method: POST
Path: /api/orders/start
Headers:
  Content-Type: application/json
  Authorization: Bearer ${JWT_TOKEN}

Body (JSON):
{
  "stationId": ${__Random(1,100)},
  "chargerId": ${__Random(1,500)},
  "userId": ${__Random(1000,9999)},
  "tenantId": 1
}
```

**监控指标**:
- 响应时间分布 (平均、P50、P90、P95、P99)
- 吞吐量 (TPS)
- 错误率 (%)
- 同时观察 JFR 中的 GC 暂停时间

---

### 场景 2: 订单查询混合负载测试

**目标**: 模拟订单查询的混合负载，测试内存使用和 GC 频率

**测试参数**:
```
Thread Group:
  - Number of Threads (users): 200
  - Ramp-up Period: 60 秒
  - Loop Count: 无限循环
  - Duration: 600 秒 (10 分钟)

Target TPS: 1000
Expected Response Time (P99): <150ms
```

**HTTP 请求配置 (混合比例)**:
```
1. 订单列表查询 (50%):
   GET /api/orders/list?page=1&size=20&tenantId=1

2. 订单详情查询 (30%):
   GET /api/orders/${ORDER_ID}

3. 订单状态查询 (20%):
   GET /api/orders/${ORDER_ID}/status
```

**监控指标**:
- 响应时间分布
- 吞吐量 (TPS)
- 堆内存使用率
- GC 频率 (Young GC / Full GC)

---

### 场景 3: 充电状态更新峰值测试

**目标**: 模拟充电桩状态更新的峰值流量，测试 CPU 和 GC 在高负载下的表现

**测试参数**:
```
Thread Group:
  - Number of Threads (users): 500
  - Ramp-up Period: 30 秒
  - Loop Count: 无限循环
  - Duration: 300 秒 (5 分钟)

Target TPS: 2000
Expected Response Time (P99): <100ms
```

**HTTP 请求配置**:
```
Method: PUT
Path: /api/chargers/${CHARGER_ID}/status
Headers:
  Content-Type: application/json

Body (JSON):
{
  "chargerId": ${__Random(1,500)},
  "status": "${__RandomString(8,IDLE|CHARGING|AVAILABLE)}",
  "power": ${__Random(0,100)},
  "current": ${__Random(0,200)},
  "voltage": ${__Random(200,240)},
  "tenantId": 1
}
```

**监控指标**:
- 响应时间分布
- 吞吐量 (TPS)
- CPU 使用率
- GC 暂停时间和频率

---

## JMeter 测试脚本结构

### 文件: `performance-tests/jvm-tuning-test.jmx`

```
Test Plan: JVM Tuning Performance Test
├── User Defined Variables (全局变量)
│   ├── BASE_URL = http://localhost:8080
│   ├── JWT_TOKEN = ${__P(jwt.token,test-token)}
│   └── TENANT_ID = 1
│
├── HTTP Header Manager (全局请求头)
│   ├── Content-Type: application/json
│   └── Authorization: Bearer ${JWT_TOKEN}
│
├── HTTP Cookie Manager (会话管理)
│
├── Thread Group 1: Order Creation Test
│   ├── HTTP Request: POST /api/orders/start
│   ├── Constant Throughput Timer: 500 TPS
│   ├── Response Assertion: Status Code = 200
│   └── Listeners:
│       ├── Summary Report
│       ├── Response Time Graph
│       └── Aggregate Report
│
├── Thread Group 2: Order Query Test
│   ├── HTTP Request: GET /api/orders/list
│   ├── HTTP Request: GET /api/orders/${ORDER_ID}
│   ├── HTTP Request: GET /api/orders/${ORDER_ID}/status
│   ├── Constant Throughput Timer: 1000 TPS
│   └── Listeners (同上)
│
└── Thread Group 3: Status Update Test
    ├── HTTP Request: PUT /api/chargers/${CHARGER_ID}/status
    ├── Constant Throughput Timer: 2000 TPS
    └── Listeners (同上)
```

---

## 执行步骤

### 1. 准备阶段

```bash
# 1.1 启动所有服务
cd c:\Users\andyz\Projects\evcs-mgr
docker-compose up -d

# 1.2 等待服务就绪
Start-Sleep -Seconds 60

# 1.3 验证服务健康状态
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### 2. 执行测试

```bash
# 2.1 切换到测试目录
cd performance-tests

# 2.2 运行基线测试（非 GUI 模式）
jmeter -n -t jvm-tuning-test.jmx `
  -l results/baseline/results.jtl `
  -e -o results/baseline/report `
  -Jjwt.token=your-test-token

# 2.3 等待测试完成（约 25 分钟）
```

### 3. 收集 JFR 数据

```bash
# 3.1 从容器复制 JFR 文件
docker cp evcs-order:/app/logs/flight.jfr ./results/baseline/order-flight.jfr
docker cp evcs-station:/app/logs/flight.jfr ./results/baseline/station-flight.jfr

# 3.2 使用 JDK Mission Control 分析
# jmc results/baseline/order-flight.jfr
```

---

## 报告生成

### JMeter HTML 报告

测试完成后，JMeter 会自动生成 HTML 报告：
```
results/baseline/report/index.html
```

**关键指标**:
- ✅ **Throughput**: 实际 TPS
- ✅ **Response Time**: P50, P90, P95, P99
- ✅ **Error Rate**: 错误百分比
- ✅ **Min/Max/Avg Response Time**

### JFR 分析报告

使用 JDK Mission Control 查看：
- ✅ **GC Pauses**: 暂停时间分布
- ✅ **Heap Usage**: 内存使用曲线
- ✅ **Hot Methods**: CPU 热点方法
- ✅ **Allocations**: 内存分配热点

---

## 预期输出

### 基线测试报告结构

```
results/
└── baseline/
    ├── results.jtl              # 原始数据
    ├── report/                  # HTML 报告
    │   ├── index.html
    │   ├── content/
    │   └── sbadmin2-1.0.7/
    ├── order-flight.jfr         # Order 服务 JFR
    ├── station-flight.jfr       # Station 服务 JFR
    └── summary.md               # 人工总结
```

### summary.md 模板

```markdown
# 基线性能测试总结

## 测试环境
- 日期: 2025-10-XX
- JVM 配置: Xms256m Xmx512m, 默认 GC
- 并发用户: 100 / 200 / 500

## 关键指标

| 场景 | 目标 TPS | 实际 TPS | P99 响应时间 | 错误率 |
|------|---------|----------|-------------|--------|
| 订单创建 | 500 | XXX | XXX ms | X% |
| 订单查询 | 1000 | XXX | XXX ms | X% |
| 状态更新 | 2000 | XXX | XXX ms | X% |

## GC 分析

| 服务 | GC 算法 | P99 暂停时间 | GC 频率 | 堆使用率 |
|------|---------|-------------|---------|---------|
| evcs-order | G1GC | XXX ms | XX 次/分 | XX% |
| evcs-station | G1GC | XXX ms | XX 次/分 | XX% |

## 问题与优化建议
- [ ] 问题1: ...
- [ ] 建议1: ...
```

---

## 下一步计划

### Week 2 Day 2-3: GC 调优测试

重复上述测试流程，测试不同 GC 配置：
1. G1GC 保守配置
2. G1GC 激进配置
3. G1GC 平衡配置
4. ZGC 默认配置
5. ZGC 优化配置

每个配置生成独立报告：
```
results/
├── baseline/
├── g1gc-conservative/
├── g1gc-aggressive/
├── g1gc-balanced/
├── zgc-default/
└── zgc-optimized/
```

### Week 2 Day 3 下午: 对比分析

创建对比分析报告 `docs/JVM-TUNING-REPORT.md`，包含：
- 各配置性能对比表
- 性能曲线图
- 推荐配置和理由

---

**文档状态**: ✅ 设计完成  
**下一步**: 创建 JMeter 脚本文件  
**预计完成时间**: 2025-10-24
