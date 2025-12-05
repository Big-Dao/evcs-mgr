# EVCS Manager 快速启动指�? 
> **版本**: v1.1 | **最后更�?*: 2025-11-10 | **维护�?*: DevOps 团队 | **状�?*: 活跃
>
> 🚀 **用�?*: 30 分钟内启动本�?/ 演示环境

## 📋 系统要求

| 环境 | 最低配�?| 推荐配置 |
|------|----------|----------|
| 开发测�?| 2核CPU, 4GB内存, 10GB磁盘 | 4核CPU, 8GB内存, 20GB磁盘 |
| 生产环境 | 4核CPU, 8GB内存, 50GB磁盘 | 8核CPU, 16GB内存, 100GB磁盘 |
| 软件依赖 | Docker 20.10+, Docker Compose 2.0+ | Docker 24.0+, Docker Compose 2.20+ |

## 🎯 一键启�?

### 最小配置（推荐新手�?
```bash
# 快速启动核心服务，适合开发和测试
docker-compose -f docker-compose.minimal.yml up -d
```

### 优化配置（推荐生产）
```bash
# 启动完整服务栈，资源优化版本
docker-compose -f docker-compose.optimized.yml up -d
```

### 自动选择配置
```bash
# 使用智能部署脚本，根据系统资源自动选择最佳配�?
./scripts/deploy/optimized-deploy.sh auto
```

## 🌐 服务访问

启动完成后，可以通过以下地址访问各个服务�?

| 服务 | 地址 | 描述 |
|------|------|------|
| **API网关** | http://localhost:8080 | 主要API入口 |
| **认证服务** | http://localhost:8081 | 用户登录认证 |
| **充电站管�?* | http://localhost:8082 | 充电站和充电桩管�?|
| **订单服务** | http://localhost:8083 | 订单和计费管�?|
| **支付服务** | http://localhost:8084 | 支付处理 |
| **协议服务** | http://localhost:8085 | OCPP协议处理 |
| **租户管理** | http://localhost:8086 | 多租户管�?|
| **监控服务** | http://localhost:8087 | 系统监控 |
| **配置中心** | http://localhost:8888 | 配置管理 |
| **服务注册** | http://localhost:8761 | Eureka控制�?|
| **前端管理** | http://localhost:3000 | 管理界面（完整配置） |
| **数据�?* | localhost:5432 | PostgreSQL |
| **缓存** | localhost:6379 | Redis |

## 🔧 常用命令

### 服务管理
```bash
# 查看服务状�?
docker-compose ps

# 查看服务日志
docker-compose logs -f

# 停止所有服�?
docker-compose down

# 重启服务
docker-compose restart
```

### 资源监控
```bash
# 实时监控资源使用
./scripts/docker/resource-monitor.sh monitor

# 资源使用分析
./scripts/docker/resource-monitor.sh analyze

# 自动优化
./scripts/docker/resource-monitor.sh optimize
```

### 配置切换
```bash
# 切换到最小配�?
./scripts/docker/resource-monitor.sh switch minimal

# 切换到优化配�?
./scripts/docker/resource-monitor.sh switch optimized

# 切换到完整配�?
./scripts/docker/resource-monitor.sh switch full
```

## 📚 详细文档

| 文档 | 路径 | 描述 |
|------|------|------|
| **部署指南** | [DEPLOYMENT-GUIDE.md](DEPLOYMENT-GUIDE.md) | 完整部署文档 |
| **资源优化** | [RESOURCE-OPTIMIZATION-GUIDE.md](../archive/deployment-docs-cleanup-2025-12-05/RESOURCE-OPTIMIZATION-GUIDE.md) | 资源优化详解 |
| **Docker优化** | [DOCKER-OPTIMIZATION.md](../archive/deployment-docs-cleanup-2025-12-05/DOCKER-OPTIMIZATION.md) | Docker镜像优化 |
| **项目结构** | [../operations/PROJECT-STRUCTURE.md](../operations/PROJECT-STRUCTURE.md) | 项目目录说明 |
| **开发指�?* | [../development/DEVELOPER-GUIDE.md](../development/DEVELOPER-GUIDE.md) | 开发环境搭�?|

## 🔍 故障排查

### 常见问题

**Q: 服务启动失败�?*
```bash
# 检查Docker服务
sudo systemctl status docker

# 检查端口占�?
netstat -tulpn | grep :8080

# 清理并重�?
docker-compose down -v
docker-compose up -d
```

**Q: 内存不足�?*
```bash
# 使用最小配�?
./scripts/deploy/optimized-deploy.sh minimal

# 或清理系统资�?
./scripts/docker/resource-monitor.sh clean
```

**Q: 访问不了服务�?*
```bash
# 检查服务状�?
./scripts/docker/resource-monitor.sh health

# 查看端口映射
docker ps --format "table {{.Names}}\t{{.Ports}}"
```

### 获取帮助

```bash
# 查看部署脚本帮助
./scripts/deploy/optimized-deploy.sh --help

# 查看监控脚本帮助
./scripts/docker/resource-monitor.sh --help

# 查看Docker Compose帮助
docker-compose --help
```

## 🎯 默认凭据

| 服务 | 用户�?| 密码 |
|------|--------|------|
| **PostgreSQL** | postgres | postgres |
| **Redis** | - | 无密�?|
| **RabbitMQ** | guest | guest |
| **默认管理�?* | admin | admin123 |

⚠️ **生产环境请修改默认密码！**

## 📈 性能调优

### 快速优�?
```bash
# 自动性能优化
./scripts/docker/resource-monitor.sh optimize

# 查看优化建议
./scripts/docker/resource-monitor.sh analyze
```

### 手动调优
1. 根据系统资源选择合适的配置文件
2. 调整JVM内存参数
3. 优化数据库连接池大小
4. 配置Redis缓存策略

详细调优指南请参考：[资源优化指南](../archive/deployment-docs-cleanup-2025-12-05/RESOURCE-OPTIMIZATION-GUIDE.md)

## 🚀 生产部署

对于生产环境，建议：

1. **使用优化配置**
   ```bash
   ./scripts/deploy/optimized-deploy.sh optimized --backup --monitor
   ```

2. **启用监控**
   ```bash
   docker-compose -f docker-compose.monitoring.yml up -d
   ```

3. **配置反向代理**（Nginx/HAProxy�?

4. **启用SSL证书**

5. **配置日志轮转**

6. **设置备份策略**

详细生产部署指南请参考：[部署指南](DEPLOYMENT-GUIDE.md)

---

## 🤝 贡献

欢迎提交Issue和Pull Request来改进这个项目！

## 📄 许可�?

本项目采�?MIT 许可证。详�?[LICENSE](../../LICENSE) 文件�?

---

**最后更�?*: 2025-11-08
**维护�?*: EVCS Manager 开发团�?
**版本**: v2.0 (资源优化�?

🎉 **感谢使用 EVCS Manager�?*
