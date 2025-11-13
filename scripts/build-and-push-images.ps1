#!/usr/bin/env pwsh
# EVCS Manager - 本地构建镜像并推送到 Docker Hub
# 用途：在本地构建所有服务镜像，然后推送到 Docker Hub

param(
    [string]$DockerRegistry = "docker.io",
    [string]$DockerNamespace = "",  # 你的 Docker Hub 用户名
    [string]$ImageTag = "latest",
    [switch]$SkipBuild,
    [switch]$SkipPush,
    [string[]]$Services = @()
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " EVCS Manager - 镜像构建与推送" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 如果未指定命名空间，提示用户输入
if ([string]::IsNullOrEmpty($DockerNamespace)) {
    $DockerNamespace = Read-Host "请输入 Docker Hub 用户名"
    if ([string]::IsNullOrEmpty($DockerNamespace)) {
        Write-Host "错误: 必须提供 Docker Hub 用户名" -ForegroundColor Red
        exit 1
    }
}

# 定义所有服务
$allServices = @(
    @{Name="eureka"; Port=8761; Description="服务注册中心"},
    @{Name="config-server"; Port=8888; Description="配置中心"},
    @{Name="gateway"; Port=8080; Description="API网关"},
    @{Name="auth-service"; Port=8081; Description="认证服务"},
    @{Name="tenant-service"; Port=8083; Description="租户服务"},
    @{Name="station-service"; Port=8082; Description="充电站服务"},
    @{Name="order-service"; Port=8084; Description="订单服务"},
    @{Name="payment-service"; Port=8085; Description="支付服务"},
    @{Name="protocol-service"; Port=8086; Description="协议服务"},
    @{Name="monitoring-service"; Port=9090; Description="监控服务"}
)

# 如果指定了特定服务，只处理这些服务
if ($Services.Count -gt 0) {
    $allServices = $allServices | Where-Object { $Services -contains $_.Name }
}

Write-Host "配置信息:" -ForegroundColor Yellow
Write-Host "  Docker Registry: $DockerRegistry" -ForegroundColor White
Write-Host "  命名空间: $DockerNamespace" -ForegroundColor White
Write-Host "  镜像标签: $ImageTag" -ForegroundColor White
Write-Host "  服务数量: $($allServices.Count)" -ForegroundColor White
Write-Host ""

# 检查 Docker
Write-Host "[1/4] 检查 Docker 环境..." -ForegroundColor Yellow
$dockerInfo = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Docker 未运行！" -ForegroundColor Red
    exit 1
}
Write-Host "  ✅ Docker 运行正常" -ForegroundColor Green
Write-Host ""

# 检查 Docker 登录状态
if (-not $SkipPush) {
    Write-Host "[2/4] 检查 Docker Hub 登录状态..." -ForegroundColor Yellow
    $loginCheck = docker info 2>&1 | Select-String $DockerNamespace
    if (-not $loginCheck) {
        Write-Host "  ⚠️  未检测到登录状态，尝试登录..." -ForegroundColor Yellow
        docker login $DockerRegistry
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  ❌ Docker Hub 登录失败！" -ForegroundColor Red
            exit 1
        }
    }
    Write-Host "  ✅ 已登录 Docker Hub" -ForegroundColor Green
    Write-Host ""
}

# 构建 JAR 文件
if (-not $SkipBuild) {
    Write-Host "[3/4] 构建应用 JAR 文件..." -ForegroundColor Yellow
    Write-Host "  正在执行 Gradle 构建..." -ForegroundColor Gray
    
    $buildOutput = .\gradlew build -x test --no-daemon 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ❌ Gradle 构建失败！" -ForegroundColor Red
        Write-Host $buildOutput -ForegroundColor Red
        exit 1
    }
    Write-Host "  ✅ JAR 文件构建成功" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "[3/4] 跳过 JAR 构建..." -ForegroundColor Gray
    Write-Host ""
}

# 构建和推送镜像
Write-Host "[4/4] 构建并推送 Docker 镜像..." -ForegroundColor Yellow
Write-Host ""

$results = @()
$successCount = 0
$failCount = 0

foreach ($service in $allServices) {
    $serviceName = $service.Name
    $imageNameLocal = "evcs-${serviceName}:${ImageTag}"
    $imageNameRemote = "${DockerRegistry}/${DockerNamespace}/evcs-${serviceName}:${ImageTag}"
    
    Write-Host "  处理服务: $serviceName ($($service.Description))" -ForegroundColor Cyan
    
    # 检查 Dockerfile 是否存在
    $moduleName = switch ($serviceName) {
        "config-server" { "evcs-config" }
        "auth-service" { "evcs-auth" }
        "tenant-service" { "evcs-tenant" }
        "station-service" { "evcs-station" }
        "order-service" { "evcs-order" }
        "payment-service" { "evcs-payment" }
        "protocol-service" { "evcs-protocol" }
        "monitoring-service" { "evcs-monitoring" }
        "gateway" { "evcs-gateway" }
        default { "evcs-$serviceName" }
    }
    
    $dockerfile = "$moduleName/Dockerfile"
    if (-not (Test-Path $dockerfile)) {
        Write-Host "    ⚠️  Dockerfile 不存在，跳过: $dockerfile" -ForegroundColor Yellow
        $results += @{Service=$serviceName; Status="跳过"; Reason="Dockerfile不存在"}
        continue
    }
    
    # 构建镜像
    Write-Host "    🔨 构建镜像..." -ForegroundColor Gray
    docker build -t $imageNameLocal -f $dockerfile . 2>&1 | Out-Null
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "    ❌ 构建失败" -ForegroundColor Red
        $results += @{Service=$serviceName; Status="失败"; Reason="构建失败"}
        $failCount++
        continue
    }
    
    Write-Host "    ✅ 镜像构建成功: $imageNameLocal" -ForegroundColor Green
    
    # 标记镜像
    docker tag $imageNameLocal $imageNameRemote
    
    # 推送镜像
    if (-not $SkipPush) {
        Write-Host "    📤 推送镜像到 Docker Hub..." -ForegroundColor Gray
        docker push $imageNameRemote 2>&1 | Out-Null
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "    ❌ 推送失败" -ForegroundColor Red
            $results += @{Service=$serviceName; Status="失败"; Reason="推送失败"}
            $failCount++
            continue
        }
        
        Write-Host "    ✅ 镜像推送成功: $imageNameRemote" -ForegroundColor Green
    } else {
        Write-Host "    ⏭️  跳过推送" -ForegroundColor Gray
    }
    
    $results += @{Service=$serviceName; Status="成功"; ImageRemote=$imageNameRemote}
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
        if ($result.ImageRemote) {
            Write-Host "   镜像: $($result.ImageRemote)" -ForegroundColor Gray
        }
    } elseif ($status -eq "跳过") {
        Write-Host "⏭️  $service - 跳过 ($($result.Reason))" -ForegroundColor Yellow
    } else {
        Write-Host "❌ $service - 失败 ($($result.Reason))" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "总计: $($results.Count) 个服务" -ForegroundColor Cyan
Write-Host "成功: $successCount" -ForegroundColor Green
Write-Host "失败: $failCount" -ForegroundColor Red
Write-Host ""

# 生成部署文件
if ($successCount -gt 0 -and -not $SkipPush) {
    Write-Host "生成 docker-compose.remote.yml 部署文件..." -ForegroundColor Yellow
    
    $composeContent = @"
version: '3.8'

# 使用远程 Docker Hub 镜像的部署配置
# 镜像源: ${DockerRegistry}/${DockerNamespace}/evcs-*:${ImageTag}

services:
"@

    foreach ($result in $results | Where-Object { $_.Status -eq "成功" }) {
        $serviceName = $result.Service
        $service = $allServices | Where-Object { $_.Name -eq $serviceName }
        
        $composeContent += @"

  ${serviceName}:
    image: $($result.ImageRemote)
    container_name: evcs-${serviceName}
    restart: unless-stopped
    ports:
      - "$($service.Port):$($service.Port)"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      EUREKA_SERVER_URL: http://eureka:8761/eureka/
    networks:
      - evcs-network
    depends_on:
      - postgres
      - redis
      - rabbitmq
"@
    }

    $composeContent += @"

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
    networks:
      - evcs-network

volumes:
  postgres_data:
  redis_data:
  rabbitmq_data:

networks:
  evcs-network:
    driver: bridge
"@

    $composeContent | Out-File -FilePath "docker-compose.remote.yml" -Encoding UTF8
    Write-Host "  ✅ 已生成: docker-compose.remote.yml" -ForegroundColor Green
    Write-Host ""
}

# 显示下一步操作
if ($successCount -gt 0) {
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host " 下一步操作" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "📦 在本地测试部署:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f docker-compose.remote.yml up -d" -ForegroundColor White
    Write-Host ""
    
    Write-Host "🚀 在远程服务器部署:" -ForegroundColor Yellow
    Write-Host "  1. 复制 docker-compose.remote.yml 到服务器" -ForegroundColor White
    Write-Host "  2. 在服务器上执行: docker-compose -f docker-compose.remote.yml up -d" -ForegroundColor White
    Write-Host ""
    
    Write-Host "🔍 查看镜像:" -ForegroundColor Yellow
    Write-Host "  docker images | findstr evcs" -ForegroundColor White
    Write-Host ""
    
    Write-Host "📋 部署的镜像:" -ForegroundColor Yellow
    foreach ($result in $results | Where-Object { $_.Status -eq "成功" }) {
        Write-Host "  $($result.ImageRemote)" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($failCount -eq 0) {
    Write-Host "🎉 所有服务镜像构建并推送成功！" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️  部分服务失败，请检查错误信息" -ForegroundColor Yellow
    exit 1
}
