#!/usr/bin/env pwsh
# EVCS Manager - 本地构建后打包 Docker 镜像
# 策略：在宿主机构建 JAR → 使用简单 Dockerfile 打包 → 加载到本地 Docker

param(
    [string[]]$Services = @(),
    [switch]$SkipBuild,
    [string]$ImageTag = "latest"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " EVCS Manager - 本地构建 Docker 镜像" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 定义所有服务
$allServices = @(
    @{Name="eureka"; Module="evcs-eureka"; Port=8761},
    @{Name="config"; Module="evcs-config"; Port=8888},
    @{Name="gateway"; Module="evcs-gateway"; Port=8080},
    @{Name="auth"; Module="evcs-auth"; Port=8081},
    @{Name="tenant"; Module="evcs-tenant"; Port=8083},
    @{Name="station"; Module="evcs-station"; Port=8082},
    @{Name="order"; Module="evcs-order"; Port=8084},
    @{Name="payment"; Module="evcs-payment"; Port=8085},
    @{Name="protocol"; Module="evcs-protocol"; Port=8086},
    @{Name="monitoring"; Module="evcs-monitoring"; Port=9090}
)

# 如果指定了特定服务，只处理这些服务
if ($Services.Count -gt 0) {
    $allServices = $allServices | Where-Object { $Services -contains $_.Name }
}

Write-Host "配置信息:" -ForegroundColor Yellow
Write-Host "  镜像标签: $ImageTag" -ForegroundColor White
Write-Host "  服务数量: $($allServices.Count)" -ForegroundColor White
Write-Host ""

# 检查 Docker
Write-Host "[1/3] 检查 Docker 环境..." -ForegroundColor Yellow
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Docker 未运行！" -ForegroundColor Red
    exit 1
}
Write-Host "  ✅ Docker 运行正常" -ForegroundColor Green
Write-Host ""

# 构建 JAR 文件
if (-not $SkipBuild) {
    Write-Host "[2/3] 在本地构建 JAR 文件..." -ForegroundColor Yellow
    Write-Host "  正在执行 Gradle 构建（跳过测试）..." -ForegroundColor Gray
    
    $buildCmd = ".\gradlew clean build -x test --no-daemon"
    Write-Host "  命令: $buildCmd" -ForegroundColor Gray
    
    Invoke-Expression $buildCmd
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ❌ Gradle 构建失败！" -ForegroundColor Red
        exit 1
    }
    Write-Host "  ✅ JAR 文件构建成功" -ForegroundColor Green
    
    # 验证 JAR 文件
    Write-Host "  验证 JAR 文件..." -ForegroundColor Gray
    foreach ($service in $allServices) {
        $jarPath = "$($service.Module)/build/libs/*.jar"
        $jars = Get-ChildItem -Path $jarPath -ErrorAction SilentlyContinue
        if ($jars) {
            Write-Host "    ✓ $($service.Module): $($jars[0].Name)" -ForegroundColor Green
        } else {
            Write-Host "    ✗ $($service.Module): JAR 文件未找到" -ForegroundColor Red
        }
    }
    Write-Host ""
} else {
    Write-Host "[2/3] 跳过 JAR 构建..." -ForegroundColor Gray
    Write-Host ""
}

# 构建 Docker 镜像
Write-Host "[3/3] 构建 Docker 镜像..." -ForegroundColor Yellow
Write-Host ""

$results = @()
$successCount = 0
$failCount = 0

foreach ($service in $allServices) {
    $serviceName = $service.Name
    $moduleName = $service.Module
    $imageName = "evcs-${serviceName}:${ImageTag}"
    
    Write-Host "  处理服务: $serviceName (端口 $($service.Port))" -ForegroundColor Cyan
    
    # 检查 JAR 文件是否存在
    $jarFiles = Get-ChildItem -Path "$moduleName/build/libs/*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike "*-plain.jar" -and $_.Name -notlike "*-sources.jar" }
    
    if (-not $jarFiles -or $jarFiles.Count -eq 0) {
        Write-Host "    ❌ JAR 文件不存在: $moduleName/build/libs/" -ForegroundColor Red
        $results += @{Service=$serviceName; Status="失败"; Reason="JAR文件不存在"}
        $failCount++
        continue
    }
    
    $jarFile = $jarFiles[0]
    Write-Host "    📦 JAR: $($jarFile.Name)" -ForegroundColor Gray
    
    # 创建临时 Dockerfile
    $tempDockerfile = "$moduleName/Dockerfile.simple"
    $dockerfileContent = @"
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl && rm -rf /var/cache/apk/*
COPY build/libs/$($jarFile.Name) app.jar
RUN addgroup -S spring && adduser -S spring -G spring && chown spring:spring app.jar
USER spring:spring
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE $($service.Port)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail http://localhost:$($service.Port)/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java `$JAVA_OPTS -jar app.jar"]
"@
    
    $dockerfileContent | Out-File -FilePath $tempDockerfile -Encoding UTF8 -NoNewline
    
    # 构建镜像
    Write-Host "    🔨 构建镜像..." -ForegroundColor Gray
    docker build -t $imageName -f $tempDockerfile $moduleName 2>&1 | Out-Null
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "    ❌ 构建失败" -ForegroundColor Red
        $results += @{Service=$serviceName; Status="失败"; Reason="Docker构建失败"}
        $failCount++
        continue
    }
    
    Write-Host "    ✅ 镜像构建成功: $imageName" -ForegroundColor Green
    $results += @{Service=$serviceName; Status="成功"; Image=$imageName; Port=$service.Port}
    $successCount++
    Write-Host ""
}

# 打印汇总结果
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " 构建结果汇总" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

foreach ($result in $results) {
    $service = $result.Service
    $status = $result.Status
    
    if ($status -eq "成功") {
        Write-Host "✅ $service - 成功" -ForegroundColor Green
        Write-Host "   镜像: $($result.Image)" -ForegroundColor Gray
    } else {
        Write-Host "❌ $service - 失败 ($($result.Reason))" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "总计: $($results.Count) 个服务" -ForegroundColor Cyan
Write-Host "成功: $successCount" -ForegroundColor Green
Write-Host "失败: $failCount" -ForegroundColor Red
Write-Host ""

# 生成本地部署 compose 文件
if ($successCount -gt 0) {
    Write-Host "生成 docker-compose.local-images.yml 部署文件..." -ForegroundColor Yellow
    
    $composeContent = @"
version: '3.8'

# 使用本地构建镜像的部署配置
# 镜像: evcs-*:${ImageTag}

x-java-env: &java-env
  SPRING_PROFILES_ACTIVE: local
  EUREKA_SERVER_URL: http://eureka:8761/eureka/
  DB_HOST: postgres
  DB_PORT: 5432
  DB_NAME: evcs_mgr
  DB_USER: postgres
  DB_PASSWORD: postgres
  SPRING_DATA_REDIS_HOST: redis
  SPRING_DATA_REDIS_PORT: 6379
  SPRING_RABBITMQ_HOST: rabbitmq
  SPRING_RABBITMQ_PORT: 5672
  SPRING_RABBITMQ_USERNAME: guest
  SPRING_RABBITMQ_PASSWORD: guest

services:
  postgres:
    image: postgres:17-alpine
    container_name: evcs-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: evcs_mgr
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./sql/init.sql:/docker-entrypoint-initdb.d/01-init.sql
      - ./sql/charging_station_tables.sql:/docker-entrypoint-initdb.d/02-station.sql
      - ./sql/evcs_order_tables.sql:/docker-entrypoint-initdb.d/03-order.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - evcs-network

  redis:
    image: redis:7-alpine
    container_name: evcs-redis
    restart: unless-stopped
    command: redis-server --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks:
      - evcs-network

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: evcs-rabbitmq
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - evcs-network

"@

    foreach ($result in $results | Where-Object { $_.Status -eq "成功" }) {
        $serviceName = $result.Service
        
        $composeContent += @"

  ${serviceName}:
    image: $($result.Image)
    container_name: evcs-${serviceName}
    restart: unless-stopped
    ports:
      - "$($result.Port):$($result.Port)"
    environment:
      <<: *java-env
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:$($result.Port)/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - evcs-network
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
"@
    }

    $composeContent += @"

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:

networks:
  evcs-network:
    driver: bridge
"@

    $composeContent | Out-File -FilePath "docker-compose.local-images.yml" -Encoding UTF8
    Write-Host "  ✅ 已生成: docker-compose.local-images.yml" -ForegroundColor Green
    Write-Host ""
}

# 显示下一步操作
if ($successCount -gt 0) {
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host " 下一步操作" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "🚀 启动所有服务:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f docker-compose.local-images.yml up -d" -ForegroundColor White
    Write-Host ""
    
    Write-Host "📊 查看服务状态:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f docker-compose.local-images.yml ps" -ForegroundColor White
    Write-Host ""
    
    Write-Host "📋 查看服务日志:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f docker-compose.local-images.yml logs -f [服务名]" -ForegroundColor White
    Write-Host ""
    
    Write-Host "🛑 停止所有服务:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f docker-compose.local-images.yml down" -ForegroundColor White
    Write-Host ""
    
    Write-Host "🔍 已构建的镜像:" -ForegroundColor Yellow
    foreach ($result in $results | Where-Object { $_.Status -eq "成功" }) {
        Write-Host "  $($result.Image)" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($failCount -eq 0) {
    Write-Host "🎉 所有服务镜像构建成功！" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️  部分服务失败，请检查错误信息" -ForegroundColor Yellow
    exit 1
}
