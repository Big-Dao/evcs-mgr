#!/usr/bin/env pwsh
# EVCS Manager 本地 Docker 完整部署脚本
# 包含: 基础设施 + 所有微服务 + 前端管理界面

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " EVCS Manager - 本地 Docker 完整部署" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker
Write-Host "[1/5] 检查 Docker 环境..." -ForegroundColor Yellow
$dockerInfo = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Docker 未运行！请启动 Docker Desktop" -ForegroundColor Red
    exit 1
}
Write-Host "  ✅ Docker 运行正常" -ForegroundColor Green
Write-Host ""

# 清理旧容器（可选）
Write-Host "[2/5] 清理旧容器..." -ForegroundColor Yellow
$cleanup = Read-Host "是否清理旧容器和镜像？(y/N)"
if ($cleanup -eq "y" -or $cleanup -eq "Y") {
    Write-Host "  停止现有容器..." -ForegroundColor Gray
    docker-compose down -v 2>$null
    
    Write-Host "  清理旧镜像..." -ForegroundColor Gray
    docker images "evcs-*" -q | ForEach-Object { docker rmi $_ -f 2>$null }
    
    Write-Host "  ✅ 清理完成" -ForegroundColor Green
} else {
    Write-Host "  ⏭️  跳过清理" -ForegroundColor Gray
}
Write-Host ""

# 构建 JAR 文件
Write-Host "[3/5] 构建应用 JAR 文件..." -ForegroundColor Yellow
Write-Host "  正在编译项目（跳过测试）..." -ForegroundColor Gray

$buildOutput = .\gradlew build -x test --no-daemon 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Gradle 构建失败！" -ForegroundColor Red
    Write-Host $buildOutput -ForegroundColor Red
    exit 1
}
Write-Host "  ✅ 构建成功" -ForegroundColor Green
Write-Host ""

# 构建 Docker 镜像
Write-Host "[4/5] 构建 Docker 镜像..." -ForegroundColor Yellow
Write-Host "  这可能需要几分钟时间..." -ForegroundColor Gray

$services = @(
    "eureka",
    "config",
    "gateway",
    "auth",
    "tenant",
    "station",
    "order",
    "payment",
    "protocol",
    "monitoring"
)

$buildFailed = $false
foreach ($service in $services) {
    Write-Host "  构建 evcs-$service..." -ForegroundColor Gray
    
    $dockerfile = "evcs-$service/Dockerfile"
    if (-not (Test-Path $dockerfile)) {
        Write-Host "    ⚠️  Dockerfile 不存在，跳过" -ForegroundColor Yellow
        continue
    }
    
    docker build -t "evcs-${service}:latest" -f $dockerfile . 2>&1 | Out-Null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "    ✅ evcs-$service" -ForegroundColor Green
    } else {
        Write-Host "    ❌ evcs-$service 构建失败" -ForegroundColor Red
        $buildFailed = $true
    }
}

if ($buildFailed) {
    Write-Host ""
    Write-Host "  ⚠️  部分镜像构建失败，但将继续启动" -ForegroundColor Yellow
}
Write-Host ""

# 启动服务
Write-Host "[5/5] 启动所有服务..." -ForegroundColor Yellow
Write-Host "  使用 docker-compose.yml 启动..." -ForegroundColor Gray

docker-compose up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ 服务启动成功" -ForegroundColor Green
} else {
    Write-Host "  ❌ 服务启动失败" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 等待服务就绪
Write-Host "等待服务启动（30秒）..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# 检查服务健康状态
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 服务健康检查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$containers = docker ps --format "{{.Names}}" | Where-Object { $_ -like "evcs-*" }
foreach ($container in $containers) {
    $health = docker inspect --format='{{.State.Health.Status}}' $container 2>$null
    $status = docker inspect --format='{{.State.Status}}' $container
    
    if ($status -eq "running") {
        if ($health -eq "healthy") {
            Write-Host "  ✅ $container - 运行中（健康）" -ForegroundColor Green
        } elseif ($health -eq "starting") {
            Write-Host "  🔄 $container - 运行中（启动中）" -ForegroundColor Yellow
        } else {
            Write-Host "  ⚠️  $container - 运行中" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ❌ $container - $status" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 部署完成！访问地址" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "🔹 基础设施服务:" -ForegroundColor Cyan
Write-Host "  PostgreSQL:     localhost:5432 (postgres/postgres)" -ForegroundColor White
Write-Host "  Redis:          localhost:6379" -ForegroundColor White
Write-Host "  RabbitMQ:       localhost:5672 (guest/guest)" -ForegroundColor White
Write-Host "  RabbitMQ 管理:  http://localhost:15672" -ForegroundColor White
Write-Host ""

Write-Host "🔹 应用服务:" -ForegroundColor Cyan
Write-Host "  服务注册中心:   http://localhost:8761" -ForegroundColor White
Write-Host "  配置中心:       http://localhost:8888" -ForegroundColor White
Write-Host "  API 网关:       http://localhost:8080" -ForegroundColor White
Write-Host "  认证服务:       http://localhost:8081/actuator/health" -ForegroundColor White
Write-Host "  租户服务:       http://localhost:8083/actuator/health" -ForegroundColor White
Write-Host "  充电站服务:     http://localhost:8082/actuator/health" -ForegroundColor White
Write-Host "  订单服务:       http://localhost:8084/actuator/health" -ForegroundColor White
Write-Host "  支付服务:       http://localhost:8085/actuator/health" -ForegroundColor White
Write-Host ""

Write-Host "🔹 监控服务:" -ForegroundColor Cyan
Write-Host "  Prometheus:     http://localhost:9090" -ForegroundColor White
Write-Host "  Grafana:        http://localhost:3000 (admin/admin)" -ForegroundColor White
Write-Host ""

Write-Host "📋 常用命令:" -ForegroundColor Cyan
Write-Host "  查看日志:       docker-compose logs -f [服务名]" -ForegroundColor Gray
Write-Host "  查看所有日志:   docker-compose logs -f" -ForegroundColor Gray
Write-Host "  重启服务:       docker-compose restart [服务名]" -ForegroundColor Gray
Write-Host "  停止所有服务:   docker-compose down" -ForegroundColor Gray
Write-Host "  查看服务状态:   docker-compose ps" -ForegroundColor Gray
Write-Host ""

Write-Host "🔍 健康检查:" -ForegroundColor Cyan
Write-Host "  .\scripts\health-check.sh" -ForegroundColor Gray
Write-Host ""

Write-Host "⚠️  注意事项:" -ForegroundColor Yellow
Write-Host "  1. 首次启动可能需要等待 1-2 分钟" -ForegroundColor Gray
Write-Host "  2. 如果服务未就绪，请使用 'docker-compose logs [服务名]' 查看日志" -ForegroundColor Gray
Write-Host "  3. 默认管理员账号: admin / admin123" -ForegroundColor Gray
Write-Host ""
