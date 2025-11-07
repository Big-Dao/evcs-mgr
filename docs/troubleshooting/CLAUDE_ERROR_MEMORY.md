# Claude 错误记忆库

## 重要：Claude必须记住的错误教训

### 🔴 严重错误记录

#### 1. 服务名不一致错误 (2025-11-06)
**我的错误**: 我反复在配置中使用错误的服务名
- ❌ 使用 `gateway-service` 而不是 `gateway`
- ❌ 使用 `evcs-auth` 而不是 `auth`
- ❌ 使用 `tenant-service` 而不是 `tenant`

**正确的做法**:
- ✅ 严格遵循 docker-compose.local-images.yml 中定义的服务名
- ✅ nginx.conf 中使用 `proxy_pass http://gateway:8080/`
- ✅ Config Server 配置中使用 `lb://auth`

**检查方法**: 修改任何配置前，先检查 `SERVICE_NAMES_MAPPING.md`

#### 2. 配置修改缺乏系统性检查
**我的错误**: 经常修改一个配置文件时，忘记检查相关文件
- 修改 nginx.conf 时忘记检查 docker-compose.yml
- 修改 Config Server 配置时忘记检查 gateway 的 application.yml

**正确的做法**:
- 使用 `grep` 或 `Grep` 工具搜索所有相关配置
- 系统性检查所有相关文件
- 运行 `docker-compose config` 验证配置

### 🟡 需要特别警惕的场景

#### 配置修改场景
当我要修改以下文件时，必须特别小心：
- `evcs-admin/nginx.conf`
- `config-repo/*.yml`
- `docker-compose*.yml`
- `*/application.yml`

#### 服务名相关操作
当我要：
- 配置代理或路由
- 设置服务间通信
- 修改负载均衡配置
- 更新 Eureka 注册

**必须**: 先查看 SERVICE_NAMES_MAPPING.md

### 📋 Claude 强制检查清单

在修改任何配置前，我必须：

1. **检查服务名**
   ```bash
   # 搜索相关服务名使用情况
   grep -r "service-name" .
   ```

2. **参考权威文档**
   - SERVICE_NAMES_MAPPING.md
   - ERROR_PREVENTION_CHECKLIST.md

3. **系统性验证**
   ```bash
   docker-compose config
   docker-compose logs service-name
   ```

### 🔄 错误修正模式

当我发现配置错误时：
1. ❌ 不要只修复一个文件
2. ✅ 要搜索所有相关文件并统一修复
3. ✅ 要记录到这个记忆库
4. ✅ 要分析根本原因

### 💡 给Claude的提醒

**重要提醒**:
- 我在这个项目中犯过服务名不一致的错误多次
- 我必须建立系统性检查习惯
- 我要依赖工具和文档而不是记忆

**当用户指出我犯了重复错误时**：
- 立即停止当前操作
- 查看这个记忆库
- 分析为什么又犯了同样的错误
- 改进我的检查流程

---

*最后更新: 2025-11-06*
*目的: 帮助Claude记住错误教训，避免重复*