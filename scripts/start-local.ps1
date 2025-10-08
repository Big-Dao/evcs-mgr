# 启动本地开发环境 - 仅基础设施服务
# 用途：本地IDE开发时使用

Write-Host "==================================" -ForegroundColor Green
Write-Host "EVCS Manager - 启动本地基础设施" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green
Write-Host ""

# 检查Docker是否运行
Write-Host "检查Docker状态..." -ForegroundColor Yellow
$dockerRunning = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: Docker未运行或未安装!" -ForegroundColor Red
    Write-Host "请启动Docker Desktop后重试" -ForegroundColor Red
    exit 1
}

Write-Host "Docker运行正常 ✓" -ForegroundColor Green
Write-Host ""

# 启动服务
Write-Host "启动基础设施服务..." -ForegroundColor Yellow
docker-compose -f docker-compose.local.yml up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "==================================" -ForegroundColor Green
    Write-Host "服务启动成功!" -ForegroundColor Green
    Write-Host "==================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "服务访问地址:" -ForegroundColor Cyan
    Write-Host "  PostgreSQL:  localhost:5432 (postgres/postgres)" -ForegroundColor White
    Write-Host "  Redis:       localhost:6379" -ForegroundColor White
    Write-Host "  RabbitMQ:    localhost:5672 (guest/guest)" -ForegroundColor White
    Write-Host "  RabbitMQ管理: http://localhost:15672" -ForegroundColor White
    Write-Host "  Adminer:     http://localhost:8090" -ForegroundColor White
    Write-Host ""
    Write-Host "查看日志: docker-compose -f docker-compose.local.yml logs -f" -ForegroundColor Yellow
    Write-Host "停止服务: docker-compose -f docker-compose.local.yml down" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "现在可以在IDE中运行应用服务:" -ForegroundColor Cyan
    Write-Host "  gradlew :evcs-tenant:bootRun" -ForegroundColor White
    Write-Host "  gradlew :evcs-station:bootRun" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "启动失败! 查看错误信息" -ForegroundColor Red
    exit 1
}
