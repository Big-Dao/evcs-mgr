# Docker 构建依赖问题修复

## 问题描述

在 CI/CD 环境中进行 Docker 构建时，`evcs-station` 模块编译失败，提示找不到 `com.evcs.protocol.api` 包：

```
> Task :evcs-station:compileJava FAILED
error: package com.evcs.protocol.api does not exist
import com.evcs.protocol.api.ICloudChargeProtocolService;
                            ^
```

## 根本原因

Docker 多阶段构建中，Gradle 任务 `./gradlew :evcs-station:bootJar` 只构建目标模块，但 **没有先构建它依赖的模块**（`evcs-common` 和 `evcs-protocol`）。在 Docker 构建环境中：

1. `evcs-protocol:compileJava` 显示 `NO-SOURCE`（无源码）
2. `evcs-protocol:jar` 生成了空的 jar 文件
3. `evcs-station:compileJava` 无法找到 protocol 的 API 类

### 为什么本地构建不报错？

本地 Gradle 构建时，`.gradle/` 缓存中已经有之前构建的 jar 文件，因此 `evcs-station` 可以找到依赖。但在 Docker 全新环境中没有缓存，必须显式构建依赖模块。

## 解决方案

### 修复策略

在所有服务的 Dockerfile 中，**先构建依赖模块的 jar，再构建目标服务**：

```dockerfile
# Before (错误)
RUN ./gradlew :evcs-station:bootJar --no-daemon

# After (正确)
# Build dependencies first
RUN ./gradlew :evcs-common:jar :evcs-protocol:jar --no-daemon
# Then build the target service
RUN ./gradlew :evcs-station:bootJar --no-daemon
```

### 依赖关系映射

| 服务模块 | 依赖模块 | Dockerfile 修复 |
|---------|---------|----------------|
| evcs-station | evcs-common + evcs-protocol | ✅ 添加两个依赖 |
| evcs-order | evcs-common + evcs-protocol | ✅ 添加两个依赖 |
| evcs-tenant | evcs-common | ✅ 添加一个依赖 |
| evcs-payment | evcs-common | ✅ 添加一个依赖 |
| evcs-gateway | evcs-common | ✅ 添加一个依赖 |
| evcs-auth | evcs-common | ✅ 添加一个依赖 |
| evcs-monitoring | evcs-common | ✅ 添加一个依赖 |
| evcs-protocol | evcs-common | ✅ 添加一个依赖 |
| evcs-config | evcs-common | ✅ 添加一个依赖 |
| evcs-eureka | 无 | ✅ 无需修改 |

### 已修复的文件

```
evcs-station/Dockerfile
evcs-order/Dockerfile
evcs-tenant/Dockerfile
evcs-payment/Dockerfile
evcs-gateway/Dockerfile
evcs-auth/Dockerfile
evcs-monitoring/Dockerfile
evcs-protocol/Dockerfile
evcs-config/Dockerfile
evcs-eureka/Dockerfile (仅添加注释)
```

## 验证步骤

### 1. 本地 Gradle 验证

```powershell
# 清理构建缓存
.\gradlew clean

# 完整构建
.\gradlew build -x test
```

**预期结果**：所有任务成功，无编译错误。

### 2. Docker 构建验证

```powershell
# 测试单个服务
docker build -t evcs-station:test -f evcs-station/Dockerfile .

# 测试所有服务
docker-compose -f docker-compose.test.yml build
```

**预期结果**：Docker 镜像构建成功，无编译错误。

### 3. CI/CD 验证

推送代码到 GitHub 触发 `.github/workflows/test-environment.yml` 中的 `build-docker-images` 作业：

```yaml
- name: Build station service image
  uses: docker/build-push-action@v5
  with:
    context: .
    file: ./evcs-station/Dockerfile
    push: false
    tags: evcs-station:test
```

**预期结果**：GitHub Actions 构建成功。

## 技术细节

### Gradle 任务依赖

当运行 `./gradlew :evcs-station:bootJar` 时，Gradle **不会自动构建** `implementation project(':evcs-protocol')` 依赖的 jar 文件，而是：

1. 尝试从 `evcs-protocol/build/libs/` 查找已存在的 jar
2. 如果找不到，尝试从 `evcs-protocol/build/classes/` 使用编译后的 class 文件
3. 都找不到时编译失败

### evcs-protocol 配置要点

`evcs-protocol/build.gradle` 必须包含 `java-library` 插件才能正确暴露 API：

```gradle
plugins {
    id 'java-library'
    id 'org.springframework.boot' version '3.2.2'
    id 'io.spring.dependency-management' version '1.1.4'
}

jar {
    enabled = true
    archiveClassifier = ''
}

bootJar {
    archiveClassifier = 'boot'
}
```

- `java-library`：允许其他模块依赖此模块的 API
- `jar.enabled = true`：生成标准 jar（给依赖方使用）
- `bootJar`：生成可执行的 Spring Boot jar（独立运行）

## 最佳实践

### Dockerfile 构建模式

```dockerfile
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

# Step 1: Build shared dependencies
RUN ./gradlew :evcs-common:jar --no-daemon

# Step 2: Build protocol layer (if needed)
RUN ./gradlew :evcs-protocol:jar --no-daemon

# Step 3: Build the target service
RUN ./gradlew :evcs-station:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
# ... runtime configuration
```

### 优化构建时间

可以合并依赖构建：

```dockerfile
# 单行构建多个依赖
RUN ./gradlew :evcs-common:jar :evcs-protocol:jar --no-daemon --parallel
```

### 避免的错误模式

❌ **错误**：只构建目标模块
```dockerfile
RUN ./gradlew :evcs-station:bootJar --no-daemon
```

❌ **错误**：依赖 Gradle 自动依赖解析
```dockerfile
# Gradle 不会自动构建 project() 依赖的 jar
RUN ./gradlew :evcs-station:build --no-daemon
```

✅ **正确**：显式构建依赖链
```dockerfile
RUN ./gradlew :evcs-common:jar :evcs-protocol:jar --no-daemon
RUN ./gradlew :evcs-station:bootJar --no-daemon
```

## 相关问题排查

### 问题 1：本地能构建，Docker 不能

**症状**：本地 `./gradlew build` 成功，Docker 构建失败。

**原因**：本地 `.gradle/` 缓存了依赖 jar。

**解决**：`./gradlew clean` 后重新构建，会复现 Docker 错误。

### 问题 2：compileJava NO-SOURCE

**症状**：Docker 日志显示 `> Task :evcs-protocol:compileJava NO-SOURCE`。

**原因**：Gradle 认为没有源文件需要编译（实际有源文件）。

**解决**：检查 Dockerfile 的 `COPY . .` 是否正确复制了源码。

### 问题 3：jar 文件为空

**症状**：`evcs-protocol.jar` 存在但内容为空或只有 MANIFEST.MF。

**原因**：未启用 `java-library` 插件或 `jar.enabled = false`。

**解决**：按照上述 "evcs-protocol 配置要点" 修复 `build.gradle`。

## 监控与验证

### 构建日志关键指标

✅ **成功标志**：
```
> Task :evcs-common:compileJava
> Task :evcs-common:jar
> Task :evcs-protocol:compileJava
> Task :evcs-protocol:jar
> Task :evcs-station:compileJava
> Task :evcs-station:bootJar
BUILD SUCCESSFUL
```

❌ **失败标志**：
```
> Task :evcs-protocol:compileJava NO-SOURCE
> Task :evcs-station:compileJava FAILED
error: package com.evcs.protocol.api does not exist
```

## 示例修复

### evcs-station/Dockerfile

```dockerfile
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
# Build dependencies first (evcs-common and evcs-protocol)
RUN ./gradlew :evcs-common:jar :evcs-protocol:jar --no-daemon
# Then build the station service
RUN ./gradlew :evcs-station:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /workspace/evcs-station/build/libs/evcs-station-*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
ENV JAVA_OPTS=""
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD curl --fail http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
```

## 文档索引

- **主文档**：`DOCUMENTATION-INDEX.md` > 问题修复 > Docker 构建依赖问题
- **相关文档**：
  - `docs/deployment/DOCKER.md` - Docker 部署指南
  - `docs/DEVELOPER-GUIDE.md` - 开发者指南
  - `.github/workflows/test-environment.yml` - CI/CD 配置

## 更新历史

- 2025-01-XX：初始文档 - 修复 `evcs-protocol/build.gradle` 缺失 java-library 插件
- 2025-01-XX：完整修复 - 更新所有服务 Dockerfile 的依赖构建顺序
