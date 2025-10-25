# 30分钟长时间负载测试
# 目的：收集足够的JFR数据用于深度GC分析
param(
    [int]$DurationMinutes = 30,
    [int]$Threads = 50,
    [int]$RequestsPerBatch = 100
)

$services = @(
    @{Name="Order"; Port=8083},
    @{Name="Station"; Port=8082},
    @{Name="Gateway"; Port=8080}
)

Write-Host "`n=== EVCS 长时间负载测试 ===" -ForegroundColor Cyan
Write-Host "持续时间: $DurationMinutes 分钟"
Write-Host "并发线程: $Threads"
Write-Host "目标: 收集足够的JFR数据用于GC分析`n"

$startTime = Get-Date
$endTime = $startTime.AddMinutes($DurationMinutes)
$results = @{}

foreach ($service in $services) {
    $results[$service.Name] = @{
        TotalRequests = 0
        Success = 0
        Errors = 0
        ResponseTimes = New-Object System.Collections.ArrayList
    }
}

Write-Host "测试开始时间: $($startTime.ToString('yyyy-MM-dd HH:mm:ss'))"
Write-Host "预计结束时间: $($endTime.ToString('yyyy-MM-dd HH:mm:ss'))`n"

$batchCount = 0
while ((Get-Date) -lt $endTime) {
    $batchCount++
    $batchStartTime = Get-Date
    
    # 为每个服务发送请求
    foreach ($service in $services) {
        $url = "http://localhost:$($service.Port)/actuator/health"
        
        $jobs = 1..$Threads | ForEach-Object {
            Start-Job -ScriptBlock {
                param($url, $reqCount)
                $successCount = 0
                $errorCount = 0
                $times = @()
                
                for ($i = 0; $i -lt $reqCount; $i++) {
                    $reqStart = Get-Date
                    try {
                        $response = Invoke-RestMethod -Uri $url -TimeoutSec 5 -ErrorAction Stop
                        $reqEnd = Get-Date
                        if ($response.status -eq "UP") {
                            $successCount++
                            $times += ($reqEnd - $reqStart).TotalMilliseconds
                        } else {
                            $errorCount++
                        }
                    } catch {
                        $errorCount++
                    }
                }
                
                @{
                    Success = $successCount
                    Errors = $errorCount
                    ResponseTimes = $times
                }
            } -ArgumentList $url, $RequestsPerBatch
        }
        
        # 等待并收集结果
        $jobResults = $jobs | Wait-Job | Receive-Job
        $jobs | Remove-Job
        
        foreach ($result in $jobResults) {
            $results[$service.Name].Success += $result.Success
            $results[$service.Name].Errors += $result.Errors
            $results[$service.Name].TotalRequests += ($result.Success + $result.Errors)
            $result.ResponseTimes | ForEach-Object { 
                [void]$results[$service.Name].ResponseTimes.Add($_)
            }
        }
    }
    
    $batchDuration = ((Get-Date) - $batchStartTime).TotalSeconds
    $elapsed = ((Get-Date) - $startTime).TotalMinutes
    $remaining = ($endTime - (Get-Date)).TotalMinutes
    
    # 每5批输出一次进度
    if ($batchCount % 5 -eq 0) {
        Write-Host ("[{0:yyyy-MM-dd HH:mm:ss}] Batch {1} - Elapsed: {2:F1}min, Remaining: {3:F1}min, Batch Duration: {4:F1}s" -f 
            (Get-Date), $batchCount, $elapsed, $remaining, $batchDuration) -ForegroundColor Yellow
        
        # 显示当前统计
        foreach ($service in $services) {
            $data = $results[$service.Name]
            if ($data.TotalRequests -gt 0) {
                $tps = [math]::Round($data.Success / ($elapsed * 60), 2)
                $errorRate = [math]::Round(($data.Errors / $data.TotalRequests) * 100, 2)
                Write-Host ("  {0}: {1} reqs, TPS: {2}, Errors: {3}%" -f 
                    $service.Name, $data.TotalRequests, $tps, $errorRate)
            }
        }
        Write-Host ""
    }
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green
$totalDuration = ((Get-Date) - $startTime).TotalMinutes

# 生成最终报告
$finalResults = @()
foreach ($service in $services) {
    $data = $results[$service.Name]
    
    if ($data.ResponseTimes.Count -gt 0) {
        $sorted = $data.ResponseTimes | Sort-Object
        $p50 = [math]::Round($sorted[[math]::Floor($sorted.Count * 0.5)], 2)
        $p90 = [math]::Round($sorted[[math]::Floor($sorted.Count * 0.9)], 2)
        $p99 = [math]::Round($sorted[[math]::Floor($sorted.Count * 0.99)], 2)
        $avg = [math]::Round(($sorted | Measure-Object -Average).Average, 2)
    } else {
        $p50 = $p90 = $p99 = $avg = 0
    }
    
    $tps = [math]::Round($data.Success / ($totalDuration * 60), 2)
    $errorRate = [math]::Round(($data.Errors / $data.TotalRequests) * 100, 2)
    
    $finalResults += [PSCustomObject]@{
        Service = $service.Name
        TotalRequests = $data.TotalRequests
        Success = $data.Success
        Errors = $data.Errors
        ErrorRate = "$errorRate%"
        TPS = $tps
        AvgResponseTime = "${avg}ms"
        P50 = "${p50}ms"
        P90 = "${p90}ms"
        P99 = "${p99}ms"
    }
}

Write-Host "`n=== 最终统计 (测试时长: $([math]::Round($totalDuration, 1)) 分钟) ===" -ForegroundColor Cyan
$finalResults | Format-Table -AutoSize

# 保存结果
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultsFile = "results/long-load-test-$timestamp.json"
New-Item -ItemType Directory -Path "results" -Force | Out-Null
$finalResults | ConvertTo-Json | Out-File -FilePath $resultsFile -Encoding UTF8

Write-Host "`n结果已保存: $resultsFile" -ForegroundColor Green

# 提示导出JFR
Write-Host "`n=== 下一步：导出JFR文件 ===" -ForegroundColor Cyan
Write-Host "docker cp evcs-order:/tmp/flight.jfr logs/jfr/day3-long-order.jfr"
Write-Host "docker cp evcs-station:/tmp/flight.jfr logs/jfr/day3-long-station.jfr"
Write-Host "docker cp evcs-gateway:/tmp/flight.jfr logs/jfr/day3-long-gateway.jfr"
