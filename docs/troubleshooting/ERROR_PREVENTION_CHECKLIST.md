# 错误预防检查清单

> **版本**: v1.0 | **更新日期**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 活跃

## 常见错误类型和解决方案

### 1. 服务名称不一致错误 ⚠️
**错误模式**: 配置文件中服务名不统一
- `gateway-service` vs `gateway`
- `evcs-auth` vs `auth-service`

**检查点**:
- [ ] Docker Compose中的服务名
- [ ] Nginx配置中的proxy_pass
- [ ] Spring Cloud Gateway配置中的uri
- [ ] Config Server配置中的服务名
- [ ] application.yml中的Eureka注册名

**解决方案**:
- 建立服务名映射表
- 统一使用简短服务名（如 `gateway`, `auth`, `tenant` 等）
- 在修改配置时检查所有相关文件

### 2. 端口配置错误
**错误模式**: 服务端口配置不匹配
- 内部端口 vs 外部暴露端口
- Spring Boot端口 vs Docker端口映射

**检查点**:
- [ ] docker-compose.yml中的ports映射
- [ ] application.yml中的server.port
- [ ] nginx.conf中的proxy_pass端口

### 3. 配置优先级错误
**错误模式**: 本地配置覆盖Config Server配置
- application.yml vs Config Server配置
- 环境变量 vs 配置文件

**检查点**:
- [ ] Spring配置导入顺序
- [ ] @Profile注解的使用
- [ ] 环境变量设置

## 修改前检查步骤

1. **查找相关配置**: 使用 `grep` 搜索所有相关文件
2. **确认服务名**: 检查docker-compose.yml中的服务名定义
3. **验证端口映射**: 确认内外端口一致性
4. **检查配置优先级**: 确认配置加载顺序

## 提交前验证步骤

1. **运行配置测试**: `docker-compose config`
2. **检查服务连通性**: `docker-compose exec ... curl`
3. **查看日志**: `docker-compose logs [service-name]`
4. **Git diff检查**: 仔细检查所有配置文件变更

## 错误修复记录

### 2025-11-06: Gateway服务名不一致
- **问题**: nginx.conf中使用`gateway-service`，但docker-compose中定义为`gateway`
- **修复**: 统一使用`gateway`作为服务名
- **涉及文件**: evcs-admin/nginx.conf, docker-compose.yml