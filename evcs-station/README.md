# EVCS充电站管理服务

> **端口**: 8082 | **状态**: 活跃
>
> **功能**: 充电站设备管理和控制

## 📋 服务概述

EVCS充电站管理服务负责充电站的业务逻辑，包括：
- 充电站信息管理
- 充电桩设备控制
- 充电状态监控
- 设备故障诊断
- 充电记录管理

## 🔧 技术栈
- Spring Boot 3.2.10
- Spring Data JPA
- PostgreSQL
- RabbitMQ (消息队列)
- MQTT (设备通信)

## 🚀 快速启动

### 本地开发
```bash
# 构建服务
./gradlew :evcs-station:build

# 运行服务
./gradlew :evcs-station:bootRun

# 访问健康检查
curl http://localhost:8082/actuator/health
```

### Docker部署
```bash
# 构建镜像
docker build -t evcs-station:latest ./evcs-station

# 运行容器
docker run -p 8082:8082 evcs-station:latest
```

## 📡 API端点

### 充电站管理
- `GET /api/v1/stations` - 获取充电站列表
- `POST /api/v1/stations` - 创建充电站
- `GET /api/v1/stations/{id}` - 获取充电站详情
- `PUT /api/v1/stations/{id}` - 更新充电站信息
- `DELETE /api/v1/stations/{id}` - 删除充电站

### 充电桩管理
- `GET /api/v1/stations/{stationId}/chargers` - 获取充电桩列表
- `POST /api/v1/stations/{stationId}/chargers` - 添加充电桩
- `PUT /api/v1/chargers/{id}` - 更新充电桩状态
- `POST /api/v1/chargers/{id}/start` - 开始充电
- `POST /api/v1/chargers/{id}/stop` - 停止充电

### 充电记录
- `GET /api/v1/charging-records` - 获取充电记录
- `GET /api/v1/charging-records/{id}` - 获取充电记录详情

## 🔌 设备通信

### MQTT主题
- `evcs/charger/{id}/status` - 充电桩状态
- `evcs/charger/{id}/control` - 充电桩控制
- `evcs/charger/{id}/data` - 充电数据

### 消息格式
```json
{
  "chargerId": "CH001",
  "status": "CHARGING",
  "power": 22.5,
  "voltage": 380,
  "current": 59.2,
  "timestamp": "2025-11-07T10:30:00Z"
}
```

## 📊 业务逻辑

### 充电状态管理
- **IDLE**: 空闲状态
- **CHARGING**: 充电中
- **COMPLETED**: 充电完成
- **FAULT**: 故障状态
- **MAINTENANCE**: 维护状态

### 计费规则
- 按时间计费
- 按电量计费
- 阶梯价格
- 服务费计算

## ⚙️ 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/evcs_station
    username: ${DB_USERNAME:evcs}
    password: ${DB_PASSWORD:password}
```

### MQTT配置
```yaml
evcs:
  mqtt:
    broker-url: tcp://localhost:1883
    username: ${MQTT_USERNAME:evcs}
    password: ${MQTT_PASSWORD:password}
    client-id: evcs-station
```

## 🔗 相关链接

- [API文档](../../docs/references/API-DOCUMENTATION.md#evcs-station)
- [项目文档](../../docs/README.md)
- [开发指南](../../docs/development/DEVELOPER-GUIDE.md)
- [架构设计](../../docs/architecture/architecture.md)

## 🧪 测试

```bash
# 运行单元测试
./gradlew :evcs-station:test

# 运行集成测试
./gradlew :evcs-station:integrationTest

# 运行设备通信测试
./gradlew :evcs-station:deviceTest
```

## 🚨 故障排查

### 常见问题
1. **设备离线**: 检查MQTT连接和网络状态
2. **充电失败**: 检查充电桩状态和权限验证
3. **数据不同步**: 检查消息队列连接

### 监控指标
- 设备在线率
- 充电成功率
- 平均充电时长
- 故障响应时间

---

**服务负责人**: 充电站团队
**代码审查**: 设备团队
**部署负责人**: 运维团队