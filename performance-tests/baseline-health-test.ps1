# 简化基线性能测试 - 测试健康检查端点
# 用途: 在没有业务数据和认证token的情况下，测试系统基础性能

param(
    [int]$Duration = 60,  # 测试持续时间（秒）
    [int]$Threads = 50    # 并发线程数
)

Write-Host "=== EVCS 简化基线性能测试 ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "测试配置:" -ForegroundColor Yellow
Write-Host "- 并发线程: $Threads" -ForegroundColor Gray
Write-Host "- 持续时间: $Duration 秒" -ForegroundColor Gray
Write-Host "- 目标服务: Actuator Health 端点" -ForegroundColor Gray
Write-Host ""

# 测试服务列表
$services = @(
    @{Name="Order"; Port=8083},
    @{Name="Station"; Port=8082},
    @{Name="Gateway"; Port=8080}
)

Write-Host "将测试以下服务:" -ForegroundColor Yellow
$services | ForEach-Object { Write-Host "  - $($_.Name) (端口 $($_.Port))" -ForegroundColor Gray }
Write-Host ""

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultsDir = "results"
$reportFile = "$resultsDir/baseline-health-$timestamp.txt"

if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir | Out-Null
}

Write-Host "开始测试..." -ForegroundColor Green
Write-Host ""

$results = @()

foreach ($service in $services) {
    Write-Host "测试 $($service.Name)..." -ForegroundColor Cyan
    
    $url = "http://localhost:$($service.Port)/actuator/health"
    $startTime = Get-Date
    $successCount = 0
    $errorCount = 0
    $responseTimes = @()
    
    1..$Threads | ForEach-Object -Parallel {
        $url = $using:url
        $duration = $using:Duration
        $endTime = (Get-Date).AddSeconds($duration)
        $localSuccess = 0
        $localErrors = 0
        $localTimes = @()
        
        while ((Get-Date) -lt $endTime) {
            $reqStart = Get-Date
            try {
                $response = Invoke-RestMethod -Uri $url -TimeoutSec 2 -ErrorAction Stop
                $reqEnd = Get-Date
                $responseTime = ($reqEnd - $reqStart).TotalMilliseconds
                $localTimes += $responseTime
                $localSuccess++
            } catch {
                $localErrors++
            }
        }
        
        [PSCustomObject]@{
            Success = $localSuccess
            Errors = $localErrors
            ResponseTimes = $localTimes
        }
    } -ThrottleLimit $Threads | ForEach-Object {
        $successCount += $_.Success
        $errorCount += $_.Errors
        $responseTimes += $_.ResponseTimes
    }
    
    $endTime = Get-Date
    $actualDuration = ($endTime - $startTime).TotalSeconds
    $totalRequests = $successCount + $errorCount
    $tps = [math]::Round($totalRequests / $actualDuration, 2)
    
    $sortedTimes = $responseTimes | Sort-Object
    $p50 = if ($sortedTimes.Count -gt 0) { [math]::Round($sortedTimes[[math]::Floor($sortedTimes.Count * 0.5)], 2) } else { 0 }
    $p90 = if ($sortedTimes.Count -gt 0) { [math]::Round($sortedTimes[[math]::Floor($sortedTimes.Count * 0.9)], 2) } else { 0 }
    $p99 = if ($sortedTimes.Count -gt 0) { [math]::Round($sortedTimes[[math]::Floor($sortedTimes.Count * 0.99)], 2) } else { 0 }
    $avg = if ($sortedTimes.Count -gt 0) { [math]::Round(($sortedTimes | Measure-Object -Average).Average, 2) } else { 0 }
    
    $result = [PSCustomObject]@{
        Service = $service.Name
        TotalRequests = $totalRequests
        Success = $successCount
        Errors = $errorCount
        ErrorRate = [math]::Round(($errorCount / $totalRequests) * 100, 2)
        TPS = $tps
        AvgResponseTime = $avg
        P50 = $p50
        P90 = $p90
        P99 = $p99
    }
    
    $results += $result
    
    Write-Host "  总请求: $totalRequests" -ForegroundColor Gray
    Write-Host "  成功: $successCount" -ForegroundColor Green
    Write-Host "  失败: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Gray" })
    Write-Host "  TPS: $tps" -ForegroundColor Yellow
    Write-Host "  响应时间 P50/P90/P99: ${p50}ms / ${p90}ms / ${p99}ms" -ForegroundColor Cyan
    Write-Host ""
}

Write-Host "=== 测试完成 ===" -ForegroundColor Green
Write-Host ""
Write-Host "汇总结果:" -ForegroundColor Cyan
$results | Format-Table -AutoSize

# 保存结果
$results | ConvertTo-Json -Depth 5 | Out-File -FilePath $reportFile -Encoding UTF8
Write-Host "结果已保存到: $reportFile" -ForegroundColor Gray
Write-Host ""

# 分析结果
$avgTps = ($results | Measure-Object -Property TPS -Average).Average
$maxP99 = ($results | Measure-Object -Property P99 -Maximum).Maximum
$avgErrorRate = ($results | Measure-Object -Property ErrorRate -Average).Average

Write-Host "=== 性能指标总结 ===" -ForegroundColor Cyan
Write-Host "平均 TPS: $([math]::Round($avgTps, 2))" -ForegroundColor Yellow
Write-Host "最大 P99 响应时间: $([math]::Round($maxP99, 2))ms" -ForegroundColor $(if ($maxP99 -lt 100) { "Green" } elseif ($maxP99 -lt 500) { "Yellow" } else { "Red" })
Write-Host "平均错误率: $([math]::Round($avgErrorRate, 2))%" -ForegroundColor $(if ($avgErrorRate -lt 1) { "Green" } else { "Red" })
Write-Host ""

if ($maxP99 -lt 100 -and $avgErrorRate -lt 1) {
    Write-Host "✅ 性能表现优秀！" -ForegroundColor Green
} elseif ($maxP99 -lt 500 -and $avgErrorRate -lt 5) {
    Write-Host "⚠️  性能可接受，但有优化空间" -ForegroundColor Yellow
} else {
    Write-Host "❌ 性能需要优化" -ForegroundColor Red
}
