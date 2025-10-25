# 简单可靠的基线性能测试
param(
    [int]$Requests = 1000,
    [int]$Concurrency = 50
)

$services = @(
    @{Name="Order"; Url="http://localhost:8083/actuator/health"},
    @{Name="Station"; Url="http://localhost:8082/actuator/health"},
    @{Name="Gateway"; Url="http://localhost:8080/actuator/health"}
)

Write-Host "`n=== EVCS 基线性能测试 ===" -ForegroundColor Cyan
Write-Host "请求数: $Requests per service"
Write-Host "并发数: $Concurrency`n"

$results = @()

foreach ($service in $services) {
    Write-Host "测试 $($service.Name)..." -NoNewline
    $startTime = Get-Date
    
    # 使用并行批次执行
    $batches = [math]::Ceiling($Requests / $Concurrency)
    $successCount = 0
    $failCount = 0
    $responseTimes = @()
    
    for ($batch = 0; $batch -lt $batches; $batch++) {
        $batchSize = [math]::Min($Concurrency, $Requests - ($batch * $Concurrency))
        
        $jobs = 1..$batchSize | ForEach-Object {
            Start-Job -ScriptBlock {
                param($url)
                $reqStart = Get-Date
                try {
                    $response = curl -s -w "%{http_code}" -o nul $url
                    $reqEnd = Get-Date
                    $elapsed = ($reqEnd - $reqStart).TotalMilliseconds
                    @{
                        Success = ($response -eq "200")
                        ResponseTime = $elapsed
                    }
                } catch {
                    @{
                        Success = $false
                        ResponseTime = 0
                    }
                }
            } -ArgumentList $service.Url
        }
        
        $jobResults = $jobs | Wait-Job | Receive-Job
        $jobs | Remove-Job
        
        foreach ($result in $jobResults) {
            if ($result.Success) {
                $successCount++
                $responseTimes += $result.ResponseTime
            } else {
                $failCount++
            }
        }
    }
    
    $endTime = Get-Date
    $totalTime = ($endTime - $startTime).TotalSeconds
    
    # 计算指标
    $tps = [math]::Round($successCount / $totalTime, 2)
    $errorRate = [math]::Round(($failCount / $Requests) * 100, 2)
    
    if ($responseTimes.Count -gt 0) {
        $sorted = $responseTimes | Sort-Object
        $p50 = [math]::Round($sorted[[math]::Floor($sorted.Count * 0.5)], 2)
        $p90 = [math]::Round($sorted[[math]::Floor($sorted.Count * 0.9)], 2)
        $p99 = [math]::Round($sorted[[math]::Floor($sorted.Count * 0.99)], 2)
        $avg = [math]::Round(($sorted | Measure-Object -Average).Average, 2)
    } else {
        $p50 = $p90 = $p99 = $avg = 0
    }
    
    Write-Host " 完成 ✓" -ForegroundColor Green
    
    $results += [PSCustomObject]@{
        Service = $service.Name
        TotalRequests = $Requests
        Success = $successCount
        Errors = $failCount
        ErrorRate = "$errorRate%"
        TPS = $tps
        AvgResponseTime = "${avg}ms"
        P50 = "${p50}ms"
        P90 = "${p90}ms"
        P99 = "${p99}ms"
    }
}

Write-Host "`n=== 测试结果 ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize

# 保存结果
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultsFile = "results/simple-baseline-$timestamp.json"
New-Item -ItemType Directory -Path "results" -Force | Out-Null
$results | ConvertTo-Json | Out-File -FilePath $resultsFile -Encoding UTF8

Write-Host "`n结果已保存: $resultsFile" -ForegroundColor Green

# 性能评估
$avgTps = ($results | Measure-Object -Property TPS -Average).Average
$maxErrorRate = ($results | ForEach-Object { [double]$_.ErrorRate.TrimEnd('%') } | Measure-Object -Maximum).Maximum

Write-Host "`n=== 性能评估 ===" -ForegroundColor Cyan
Write-Host "平均 TPS: $([math]::Round($avgTps, 2))"
Write-Host "最大错误率: $maxErrorRate%"

if ($avgTps -gt 200 -and $maxErrorRate -lt 1) {
    Write-Host "✓ 性能优异" -ForegroundColor Green
} elseif ($avgTps -gt 100 -and $maxErrorRate -lt 5) {
    Write-Host "✓ 性能可接受" -ForegroundColor Yellow
} else {
    Write-Host "✗ 性能需优化" -ForegroundColor Red
}
