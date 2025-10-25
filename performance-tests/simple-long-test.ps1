# 简化的长时间负载测试（使用curl，更可靠）
param(
    [int]$DurationMinutes = 30,
    [int]$ConcurrentRequests = 20
)

$services = @(
    @{Name="Order"; Port=8083},
    @{Name="Station"; Port=8082},
    @{Name="Gateway"; Port=8080}
)

Write-Host "`n=== EVCS 长时间负载测试 ===" -ForegroundColor Cyan
Write-Host "持续时间: $DurationMinutes 分钟"
Write-Host "并发请求: $ConcurrentRequests per service"
Write-Host "目标: 收集JFR数据`n"

$startTime = Get-Date
$endTime = $startTime.AddMinutes($DurationMinutes)

Write-Host "开始时间: $($startTime.ToString('HH:mm:ss'))"
Write-Host "结束时间: $($endTime.ToString('HH:mm:ss'))`n"

$results = @{}
foreach ($svc in $services) {
    $results[$svc.Name] = @{Success=0; Error=0}
}

$iteration = 0
while ((Get-Date) -lt $endTime) {
    $iteration++
    
    foreach ($service in $services) {
        $url = "http://localhost:$($service.Port)/actuator/health"
        
        # 并发请求
        1..$ConcurrentRequests | ForEach-Object -Parallel {
            try {
                $response = curl -s -w "%{http_code}" -o nul $using:url
                if ($response -eq "200") {
                    [PSCustomObject]@{Service=$using:service.Name; Status="Success"}
                } else {
                    [PSCustomObject]@{Service=$using:service.Name; Status="Error"}
                }
            } catch {
                [PSCustomObject]@{Service=$using:service.Name; Status="Error"}
            }
        } -ThrottleLimit $ConcurrentRequests | ForEach-Object {
            if ($_.Status -eq "Success") {
                $results[$_.Service].Success++
            } else {
                $results[$_.Service].Error++
            }
        }
    }
    
    # 每10次迭代输出进度
    if ($iteration % 10 -eq 0) {
        $elapsed = ((Get-Date) - $startTime).TotalMinutes
        $remaining = ($endTime - (Get-Date)).TotalMinutes
        
        Write-Host ("[{0:HH:mm:ss}] Iteration {1} - Elapsed: {2:F1}min, Remaining: {3:F1}min" -f 
            (Get-Date), $iteration, $elapsed, $remaining) -ForegroundColor Yellow
        
        foreach ($service in $services) {
            $s = $results[$service.Name].Success
            $e = $results[$service.Name].Error
            $total = $s + $e
            if ($total -gt 0) {
                Write-Host ("  {0}: {1} reqs ({2} success, {3} errors)" -f 
                    $service.Name, $total, $s, $e)
            }
        }
        Write-Host ""
    }
    
    # 短暂休息避免过载
    Start-Sleep -Milliseconds 500
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Green

$finalResults = @()
foreach ($service in $services) {
    $s = $results[$service.Name].Success
    $e = $results[$service.Name].Error
    $total = $s + $e
    
    $finalResults += [PSCustomObject]@{
        Service = $service.Name
        TotalRequests = $total
        Success = $s
        Errors = $e
        ErrorRate = if ($total -gt 0) { "$([math]::Round(($e/$total)*100, 2))%" } else { "0%" }
    }
}

$finalResults | Format-Table -AutoSize

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultsFile = "results/long-load-$timestamp.json"
$finalResults | ConvertTo-Json | Out-File -FilePath $resultsFile -Encoding UTF8

Write-Host "`n结果已保存: $resultsFile"
Write-Host "`n现在可以导出JFR文件:" -ForegroundColor Cyan
Write-Host "docker cp evcs-order:/tmp/flight.jfr logs/jfr/day3-long-order.jfr"
Write-Host "docker cp evcs-station:/tmp/flight.jfr logs/jfr/day3-long-station.jfr"  
Write-Host "docker cp evcs-gateway:/tmp/flight.jfr logs/jfr/day3-long-gateway.jfr"
