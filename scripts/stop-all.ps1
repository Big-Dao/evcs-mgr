# 停止所有Docker服务
# 用途：清理开发环境

Write-Host "==================================" -ForegroundColor Yellow
Write-Host "EVCS Manager - 停止所有服务" -ForegroundColor Yellow
Write-Host "==================================" -ForegroundColor Yellow
Write-Host ""

$choice = Read-Host "选择停止方式:
  1) 停止本地基础设施 (docker-compose.local.yml)
  2) 停止完整开发环境 (docker-compose.dev.yml)  
  3) 停止所有并删除数据卷
  4) 取消
请选择 (1-4)"

switch ($choice) {
    "1" {
        Write-Host ""
        Write-Host "停止本地基础设施服务..." -ForegroundColor Yellow
        docker-compose -f docker-compose.local.yml down
        Write-Host "服务已停止 ✓" -ForegroundColor Green
    }
    "2" {
        Write-Host ""
        Write-Host "停止完整开发环境..." -ForegroundColor Yellow
        docker-compose -f docker-compose.dev.yml down
        Write-Host "服务已停止 ✓" -ForegroundColor Green
    }
    "3" {
        Write-Host ""
        Write-Host "警告: 这将删除所有数据!" -ForegroundColor Red
        $confirm = Read-Host "确认删除? (yes/no)"
        if ($confirm -eq "yes") {
            Write-Host "停止服务并删除数据卷..." -ForegroundColor Yellow
            docker-compose -f docker-compose.local.yml down -v
            docker-compose -f docker-compose.dev.yml down -v
            Write-Host "服务已停止，数据已删除 ✓" -ForegroundColor Green
        } else {
            Write-Host "已取消" -ForegroundColor Yellow
        }
    }
    "4" {
        Write-Host "已取消" -ForegroundColor Yellow
        exit 0
    }
    default {
        Write-Host "无效选择" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "查看运行中的容器:" -ForegroundColor Cyan
docker ps
