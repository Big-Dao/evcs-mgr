#!/usr/bin/env pwsh
# Docker 构建验证脚本
# 验证所有服务的 Dockerfile 能否成功构建

$ErrorActionPreference = "Stop"

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host " Docker 构建依赖修复验证" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# 要测试的服务列表
$services = @(
    "evcs-station",
    "evcs-order",
    "evcs-tenant",
    "evcs-payment",
    "evcs-gateway",
    "evcs-auth"
)

$results = @()

foreach ($service in $services) {
    Write-Host "[测试] 构建 $service 镜像..." -ForegroundColor Yellow
    
    $dockerfilePath = "$service/Dockerfile"
    
    if (-not (Test-Path $dockerfilePath)) {
        Write-Host "  ❌ Dockerfile 不存在: $dockerfilePath" -ForegroundColor Red
        $results += @{Service=$service; Status="失败"; Reason="Dockerfile不存在"}
        continue
    }
    
    try {
        # 执行 Docker 构建
        $output = docker build -t "${service}:test" -f $dockerfilePath . 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✅ $service 构建成功" -ForegroundColor Green
            $results += @{Service=$service; Status="成功"; Reason=""}
        } else {
            Write-Host "  ❌ $service 构建失败" -ForegroundColor Red
            Write-Host "  错误输出:" -ForegroundColor Red
            Write-Host ($output | Select-Object -Last 20) -ForegroundColor Gray
            $results += @{Service=$service; Status="失败"; Reason="构建失败"}
        }
    } catch {
        Write-Host "  ❌ $service 构建异常: $_" -ForegroundColor Red
        $results += @{Service=$service; Status="失败"; Reason=$_.Exception.Message}
    }
    
    Write-Host ""
}

# 打印汇总结果
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host " 构建结果汇总" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$successCount = 0
$failCount = 0

foreach ($result in $results) {
    $service = $result.Service
    $status = $result.Status
    $reason = $result.Reason
    
    if ($status -eq "成功") {
        Write-Host "✅ $service - 成功" -ForegroundColor Green
        $successCount++
    } else {
        Write-Host "❌ $service - 失败 ($reason)" -ForegroundColor Red
        $failCount++
    }
}

Write-Host ""
Write-Host "总计: $($results.Count) 个服务" -ForegroundColor Cyan
Write-Host "成功: $successCount" -ForegroundColor Green
Write-Host "失败: $failCount" -ForegroundColor Red
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "🎉 所有 Docker 构建验证通过！" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️ 部分 Docker 构建失败，请检查错误日志" -ForegroundColor Yellow
    exit 1
}
