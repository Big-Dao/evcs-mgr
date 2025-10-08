# 启动完整开发环境 - 包含应用服务
# 用途：Docker环境完整测试

Write-Host "==================================" -ForegroundColor Green
Write-Host "EVCS Manager - 启动完整开发环境" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green
Write-Host ""

# 检查Docker是否运行
Write-Host "检查Docker状态..." -ForegroundColor Yellow
$dockerInfo = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: Docker未运行或未安装!" -ForegroundColor Red
    Write-Host "请启动Docker Desktop后重试" -ForegroundColor Red
    exit 1
}

Write-Host "Docker运行正常 ✓" -ForegroundColor Green
Write-Host ""

# 询问是否重新构建
$rebuild = Read-Host "是否重新构建应用镜像? (y/N)"
$buildFlag = ""
if ($rebuild -eq "y" -or $rebuild -eq "Y") {
    $buildFlag = "--build"
    Write-Host "将重新构建应用镜像..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "启动所有服务 (可能需要几分钟)..." -ForegroundColor Yellow
docker-compose -f docker-compose.dev.yml up $buildFlag -d

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "等待服务就绪..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    
    Write-Host ""
    Write-Host "==================================" -ForegroundColor Green
    Write-Host "服务启动成功!" -ForegroundColor Green
    Write-Host "==================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "服务访问地址:" -ForegroundColor Cyan
    Write-Host "  PostgreSQL:  localhost:5432" -ForegroundColor White
    Write-Host "  Redis:       localhost:6379" -ForegroundColor White
    Write-Host "  RabbitMQ:    localhost:5672" -ForegroundColor White
    Write-Host "  RabbitMQ管理: http://localhost:15672" -ForegroundColor White
    Write-Host "  租户服务:    http://localhost:8081" -ForegroundColor White
    Write-Host "  充电站服务:  http://localhost:8082" -ForegroundColor White
    Write-Host ""
    Write-Host "健康检查:" -ForegroundColor Cyan
    Write-Host "  curl http://localhost:8081/actuator/health" -ForegroundColor White
    Write-Host "  curl http://localhost:8082/actuator/health" -ForegroundColor White
    Write-Host ""
    Write-Host "查看日志:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f docker-compose.dev.yml logs -f" -ForegroundColor White
    Write-Host "  docker-compose -f docker-compose.dev.yml logs -f tenant-service" -ForegroundColor White
    Write-Host "  docker-compose -f docker-compose.dev.yml logs -f station-service" -ForegroundColor White
    Write-Host ""
    Write-Host "停止服务:" -ForegroundColor Yellow
    Write-Host "  docker-compose -f docker-compose.dev.yml down" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "启动失败! 查看错误信息" -ForegroundColor Red
    exit 1
}
