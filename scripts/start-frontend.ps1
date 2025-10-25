# 前端快速启动脚本

Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  EVCS Admin 前端启动" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 检查 Node.js
Write-Host "检查 Node.js..." -ForegroundColor Cyan
$nodeVersion = node --version 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Node.js 版本: $nodeVersion" -ForegroundColor Green
} else {
    Write-Host "✗ Node.js 未安装！请先安装 Node.js" -ForegroundColor Red
    exit 1
}

# 进入前端目录
Set-Location evcs-admin

# 检查依赖
Write-Host "`n检查依赖..." -ForegroundColor Cyan
if (Test-Path "node_modules") {
    Write-Host "✓ 依赖已安装" -ForegroundColor Green
} else {
    Write-Host "⚠ 正在安装依赖..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ 依赖安装失败！" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ 依赖安装成功" -ForegroundColor Green
}

# 检查后端服务
Write-Host "`n检查后端服务..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Gateway 服务运行中 (http://localhost:8080)" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠ Gateway 服务未启动！" -ForegroundColor Yellow
    Write-Host "  请先启动后端服务: docker-compose up -d" -ForegroundColor Gray
}

# 启动开发服务器
Write-Host "`n启动开发服务器..." -ForegroundColor Cyan
Write-Host "访问地址: http://localhost:3000" -ForegroundColor Green
Write-Host "按 Ctrl+C 停止服务`n" -ForegroundColor Gray
Write-Host "═══════════════════════════════════════`n" -ForegroundColor Cyan

npm run dev
