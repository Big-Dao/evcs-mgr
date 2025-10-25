# 前端完整部署脚本（包含构建）

param(
    [switch]$SkipBuild = $false,
    [switch]$SkipDocker = $false
)

Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  EVCS Admin 完整部署" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"

# 返回根目录
Set-Location $PSScriptRoot\..

# 1. 构建前端
if (-not $SkipBuild) {
    Write-Host "步骤 1/3: 构建前端项目..." -ForegroundColor Cyan
    Set-Location evcs-admin
    
    if (-not (Test-Path "node_modules")) {
        Write-Host "  安装依赖..." -ForegroundColor Yellow
        npm install
    }
    
    Write-Host "  执行构建..." -ForegroundColor Yellow
    npm run build
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ 构建失败！" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "✓ 前端构建成功" -ForegroundColor Green
    Set-Location ..
} else {
    Write-Host "步骤 1/3: 跳过构建（使用现有 dist/）" -ForegroundColor Yellow
}

# 2. 构建 Docker 镜像
if (-not $SkipDocker) {
    Write-Host "`n步骤 2/3: 构建 Docker 镜像..." -ForegroundColor Cyan
    docker build -t evcs-admin:latest ./evcs-admin
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ Docker 镜像构建失败！" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "✓ Docker 镜像构建成功" -ForegroundColor Green
} else {
    Write-Host "`n步骤 2/3: 跳过 Docker 构建" -ForegroundColor Yellow
}

# 3. 启动服务
Write-Host "`n步骤 3/3: 启动前端服务..." -ForegroundColor Cyan

# 检查服务是否已运行
$existingContainer = docker ps -a --filter "name=evcs-admin" --format "{{.Names}}" 2>$null
if ($existingContainer -eq "evcs-admin") {
    Write-Host "  停止现有容器..." -ForegroundColor Yellow
    docker stop evcs-admin | Out-Null
    docker rm evcs-admin | Out-Null
}

# 启动新容器
Write-Host "  启动容器..." -ForegroundColor Yellow
docker run -d `
    --name evcs-admin `
    --network evcs-mgr_evcs-network `
    -p 3000:80 `
    evcs-admin:latest

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 容器启动失败！" -ForegroundColor Red
    exit 1
}

Write-Host "✓ 前端服务启动成功" -ForegroundColor Green

# 等待服务就绪
Write-Host "`n等待服务就绪..." -ForegroundColor Cyan
Start-Sleep -Seconds 3

# 健康检查
$maxRetries = 10
$retry = 0
$healthy = $false

while ($retry -lt $maxRetries -and -not $healthy) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:3000/" -Method GET -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $healthy = $true
        }
    } catch {
        $retry++
        Start-Sleep -Seconds 2
    }
}

Write-Host ""
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan

if ($healthy) {
    Write-Host "  ✓ 部署成功！" -ForegroundColor Green
    Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "访问地址: " -NoNewline -ForegroundColor White
    Write-Host "http://localhost:3000" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "查看日志: " -NoNewline -ForegroundColor White
    Write-Host "docker logs -f evcs-admin" -ForegroundColor Gray
    Write-Host "停止服务: " -NoNewline -ForegroundColor White
    Write-Host "docker stop evcs-admin" -ForegroundColor Gray
} else {
    Write-Host "  ⚠ 服务未就绪" -ForegroundColor Yellow
    Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "请检查日志: docker logs evcs-admin" -ForegroundColor Yellow
}

Write-Host ""
