# EVCS Manager 部署指南

## 📋 概述

本文档详细说明如何在生产环境中部署 EVCS Manager 系统。

## 🔧 环境要求

### 硬件要求

**最小配置**:
- CPU: 4核
- 内存: 8GB
- 磁盘: 100GB SSD

**推荐配置**:
- CPU: 8核以上
- 内存: 16GB以上
- 磁盘: 500GB SSD

### 软件要求

- **操作系统**: Linux (Ubuntu 20.04+ / CentOS 7+ / RHEL 8+)
- **Java**: OpenJDK 17或21
- **数据库**: PostgreSQL 14+
- **缓存**: Redis 6+
- **消息队列**: RabbitMQ 3.9+
- **容器**: Docker 20.10+ 和 Docker Compose 2.0+ (可选)

## 🐳 Docker Compose 部署（推荐）

### 1. 准备工作

```bash
# 克隆代码仓库
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr

# 确保有执行权限
chmod +x gradlew
```

### 2. 配置环境变量

创建 `.env` 文件：

```bash
# 数据库配置
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=evcs_db
POSTGRES_USER=evcs_user
POSTGRES_PASSWORD=your_secure_password

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# RabbitMQ配置
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=evcs
RABBITMQ_PASSWORD=your_rabbitmq_password

# JWT配置
JWT_SECRET=your_jwt_secret_key_at_least_32_characters
JWT_EXPIRATION=7200

# 服务端口
GATEWAY_PORT=8080
STATION_PORT=8081
ORDER_PORT=8082
PAYMENT_PORT=8083
AUTH_PORT=8084
TENANT_PORT=8085
```

### 3. 构建应用

```bash
# 构建所有服务
./gradlew build -x test

# 构建Docker镜像
./gradlew dockerBuild
```

### 4. 启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f evcs-gateway
```

### 5. 初始化数据库

```bash
# 执行数据库迁移脚本
docker-compose exec postgres psql -U evcs_user -d evcs_db -f /docker-entrypoint-initdb.d/init.sql

# 或者使用Flyway自动迁移（推荐）
# Flyway脚本位于各模块的 src/main/resources/db/migration 目录
```

### 6. 验证部署

```bash
# 检查所有服务健康状态
curl http://localhost:8080/actuator/health

# 访问API文档
# http://localhost:8080/doc.html

# 测试登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 🖥️ 传统部署方式

### 1. 安装依赖服务

#### 安装PostgreSQL

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install postgresql-14

# CentOS/RHEL
sudo yum install postgresql14-server
sudo postgresql-14-setup initdb
sudo systemctl start postgresql-14
sudo systemctl enable postgresql-14

# 创建数据库和用户
sudo -u postgres psql
CREATE DATABASE evcs_db;
CREATE USER evcs_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE evcs_db TO evcs_user;
\q
```

#### 安装Redis

```bash
# Ubuntu/Debian
sudo apt install redis-server

# CentOS/RHEL
sudo yum install redis

# 配置密码
sudo vi /etc/redis/redis.conf
# 添加: requirepass your_redis_password

sudo systemctl start redis
sudo systemctl enable redis
```

#### 安装RabbitMQ

```bash
# Ubuntu/Debian
sudo apt install rabbitmq-server

# CentOS/RHEL
sudo yum install rabbitmq-server

# 启动服务
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server

# 创建用户
sudo rabbitmqctl add_user evcs your_rabbitmq_password
sudo rabbitmqctl set_permissions -p / evcs ".*" ".*" ".*"
sudo rabbitmqctl set_user_tags evcs administrator

# 启用管理插件
sudo rabbitmq-plugins enable rabbitmq_management
```

### 2. 构建应用

```bash
# 构建所有模块
./gradlew build -x test

# JAR文件位置
# evcs-gateway/build/libs/evcs-gateway-1.0.0.jar
# evcs-station/build/libs/evcs-station-1.0.0.jar
# evcs-order/build/libs/evcs-order-1.0.0.jar
# ... 其他模块
```

### 3. 配置应用

为每个服务创建 `application-prod.yml`:

```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://localhost:5432/evcs_db
    username: evcs_user
    password: ${POSTGRES_PASSWORD}
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD}
  rabbitmq:
    host: localhost
    port: 5672
    username: evcs
    password: ${RABBITMQ_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration: 7200

logging:
  level:
    com.evcs: INFO
  file:
    name: /var/log/evcs/${spring.application.name}.log
```

### 4. 创建系统服务

创建 systemd 服务文件 `/etc/systemd/system/evcs-gateway.service`:

```ini
[Unit]
Description=EVCS Gateway Service
After=network.target postgresql.service redis.service

[Service]
Type=simple
User=evcs
WorkingDirectory=/opt/evcs
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="POSTGRES_PASSWORD=your_password"
Environment="REDIS_PASSWORD=your_redis_password"
Environment="RABBITMQ_PASSWORD=your_rabbitmq_password"
Environment="JWT_SECRET=your_jwt_secret"
ExecStart=/usr/bin/java -jar -Xmx2g -Xms2g /opt/evcs/evcs-gateway-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

对每个服务重复此过程（gateway, station, order, payment, auth, tenant）。

### 5. 启动服务

```bash
# 重新加载systemd配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start evcs-gateway
sudo systemctl start evcs-station
sudo systemctl start evcs-order
sudo systemctl start evcs-payment
sudo systemctl start evcs-auth
sudo systemctl start evcs-tenant

# 设置开机自启
sudo systemctl enable evcs-gateway
sudo systemctl enable evcs-station
sudo systemctl enable evcs-order
sudo systemctl enable evcs-payment
sudo systemctl enable evcs-auth
sudo systemctl enable evcs-tenant

# 查看服务状态
sudo systemctl status evcs-gateway
```

## ☸️ Kubernetes 部署

### 1. 准备 Helm Chart

```bash
# Chart结构
evcs-helm/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── configmap.yaml
│   └── secret.yaml
```

### 2. 安装

```bash
# 添加仓库
helm repo add evcs https://charts.evcs.io
helm repo update

# 安装
helm install evcs evcs/evcs-manager \
  --namespace evcs \
  --create-namespace \
  --set postgresql.password=your_password \
  --set redis.password=your_redis_password \
  --set jwt.secret=your_jwt_secret

# 查看状态
kubectl get pods -n evcs
```

## 🔒 安全配置

### 1. 防火墙配置

```bash
# 开放必要端口
sudo firewall-cmd --permanent --add-port=8080/tcp  # Gateway
sudo firewall-cmd --permanent --add-port=5432/tcp  # PostgreSQL
sudo firewall-cmd --permanent --add-port=6379/tcp  # Redis
sudo firewall-cmd --reload
```

### 2. SSL/TLS 配置

使用Nginx作为反向代理并配置SSL：

```nginx
server {
    listen 443 ssl http2;
    server_name api.evcs.example.com;

    ssl_certificate /etc/ssl/certs/evcs.crt;
    ssl_certificate_key /etc/ssl/private/evcs.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3. 数据库安全

```sql
-- 限制数据库访问
-- 编辑 pg_hba.conf
host    evcs_db    evcs_user    10.0.0.0/8    md5

-- 定期备份
pg_dump -U evcs_user evcs_db > backup_$(date +%Y%m%d).sql
```

## 📊 监控配置

### 1. Prometheus 监控

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'evcs-services'
    static_configs:
      - targets:
        - 'localhost:8080'  # Gateway
        - 'localhost:8081'  # Station
        - 'localhost:8082'  # Order
        - 'localhost:8083'  # Payment
        - 'localhost:8084'  # Auth
        - 'localhost:8085'  # Tenant
    metrics_path: '/actuator/prometheus'
```

### 2. Grafana Dashboard

导入EVCS Manager Dashboard (ID: 待发布)

### 3. 日志收集

使用ELK Stack收集日志：

```yaml
# filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/evcs/*.log
    json.keys_under_root: true
    json.add_error_key: true

output.logstash:
  hosts: ["logstash:5044"]
```

## 🔄 灰度发布

### 1. 准备新版本

```bash
# 构建新版本
./gradlew build -Pversion=1.1.0

# 标记镜像
docker tag evcs-gateway:1.1.0 evcs-gateway:canary
```

### 2. 分阶段部署

```bash
# 5% 流量
kubectl set image deployment/evcs-gateway \
  evcs-gateway=evcs-gateway:canary --record
kubectl scale deployment/evcs-gateway-canary --replicas=1

# 观察1天后，扩大到20%
kubectl scale deployment/evcs-gateway-canary --replicas=2

# 继续观察并逐步增加到100%
```

## 🔙 回滚方案

### Docker Compose回滚

```bash
# 回滚到上一个版本
docker-compose down
docker-compose up -d --build
```

### Kubernetes回滚

```bash
# 查看部署历史
kubectl rollout history deployment/evcs-gateway -n evcs

# 回滚到上一个版本
kubectl rollout undo deployment/evcs-gateway -n evcs

# 回滚到指定版本
kubectl rollout undo deployment/evcs-gateway --to-revision=2 -n evcs
```

## 📝 故障排查

### 常见问题

#### 1. 服务无法启动

```bash
# 查看日志
docker-compose logs evcs-gateway
# 或
journalctl -u evcs-gateway -f

# 检查端口占用
sudo netstat -tlnp | grep 8080

# 检查Java进程
ps aux | grep java
```

#### 2. 数据库连接失败

```bash
# 测试数据库连接
psql -h localhost -U evcs_user -d evcs_db

# 检查PostgreSQL日志
sudo tail -f /var/log/postgresql/postgresql-14-main.log
```

#### 3. Redis连接失败

```bash
# 测试Redis连接
redis-cli -h localhost -p 6379 -a your_password ping

# 检查Redis日志
sudo tail -f /var/log/redis/redis-server.log
```

## 📞 技术支持

- **文档**: https://docs.evcs.io
- **问题反馈**: GitHub Issues
- **紧急联系**: support@evcs.io

---

**文档版本**: v1.0.0  
**最后更新**: 2025-01-08
