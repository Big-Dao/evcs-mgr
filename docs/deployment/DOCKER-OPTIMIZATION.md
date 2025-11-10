# Docker 镜像优化指南

> **版本**: v1.1 | **最后更新**: 2025-11-10 | **维护者**: DevOps 团队 | **状态**: 活跃
>
> 🐳 **用途**: 记录多阶段构建、缓存策略与性能对比

本文档详细说明了 EVCS Manager 项目中实施的 Docker 多阶段构建优化方案。

## 目录

- [优化概述](#优化概述)
- [Java 微服务优化](#java-微服务优化)
- [前端服务优化](#前端服务优化)
- [构建缓存策略](#构建缓存策略)
- [使用指南](#使用指南)
- [性能对比](#性能对比)
- [最佳实践](#最佳实践)

## 优化概述

### 实施的优化措施

1. **三阶段构建模式** - Java 微服务采用依赖下载、编译构建、运行时三个独立阶段
2. **精确的文件复制** - 仅复制必要的构建文件，最大化缓存命中率
3. **前端多阶段构建** - Node.js 构建层 + Nginx 运行层，大幅减小镜像体积
4. **优化的 .dockerignore** - 排除所有不必要的文件，加快构建上下文传输
5. **统一 JVM 参数** - 容器环境优化的 JVM 配置
6. **非 root 用户运行** - 所有服务以非特权用户运行，提升安全性
7. **CI/CD 缓存集成** - GitHub Actions 启用 Docker BuildKit 缓存

### 预期效果

- ✅ **镜像体积减少**: 15%-30%（前端服务可达 50%+）
- ✅ **增量构建时间**: 从 8-10 分钟降至 2-3 分钟
- ✅ **缓存命中率**: 依赖层缓存命中率 > 90%
- ✅ **安全性提升**: 所有服务非 root 运行，最小化依赖

## Java 微服务优化

### 优化后的 Dockerfile 结构

所有 Java 微服务（evcs-auth、evcs-config、evcs-eureka、evcs-gateway、evcs-monitoring、evcs-order、evcs-payment、evcs-protocol、evcs-station、evcs-tenant）均采用以下三阶段结构：

```dockerfile
# ============================================================
# Stage 1: 下载依赖（高缓存命中率）
# ============================================================
FROM gradle:8.5-jdk21-alpine AS dependencies
WORKDIR /workspace

# 仅复制依赖配置文件
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 下载所有依赖到缓存层
RUN ./gradlew dependencies --no-daemon || true

# ============================================================
# Stage 2: 编译构建（仅在源码变更时重新构建）
# ============================================================
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /workspace

# 复用依赖缓存
COPY --from=dependencies /root/.gradle /root/.gradle

# 复制依赖配置
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 复制必要的源代码（仅 evcs-common 和当前模块）
COPY evcs-common ./evcs-common
COPY evcs-[module] ./evcs-[module]

# 构建当前模块
RUN ./gradlew :evcs-[module]:bootJar --no-daemon --parallel

# ============================================================
# Stage 3: 运行时（最小化镜像）
# ============================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 仅安装必要工具（健康检查）
RUN apk add --no-cache curl && rm -rf /var/cache/apk/*

# 仅复制构建产物
COPY --from=build /workspace/evcs-[module]/build/libs/*.jar app.jar

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring && \
    chown spring:spring app.jar
USER spring:spring

# 容器优化的 JVM 参数
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE [port]
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail http://localhost:[port]/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 关键优化点

1. **依赖层分离**
   - Stage 1 仅复制 `build.gradle`、`settings.gradle`、`gradlew` 和 `gradle/` 目录
   - 除非这些文件变更，否则 Docker 会使用缓存层
   - 避免了每次源码修改都重新下载依赖的问题

2. **选择性源码复制**
   - 仅复制 `evcs-common`（共享依赖）和当前模块源码
   - 不复制其他无关模块，减少构建上下文

3. **JVM 容器优化**
   ```
   -XX:+UseContainerSupport       # 自动检测容器内存限制
   -XX:MaxRAMPercentage=75.0      # 使用 75% 容器内存
   -XX:InitialRAMPercentage=50.0  # 初始堆为 50% 容器内存
   -XX:+UseG1GC                   # G1 垃圾回收器
   -XX:+OptimizeStringConcat      # 字符串拼接优化
   ```

4. **安全加固**
   - 使用 JRE alpine（而非 JDK），减小镜像体积
   - 创建非特权用户 `spring:spring`
   - 仅安装必要的工具（curl 用于健康检查）

## 前端服务优化

### evcs-admin 多阶段构建

```dockerfile
# ============================================================
# Stage 1: Node.js 构建层
# ============================================================
FROM node:20-alpine AS build
WORKDIR /app

# 复制依赖配置（利用 npm 缓存）
COPY package.json package-lock.json* ./
RUN npm ci --only=production || npm install

# 复制源码并构建
COPY . .
RUN npm run build

# ============================================================
# Stage 2: Nginx 运行层（极小镜像）
# ============================================================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# 清理默认内容
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# 复制配置和构建产物
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist .

# 非 root 用户运行
RUN addgroup -g 101 -S nginx-group && \
    adduser -S -D -H -u 101 -h /var/cache/nginx -s /sbin/nologin \
        -G nginx-group -g nginx-group nginx-user && \
    chown -R nginx-user:nginx-group /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

USER nginx-user

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
```

### 优化效果

| 指标 | 优化前 | 优化后 | 改善 |
|------|--------|--------|------|
| 镜像大小 | ~150MB | ~40MB | **-73%** |
| 构建时间（首次） | ~5 分钟 | ~3 分钟 | **-40%** |
| 构建时间（增量） | ~4 分钟 | ~30 秒 | **-87%** |
| 安全性 | root 用户 | 非 root | ✅ 提升 |

## 构建缓存策略

### .dockerignore 优化

排除所有不必要的文件，减少构建上下文大小：

```dockerignore
# Git 相关
.git
.gitignore
.github/

# 构建产物
.gradle/
build/
*/build/
node_modules/
dist/

# 文档和测试
*.md
docs/
**/test-results/
coverage/

# IDE 和临时文件
.idea/
.vscode/
*.log
*.tmp
```

### Docker Compose 缓存卷

```yaml
volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:
  gradle_cache:  # 共享 Gradle 缓存，加速重复构建
```

### GitHub Actions 缓存配置

```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3

- name: Cache Gradle packages
  uses: actions/cache@v3
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

- name: Build Docker image
  uses: docker/build-push-action@v5
  with:
    cache-from: type=gha,scope=${{ matrix.service }}
    cache-to: type=gha,mode=max,scope=${{ matrix.service }}
```

## 使用指南

### 本地开发构建

```bash
# 构建单个服务
docker build -t evcs-auth:latest -f evcs-auth/Dockerfile .

# 使用 docker-compose 构建所有服务
docker-compose build

# 使用 BuildKit 加速（推荐）
DOCKER_BUILDKIT=1 docker-compose build

# 并行构建多个服务
docker-compose build --parallel
```

### 查看镜像大小

```bash
# 查看所有 EVCS 镜像
docker images | grep evcs

# 对比优化前后大小
docker history evcs-auth:latest
```

### 清理构建缓存

```bash
# 清理悬空镜像和构建缓存
docker system prune -f

# 清理所有未使用的镜像（谨慎使用）
docker system prune -a --volumes
```

### CI/CD 中使用

GitHub Actions 会自动：
1. 缓存 Gradle 依赖包
2. 使用 Docker BuildKit 缓存层
3. 并行构建所有服务镜像
4. 验证镜像大小和健康检查

## 性能对比

### 构建时间对比（基于 10 个微服务）

| 场景 | 优化前 | 优化后 | 改善 |
|------|--------|--------|------|
| **首次全量构建** | 15-20 分钟 | 10-12 分钟 | **-40%** |
| **修改单个服务源码** | 8-10 分钟 | 2-3 分钟 | **-70%** |
| **仅修改配置文件** | 8-10 分钟 | 1-2 分钟 | **-80%** |
| **CI/CD 增量构建** | 10-15 分钟 | 3-5 分钟 | **-65%** |

### 镜像大小对比

| 服务 | 优化前 | 优化后 | 减少 |
|------|--------|--------|------|
| evcs-auth | ~350MB | ~250MB | **-28%** |
| evcs-gateway | ~360MB | ~255MB | **-29%** |
| evcs-order | ~340MB | ~245MB | **-28%** |
| evcs-admin（前端） | ~150MB | ~40MB | **-73%** |
| **总计（11 个服务）** | ~3.6GB | ~2.5GB | **-30%** |

## 最佳实践

### ✅ 推荐做法

1. **使用 BuildKit**
   ```bash
   export DOCKER_BUILDKIT=1
   ```

2. **定期清理缓存**
   ```bash
   # 每周清理一次
   docker system prune -f
   ```

3. **监控镜像大小**
   ```bash
   # 使用 dive 工具分析镜像层
   dive evcs-auth:latest
   ```

4. **验证健康检查**
   ```bash
   docker inspect --format='{{.State.Health.Status}}' evcs-auth
   ```

### ❌ 避免做法

1. **不要复制整个项目**
   ```dockerfile
   # ❌ 错误
   COPY . .
   
   # ✅ 正确
   COPY build.gradle settings.gradle ./
   COPY evcs-common ./evcs-common
   COPY evcs-auth ./evcs-auth
   ```

2. **不要跳过健康检查**
   - 所有服务必须配置 HEALTHCHECK
   - 确保 actuator/health 端点可访问

3. **不要使用 root 用户**
   - 始终创建非特权用户运行应用

4. **不要忽略 .dockerignore**
   - 排除所有不必要的文件
   - 减少构建上下文传输时间

## 故障排查

### 构建缓存未命中

**症状**: 每次构建都重新下载依赖

**解决方案**:
```bash
# 确认 BuildKit 已启用
export DOCKER_BUILDKIT=1

# 查看构建缓存
docker buildx du

# 手动清理并重建
docker builder prune -f
docker-compose build --no-cache
```

### 镜像体积过大

**诊断**:
```bash
# 使用 dive 分析镜像层
docker run --rm -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  wagoodman/dive:latest evcs-auth:latest
```

**优化建议**:
- 合并 RUN 命令减少层数
- 清理 apk 缓存: `rm -rf /var/cache/apk/*`
- 使用 alpine 基础镜像

### 健康检查失败

**症状**: 容器启动后一直 unhealthy

**检查步骤**:
```bash
# 查看健康检查日志
docker inspect --format='{{json .State.Health}}' evcs-auth | jq

# 手动测试健康端点
docker exec evcs-auth curl -f http://localhost:8081/actuator/health

# 增加启动等待时间
HEALTHCHECK --start-period=90s ...
```

## 参考资源

- [Docker 多阶段构建官方文档](https://docs.docker.com/develop/develop-images/multistage-build/)
- [Docker BuildKit 缓存指南](https://docs.docker.com/build/cache/)
- [Gradle Docker 最佳实践](https://docs.gradle.org/current/userguide/docker.html)
- [Spring Boot Docker 优化](https://spring.io/guides/topicals/spring-boot-docker/)

---

**最后更新**: 2025-10-28  
**维护者**: EVCS Manager 团队  
**版本**: v1.0
