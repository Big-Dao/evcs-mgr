# 批量构建所有服务镜像
# 使用本地已构建的 JAR 文件

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "批量构建 EVCS 服务镜像" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{Name="gateway"; Module="evcs-gateway"; Port=8080},
    @{Name="auth-service"; Module="evcs-auth"; Port=8081},
    @{Name="tenant-service"; Module="evcs-tenant"; Port=8086},
    @{Name="station-service"; Module="evcs-station"; Port=8082},
    @{Name="order-service"; Module="evcs-order"; Port=8083},
    @{Name="payment-service"; Module="evcs-payment"; Port=8084},
    @{Name="protocol-service"; Module="evcs-protocol"; Port=8085},
    @{Name="monitoring-service"; Module="evcs-monitoring"; Port=8087}
)

$successCount = 0
$failCount = 0
$failedServices = @()

foreach ($service in $services) {
    $moduleName = $service.Module
    $serviceName = $service.Name
    $imageName = "evcs-mgr-$serviceName"
    
    Write-Host "[$($successCount + $failCount + 1)/$($services.Count)] 构建 $serviceName..." -ForegroundColor Yellow
    
    # 检查 JAR 是否存在
    $jarPath = "$moduleName/build/libs/$moduleName-*.jar"
    if (-not (Test-Path $jarPath)) {
        Write-Host "  ❌ JAR 文件不存在: $jarPath" -ForegroundColor Red
        $failCount++
        $failedServices += $serviceName
        continue
    }
    
    # 构建镜像
    $buildCmd = "docker build -t ${imageName}:latest -f $moduleName/Dockerfile.simple $moduleName"
    Write-Host "  执行: $buildCmd" -ForegroundColor Gray
    
    try {
        Invoke-Expression $buildCmd 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  ✅ 成功构建 $serviceName" -ForegroundColor Green
            $successCount++
        } else {
            Write-Host "  ❌ 构建失败 $serviceName (退出码: $LASTEXITCODE)" -ForegroundColor Red
            $failCount++
            $failedServices += $serviceName
        }
    } catch {
        Write-Host "  ❌ 构建异常 $serviceName : $_" -ForegroundColor Red
        $failCount++
        $failedServices += $serviceName
    }
    
    Write-Host ""
}

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "构建完成!" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "成功: $successCount / $($services.Count)" -ForegroundColor Green
Write-Host "失败: $failCount / $($services.Count)" -ForegroundColor Red

if ($failCount -gt 0) {
    Write-Host ""
    Write-Host "失败的服务:" -ForegroundColor Red
    foreach ($failed in $failedServices) {
        Write-Host "  - $failed" -ForegroundColor Red
    }
}

Write-Host ""
if ($successCount -eq $services.Count) {
    Write-Host "🎉 所有镜像构建成功! 可以运行 'docker compose up -d' 启动所有服务" -ForegroundColor Green
} elseif ($successCount -gt 0) {
    Write-Host "⚠️  部分镜像构建成功,请检查失败的服务" -ForegroundColor Yellow
} else {
    Write-Host "❌ 所有镜像构建失败,请检查错误信息" -ForegroundColor Red
}
