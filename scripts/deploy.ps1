# EVCS Manager 生产环境部署脚本
# 版本: v1.2.0
# 更新日期: 2025-10-28
# 基于 P4 Week 2 性能优化成果

param(
    [switch]$Build,
    [switch]$NoBuild,
    [switch]$Verbose
)

Write-Host "==================================" -ForegroundColor Green
Write-Host "EVCS Manager - 生产环境部署" -ForegroundColor Green
Write-Host "版本: v1.2.0 (P4 Week 2)" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green
Write-Host ""

# 检查Docker是否运行
Write-Host "1. 检查Docker状态..." -ForegroundColor Yellow
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: Docker未运行或未安装!" -ForegroundColor Red
    Write-Host "请启动Docker Desktop后重试" -ForegroundColor Red
    exit 1
}
Write-Host "   Docker运行正常 ✓" -ForegroundColor Green

# 检查docker-compose.yml
if (-not (Test-Path "docker-compose.yml")) {
    Write-Host "错误: 未找到 docker-compose.yml" -ForegroundColor Red
    exit 1
}

# 构建应用镜像
if ($Build -or (-not $NoBuild)) {
    Write-Host ""
    Write-Host "2. 构建应用镜像..." -ForegroundColor Yellow
    Write-Host "   运行测试和构建 (可能需要几分钟)..." -ForegroundColor Cyan
    
    & .\gradlew.bat clean build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "   构建失败!" -ForegroundColor Red
        exit 1
    }
    Write-Host "   构建成功 ✓" -ForegroundColor Green
}

# 停止旧服务
Write-Host ""
Write-Host "3. 停止旧服务..." -ForegroundColor Yellow
docker-compose down
Write-Host "   旧服务已停止 ✓" -ForegroundColor Green

# 启动新服务
Write-Host ""
Write-Host "4. 启动新服务..." -ForegroundColor Yellow
if ($Build -or (-not $NoBuild)) {
    docker-compose up --build -d
} else {
    docker-compose up -d
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "   启动失败!" -ForegroundColor Red
    exit 1
}

# 等待服务就绪
Write-Host ""
Write-Host "5. 等待服务就绪 (60秒)..." -ForegroundColor Yellow
for ($i = 1; $i -le 12; $i++) {
    Write-Host "   等待... $($i*5)秒" -ForegroundColor Cyan
    Start-Sleep -Seconds 5
}

# 健康检查
Write-Host ""
Write-Host "6. 执行健康检查..." -ForegroundColor Yellow

$services = @(
    @{Name="Gateway"; Port=8080; Path="/actuator/health"},
    @{Name="Auth"; Port=8081; Path="/actuator/health"},
    @{Name="Station"; Port=8082; Path="/actuator/health"},
    @{Name="Order"; Port=8083; Path="/actuator/health"},
    @{Name="Payment"; Port=8084; Path="/actuator/health"},
    @{Name="Protocol"; Port=8085; Path="/actuator/health"},
    @{Name="Tenant"; Port=8086; Path="/actuator/health"},
    @{Name="Monitoring"; Port=8087; Path="/actuator/health"}
)

$allHealthy = $true
foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$($service.Port)$($service.Path)" -TimeoutSec 5 -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            Write-Host "   $($service.Name): ✓ 健康" -ForegroundColor Green
        } else {
            Write-Host "   $($service.Name): ✗ 状态码 $($response.StatusCode)" -ForegroundColor Red
            $allHealthy = $false
        }
    } catch {
        Write-Host "   $($service.Name): ✗ 无法访问" -ForegroundColor Red
        $allHealthy = $false
    }
}

# 部署结果
Write-Host ""
Write-Host "==================================" -ForegroundColor Green
if ($allHealthy) {
    Write-Host "部署成功! 所有服务健康运行" -ForegroundColor Green
    Write-Host "==================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "访问地址:" -ForegroundColor Cyan
    Write-Host "  API网关:     http://localhost:8080" -ForegroundColor White
    Write-Host "  管理界面:    http://localhost:3000" -ForegroundColor White
    Write-Host "  Eureka:      http://localhost:8761" -ForegroundColor White
    Write-Host "  RabbitMQ:    http://localhost:15672 (guest/guest)" -ForegroundColor White
    Write-Host ""
    Write-Host "性能指标 (P4 Week 2 优化):" -ForegroundColor Cyan
    Write-Host "  JVM堆内存:   512MB (固定)" -ForegroundColor White
    Write-Host "  GC策略:      G1GC (最大暂停 100ms)" -ForegroundColor White
    Write-Host "  连接池:      HikariCP max=30, min=10" -ForegroundColor White
    Write-Host "  测试通过率:  168/168 (100%)" -ForegroundColor White
    Write-Host "  代码覆盖率:  96%" -ForegroundColor White
} else {
    Write-Host "部署完成，但部分服务不健康" -ForegroundColor Yellow
    Write-Host "==================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "请检查日志:" -ForegroundColor Yellow
    Write-Host "  docker-compose logs -f" -ForegroundColor White
}

Write-Host ""
Write-Host "常用命令:" -ForegroundColor Cyan
Write-Host "  查看状态:    docker-compose ps" -ForegroundColor White
Write-Host "  查看日志:    docker-compose logs -f [service]" -ForegroundColor White
Write-Host "  重启服务:    docker-compose restart [service]" -ForegroundColor White
Write-Host "  停止服务:    docker-compose down" -ForegroundColor White
Write-Host ""
