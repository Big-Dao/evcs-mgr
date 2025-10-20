# 测试环境快速开始

> **最后更新**: 2025-10-20  
> **维护者**: EVCS QA Team  
> **状态**: 已发布  
> **简介**: 5分钟快速部署EVCS Manager测试环境

## 🚀 快速部署

### Linux/macOS

```bash
# 1. 克隆代码（如果还没有）
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr

# 2. 启动测试环境
./scripts/start-test.sh

# 3. 等待服务启动（约1-2分钟）
# 脚本会自动显示服务状态

# 4. 验证环境
./scripts/health-check.sh

# 5. 运行冒烟测试
./scripts/smoke-test.sh
```

### Windows

```powershell
# 1. 克隆代码（如果还没有）
git clone https://github.com/Big-Dao/evcs-mgr.git
cd evcs-mgr

# 2. 启动测试环境
docker compose -f docker-compose.test.yml up --build -d

# 3. 等待服务启动
Start-Sleep -Seconds 30

# 4. 检查服务状态
docker compose -f docker-compose.test.yml ps
```

## 📍 访问地址

| 服务 | 地址 | 凭证 |
|------|------|------|
| **租户服务API** | http://localhost:8081 | - |
| **充电站服务API** | http://localhost:8082 | - |
| **数据库管理** | http://localhost:8090 | postgres / evcs_test / test_password_123 |
| **RabbitMQ管理** | http://localhost:15672 | evcs_test / test_mq_123 |
| **租户服务健康检查** | http://localhost:8081/actuator/health | - |
| **充电站服务健康检查** | http://localhost:8082/actuator/health | - |

## 🧪 测试验证

### 方式1：自动化测试

```bash
# 健康检查（检查所有服务是否正常）
./scripts/health-check.sh

# 冒烟测试（运行13项基础测试）
./scripts/smoke-test.sh
```

### 方式2：手动测试

```bash
# 检查租户服务
curl http://localhost:8081/actuator/health

# 检查充电站服务
curl http://localhost:8082/actuator/health

# 查看服务信息
curl http://localhost:8081/actuator/info
curl http://localhost:8082/actuator/info
```

## 🔍 查看日志

```bash
# 查看所有服务日志
docker compose -f docker-compose.test.yml logs -f

# 查看特定服务日志
docker compose -f docker-compose.test.yml logs -f tenant-service
docker compose -f docker-compose.test.yml logs -f station-service

# 查看最近100行日志
docker compose -f docker-compose.test.yml logs --tail=100 tenant-service
```

## 🛑 停止环境

### Linux/macOS

```bash
# 停止环境（保留数据）
./scripts/stop-test.sh

# 停止环境并删除所有数据
docker compose -f docker-compose.test.yml down -v
```

### Windows

```powershell
# 停止环境（保留数据）
docker compose -f docker-compose.test.yml down

# 停止环境并删除所有数据
docker compose -f docker-compose.test.yml down -v
```

## ❓ 常见问题

### 1. 端口被占用

**错误**: `port is already allocated`

**解决**: 
```bash
# 查看占用端口的进程
sudo lsof -i :8081
# 或
sudo netstat -tulpn | grep 8081

# 停止冲突的服务或修改 docker-compose.test.yml 中的端口
```

### 2. 服务启动失败

**症状**: 容器不断重启

**解决**:
```bash
# 查看失败服务的日志
docker compose -f docker-compose.test.yml logs tenant-service

# 常见原因：
# - 等待数据库就绪（多等待30秒）
# - 内存不足（增加Docker内存限制到8GB）
# - 端口冲突（参考问题1）
```

### 3. 健康检查失败

**症状**: `health-check.sh` 报告失败

**解决**:
```bash
# 1. 等待更长时间（服务启动需要1-2分钟）
sleep 60

# 2. 手动检查服务状态
curl http://localhost:8081/actuator/health
docker compose -f docker-compose.test.yml ps

# 3. 重新部署
docker compose -f docker-compose.test.yml down -v
./scripts/start-test.sh
```

### 4. 数据库连接失败

**症状**: 应用日志显示数据库连接错误

**解决**:
```bash
# 检查PostgreSQL是否正常
docker compose -f docker-compose.test.yml logs postgres
docker exec evcs-postgres-test pg_isready -U evcs_test

# 重启数据库
docker compose -f docker-compose.test.yml restart postgres
```

## 📚 详细文档

- **完整指南**: [测试环境部署指南](docs/TEST-ENVIRONMENT-GUIDE.md)
- **部署总结**: [DEPLOYMENT-TESTING-SUMMARY.md](DEPLOYMENT-TESTING-SUMMARY.md)
- **Docker部署**: [DOCKER-DEPLOYMENT.md](DOCKER-DEPLOYMENT.md)
- **项目主页**: [README.md](README.md)

## 💡 提示

### 开发调试

```bash
# 进入服务容器调试
docker exec -it evcs-tenant-test /bin/sh
docker exec -it evcs-station-test /bin/sh

# 进入数据库
docker exec -it evcs-postgres-test psql -U evcs_test -d evcs_mgr_test

# 进入Redis
docker exec -it evcs-redis-test redis-cli -a test_redis_123
```

### 性能优化

推荐的Docker Desktop设置：
- **CPU**: 4核或以上
- **内存**: 8GB或以上
- **磁盘**: 20GB可用空间

### CI/CD集成

项目包含GitHub Actions工作流，自动执行：
- 构建应用
- 启动测试环境
- 运行健康检查
- 运行冒烟测试
- 收集日志（失败时）

查看: `.github/workflows/test-environment.yml`

---

## 🎯 下一步

环境启动后，你可以：

1. **API测试** - 使用Postman/curl测试API
2. **数据库查看** - 访问 http://localhost:8090
3. **日志分析** - 查看应用日志排查问题
4. **性能测试** - 使用JMeter/k6进行压测
5. **集成测试** - 编写和运行集成测试

---

**准备就绪？开始测试吧！** 🎉

```bash
./scripts/start-test.sh && ./scripts/health-check.sh && ./scripts/smoke-test.sh
```
