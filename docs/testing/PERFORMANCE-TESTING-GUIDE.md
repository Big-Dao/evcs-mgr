# EVCS 性能测试指南

> **版本**: v1.0  
> **创建日期**: 2026-01-13  
> **维护者**: QA/性能团队  
> **状态**: 已发布

---

## 1. 概述

本文档定义 EVCS 充电站管理系统的性能测试规范，包括测试场景、工具配置、基线指标和报告模板。

### 1.1 测试目标

- **容量规划**: 确定系统最大处理能力
- **性能基线**: 建立性能指标基准
- **瓶颈识别**: 发现系统性能瓶颈
- **回归验证**: 确保新版本性能不退化

### 1.2 测试类型

| 类型 | 目的 | 执行时机 |
|------|------|----------|
| **负载测试** | 验证正常负载下的性能 | 每次发布 |
| **压力测试** | 确定系统极限容量 | 每季度 |
| **耐久测试** | 验证长期运行稳定性 | 每月 |
| **峰值测试** | 验证突发流量处理 | 每季度 |

---

## 2. 性能基线

### 2.1 API 响应时间

| 接口分类 | P50 | P95 | P99 | 最大 |
|----------|-----|-----|-----|------|
| 简单查询 | < 50ms | < 100ms | < 200ms | < 500ms |
| 复杂查询 | < 100ms | < 300ms | < 500ms | < 1s |
| 写操作 | < 100ms | < 300ms | < 500ms | < 1s |
| 批量操作 | < 500ms | < 1s | < 2s | < 5s |

### 2.2 吞吐量目标

| 场景 | 目标 TPS | 并发用户 |
|------|----------|----------|
| 日常运营 | 500 | 100 |
| 高峰时段 | 1000 | 300 |
| 极限容量 | 2000 | 500 |

### 2.3 资源使用

| 指标 | 正常 | 警告 | 危险 |
|------|------|------|------|
| CPU 使用率 | < 60% | 60-80% | > 80% |
| 内存使用率 | < 70% | 70-85% | > 85% |
| 数据库连接 | < 60% | 60-80% | > 80% |
| 网络带宽 | < 50% | 50-70% | > 70% |

---

## 3. 测试场景

### 3.1 核心业务场景

#### 场景 1: 用户登录

```yaml
name: 用户登录
endpoint: POST /api/v1/auth/login
weight: 10%
think_time: 2s
validations:
  - response_code: 200
  - response_time_p95: < 200ms
```

#### 场景 2: 查询站点列表

```yaml
name: 查询站点列表
endpoint: GET /api/v1/stations
weight: 20%
think_time: 3s
parameters:
  page: 1
  size: 20
  lat: 39.9042
  lng: 116.4074
  radius: 5000
validations:
  - response_code: 200
  - response_time_p95: < 300ms
```

#### 场景 3: 创建充电订单

```yaml
name: 创建充电订单
endpoint: POST /api/v1/orders
weight: 15%
think_time: 5s
prerequisites:
  - 用户登录
  - 充电桩空闲
validations:
  - response_code: 200
  - response_time_p95: < 500ms
```

#### 场景 4: 支付订单

```yaml
name: 支付订单
endpoint: POST /api/v1/payments
weight: 10%
think_time: 3s
prerequisites:
  - 订单创建成功
validations:
  - response_code: 200
  - response_time_p95: < 500ms
```

#### 场景 5: 充电状态查询

```yaml
name: 充电状态查询
endpoint: GET /api/v1/orders/{orderId}/status
weight: 25%
think_time: 10s
loop: true
validations:
  - response_code: 200
  - response_time_p95: < 100ms
```

### 3.2 混合场景配置

```yaml
scenarios:
  - name: 日常运营
    duration: 30m
    users: 100
    ramp_up: 5m
    composition:
      - login: 10%
      - query_stations: 30%
      - create_order: 15%
      - payment: 10%
      - query_status: 25%
      - other: 10%

  - name: 高峰时段
    duration: 60m
    users: 300
    ramp_up: 10m
    composition:
      - login: 5%
      - query_stations: 25%
      - create_order: 25%
      - payment: 20%
      - query_status: 20%
      - other: 5%

  - name: 压力测试
    duration: 30m
    users: 500
    ramp_up: 15m
    step_users: 50
    step_duration: 3m
```

---

## 4. JMeter 配置

### 4.1 目录结构

```
performance-tests/
├── config/
│   ├── env-dev.properties
│   ├── env-staging.properties
│   └── env-prod.properties
├── data/
│   ├── users.csv
│   ├── stations.csv
│   └── chargers.csv
├── scripts/
│   ├── evcs-login.jmx
│   ├── evcs-order.jmx
│   └── evcs-mixed.jmx
├── lib/
│   └── plugins/
└── reports/
    └── 2026-01-13/
```

### 4.2 JMeter 测试计划

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="EVCS性能测试">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">${__P(base.url,http://localhost:8080)}</stringProp>
          </elementProp>
          <elementProp name="THREAD_COUNT" elementType="Argument">
            <stringProp name="Argument.name">THREAD_COUNT</stringProp>
            <stringProp name="Argument.value">${__P(threads,100)}</stringProp>
          </elementProp>
          <elementProp name="RAMP_UP" elementType="Argument">
            <stringProp name="Argument.name">RAMP_UP</stringProp>
            <stringProp name="Argument.value">${__P(rampup,300)}</stringProp>
          </elementProp>
          <elementProp name="DURATION" elementType="Argument">
            <stringProp name="Argument.name">DURATION</stringProp>
            <stringProp name="Argument.value">${__P(duration,1800)}</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <!-- Thread Group -->
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="用户组">
        <intProp name="ThreadGroup.num_threads">${THREAD_COUNT}</intProp>
        <intProp name="ThreadGroup.ramp_time">${RAMP_UP}</intProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
        <stringProp name="ThreadGroup.duration">${DURATION}</stringProp>
      </ThreadGroup>
      <hashTree>
        <!-- HTTP Header Manager -->
        <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP头">
          <collectionProp name="HeaderManager.headers">
            <elementProp name="Content-Type" elementType="Header">
              <stringProp name="Header.name">Content-Type</stringProp>
              <stringProp name="Header.value">application/json</stringProp>
            </elementProp>
            <elementProp name="Authorization" elementType="Header">
              <stringProp name="Header.name">Authorization</stringProp>
              <stringProp name="Header.value">Bearer ${token}</stringProp>
            </elementProp>
          </collectionProp>
        </HeaderManager>
        <!-- HTTP Sampler -->
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="登录">
          <stringProp name="HTTPSampler.domain">${__P(host,localhost)}</stringProp>
          <intProp name="HTTPSampler.port">${__P(port,8080)}</intProp>
          <stringProp name="HTTPSampler.path">/api/v1/auth/login</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### 4.3 运行命令

```bash
#!/bin/bash
# 运行性能测试

ENV=${1:-dev}
THREADS=${2:-100}
DURATION=${3:-1800}

# 加载环境配置
source config/env-${ENV}.properties

# 创建报告目录
REPORT_DIR="reports/$(date +%Y-%m-%d_%H%M%S)"
mkdir -p ${REPORT_DIR}

# 运行测试
jmeter -n \
  -t scripts/evcs-mixed.jmx \
  -Dbase.url=${BASE_URL} \
  -Dthreads=${THREADS} \
  -Dduration=${DURATION} \
  -l ${REPORT_DIR}/results.jtl \
  -e -o ${REPORT_DIR}/html

echo "测试报告: ${REPORT_DIR}/html/index.html"
```

---

## 5. Gatling 配置

### 5.1 Scala 测试脚本

```scala
package evcs

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class EvcsSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val userFeeder = csv("data/users.csv").circular

  val login = exec(
    http("登录")
      .post("/api/v1/auth/login")
      .body(StringBody("""{"username":"${username}","password":"${password}"}"""))
      .check(status.is(200))
      .check(jsonPath("$.data.token").saveAs("token"))
  )

  val queryStations = exec(
    http("查询站点")
      .get("/api/v1/stations")
      .queryParam("page", "1")
      .queryParam("size", "20")
      .header("Authorization", "Bearer ${token}")
      .check(status.is(200))
      .check(responseTimeInMillis.lt(300))
  )

  val createOrder = exec(
    http("创建订单")
      .post("/api/v1/orders")
      .header("Authorization", "Bearer ${token}")
      .body(StringBody("""{"chargerId":${chargerId},"connectorId":1}"""))
      .check(status.is(200))
      .check(jsonPath("$.data.orderId").saveAs("orderId"))
  )

  val queryOrderStatus = repeat(10) {
    exec(
      http("查询订单状态")
        .get("/api/v1/orders/${orderId}/status")
        .header("Authorization", "Bearer ${token}")
        .check(status.is(200))
    ).pause(5)
  }

  val userScenario = scenario("用户场景")
    .feed(userFeeder)
    .exec(login)
    .pause(2)
    .exec(queryStations)
    .pause(3)
    .exec(createOrder)
    .pause(1)
    .exec(queryOrderStatus)

  setUp(
    userScenario.inject(
      rampUsers(100).during(5.minutes),
      constantUsersPerSec(10).during(30.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile(95).lt(500),
     global.successfulRequests.percent.gt(99)
   )
}
```

### 5.2 运行命令

```bash
# 使用 Maven 运行
mvn gatling:test -Dgatling.simulationClass=evcs.EvcsSimulation

# 使用 SBT 运行
sbt "Gatling/testOnly evcs.EvcsSimulation"
```

---

## 6. 协议层压测

### 6.1 WebSocket 压测

```python
#!/usr/bin/env python3
# ws_stress_test.py

import asyncio
import websockets
import json
import time
from concurrent.futures import ThreadPoolExecutor

async def simulate_charger(charger_id, duration):
    """模拟充电桩"""
    uri = f"ws://localhost:8085/ws/charger/{charger_id}"
    
    start_time = time.time()
    message_count = 0
    
    async with websockets.connect(uri) as websocket:
        while time.time() - start_time < duration:
            # 发送心跳
            heartbeat = {
                "type": "heartbeat",
                "chargerId": charger_id,
                "timestamp": int(time.time() * 1000)
            }
            await websocket.send(json.dumps(heartbeat))
            
            # 接收响应
            response = await asyncio.wait_for(websocket.recv(), timeout=5)
            message_count += 1
            
            await asyncio.sleep(30)  # 30秒心跳间隔
    
    return charger_id, message_count

async def main(charger_count, duration):
    """主函数"""
    tasks = [
        simulate_charger(f"CHARGER_{i:04d}", duration)
        for i in range(charger_count)
    ]
    
    results = await asyncio.gather(*tasks)
    
    total_messages = sum(r[1] for r in results)
    print(f"总充电桩: {charger_count}")
    print(f"总消息数: {total_messages}")
    print(f"平均消息: {total_messages / charger_count}")

if __name__ == "__main__":
    import sys
    charger_count = int(sys.argv[1]) if len(sys.argv) > 1 else 1000
    duration = int(sys.argv[2]) if len(sys.argv) > 2 else 300
    
    asyncio.run(main(charger_count, duration))
```

### 6.2 OCPP 模拟器

```python
#!/usr/bin/env python3
# ocpp_simulator.py

import asyncio
import websockets
from datetime import datetime
from ocpp.v16 import ChargePoint as cp
from ocpp.v16.enums import RegistrationStatus
from ocpp.v16 import call

class ChargePointSimulator(cp):
    async def send_boot_notification(self):
        request = call.BootNotificationPayload(
            charge_point_model="Simulator",
            charge_point_vendor="EVCS"
        )
        response = await self.call(request)
        return response.status == RegistrationStatus.accepted

    async def send_heartbeat(self):
        request = call.HeartbeatPayload()
        response = await self.call(request)
        return response

    async def start_transaction(self, connector_id, id_tag, meter_start):
        request = call.StartTransactionPayload(
            connector_id=connector_id,
            id_tag=id_tag,
            meter_start=meter_start,
            timestamp=datetime.utcnow().isoformat()
        )
        response = await self.call(request)
        return response.transaction_id

async def simulate_charge_point(cp_id, duration):
    uri = f"ws://localhost:8085/ocpp/{cp_id}"
    
    async with websockets.connect(uri, subprotocols=["ocpp1.6"]) as ws:
        cp = ChargePointSimulator(cp_id, ws)
        
        await asyncio.gather(
            cp.start(),
            run_simulation(cp, duration)
        )

async def run_simulation(cp, duration):
    # Boot notification
    await cp.send_boot_notification()
    
    start_time = asyncio.get_event_loop().time()
    
    while asyncio.get_event_loop().time() - start_time < duration:
        # Heartbeat
        await cp.send_heartbeat()
        await asyncio.sleep(30)

if __name__ == "__main__":
    asyncio.run(simulate_charge_point("CP001", 300))
```

---

## 7. 监控与分析

### 7.1 测试期间监控

```yaml
# Prometheus 查询

# QPS
rate(http_server_requests_seconds_count[1m])

# 响应时间 P99
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[1m]))

# 错误率
sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) 
/ sum(rate(http_server_requests_seconds_count[1m]))

# 数据库连接使用
hikaricp_connections_active / hikaricp_connections_max

# JVM 堆内存
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

### 7.2 性能分析工具

```bash
# 火焰图生成
async-profiler -d 60 -f flamegraph.html <pid>

# JVM 堆分析
jmap -dump:format=b,file=heap.hprof <pid>
jhat heap.hprof

# 线程分析
jstack <pid> > thread_dump.txt

# GC 日志分析
java -Xlog:gc*:gc.log:time,uptime:filecount=5,filesize=10M
```

---

## 8. 报告模板

### 8.1 性能测试报告

```markdown
# EVCS 性能测试报告

## 基本信息
- **测试日期**: 2026-01-13
- **测试环境**: Staging
- **测试版本**: v1.2.3
- **执行人员**: QA 团队

## 测试场景
- 场景类型: 混合场景
- 并发用户: 100
- 测试时长: 30 分钟
- 递增时间: 5 分钟

## 测试结果

### 响应时间
| 接口 | 样本数 | 平均值 | P50 | P95 | P99 | 最大值 |
|------|--------|--------|-----|-----|-----|--------|
| 登录 | 10000 | 85ms | 75ms | 150ms | 280ms | 1.2s |
| 查询站点 | 30000 | 120ms | 100ms | 250ms | 450ms | 2.1s |
| 创建订单 | 15000 | 180ms | 150ms | 350ms | 580ms | 3.2s |

### 吞吐量
- 平均 TPS: 850
- 峰值 TPS: 1200
- 错误率: 0.05%

### 资源使用
| 资源 | 平均值 | 峰值 |
|------|--------|------|
| CPU | 45% | 72% |
| 内存 | 65% | 78% |
| 数据库连接 | 40% | 65% |

## 结论
- [x] 满足性能基线要求
- [x] 无明显性能瓶颈
- [ ] 建议优化查询站点接口

## 改进建议
1. 添加站点列表缓存
2. 优化地理位置查询索引
```

---

## 9. 相关文档

- [资源规划指南](../deployment/RESOURCE-PLANNING-GUIDE.md)
- [监控告警配置指南](MONITORING-ALERTING-GUIDE.md)
- [系统架构风险审计报告](../architecture/RISK-AUDIT-REPORT.md)

---

## 10. 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.0 | 初始版本 |
