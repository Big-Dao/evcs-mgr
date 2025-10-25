# 快速环境健康检查
# 用途: 验证所有关键服务是否就绪

Write-Host "=== EVCS 环境健康检查 ===" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{Name="Eureka"; Port=8761; Path="/actuator/health"},
    @{Name="Config Server"; Port=8888; Path="/actuator/health"},
    @{Name="Gateway"; Port=8080; Path="/actuator/health"},
    @{Name="Auth"; Port=8081; Path="/actuator/health"},
    @{Name="Station"; Port=8082; Path="/actuator/health"},
    @{Name="Order"; Port=8083; Path="/actuator/health"},
    @{Name="Payment"; Port=8084; Path="/actuator/health"},
    @{Name="Protocol"; Port=8085; Path="/actuator/health"},
    @{Name="Tenant"; Port=8086; Path="/actuator/health"}
)

$successCount = 0
$totalCount = $services.Count

foreach ($service in $services) {
    $url = "http://localhost:$($service.Port)$($service.Path)"
    Write-Host "检查 $($service.Name) ($url)..." -NoNewline
    
    try {
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 5 -ErrorAction Stop
        if ($response.status -eq "UP") {
            Write-Host " ✅ UP" -ForegroundColor Green
            $successCount++
        } else {
            Write-Host " ⚠️  $($response.status)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host " ❌ DOWN" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== 健康检查结果 ===" -ForegroundColor Cyan
Write-Host "成功: $successCount / $totalCount" -ForegroundColor $(if ($successCount -eq $totalCount) { "Green" } else { "Yellow" })

if ($successCount -eq $totalCount) {
    Write-Host ""
    Write-Host "✅ 所有服务健康！可以执行性能测试" -ForegroundColor Green
    Write-Host ""
    Write-Host "下一步: cd performance-tests; .\run-test.ps1" -ForegroundColor Cyan
    exit 0
} elseif ($successCount -ge $totalCount * 0.8) {
    Write-Host ""
    Write-Host "⚠️  大部分服务健康，建议等待 30 秒后重试" -ForegroundColor Yellow
    exit 1
} else {
    Write-Host ""
    Write-Host "❌ 多个服务未就绪，请检查 Docker 容器状态" -ForegroundColor Red
    Write-Host "运行: docker ps" -ForegroundColor Gray
    exit 1
}
