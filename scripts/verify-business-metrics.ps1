# 业务监控指标验证脚本

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "EVCS 业务监控指标验证工具" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 配置
$services = @(
    @{Name="Order Service"; Port=8083; Module="evcs-order"}
    @{Name="Station Service"; Port=8082; Module="evcs-station"}
    @{Name="Payment Service"; Port=8084; Module="evcs-payment"}
)

$metricsToCheck = @{
    "evcs-order" = @(
        "evcs_order_created_total",
        "evcs_order_active_current",
        "evcs_order_creation_duration"
    )
    "evcs-station" = @(
        "evcs_charger_online",
        "evcs_charger_total",
        "evcs_charger_heartbeat_received_total"
    )
    "evcs-payment" = @(
        "evcs_payment_success_total",
        "evcs_payment_amount_total",
        "evcs_payment_process_duration"
    )
}

# 函数：检查服务健康状态
function Test-ServiceHealth {
    param($ServiceName, $Port)
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$Port/actuator/health" -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "[✓] $ServiceName 运行正常 (http://localhost:$Port)" -ForegroundColor Green
            return $true
        }
    } catch {
        Write-Host "[✗] $ServiceName 未运行或无响应 (http://localhost:$Port)" -ForegroundColor Red
        Write-Host "    错误: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
    return $false
}

# 函数：检查 Prometheus 指标
function Test-PrometheusMetrics {
    param($ServiceName, $Port, $Module)
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$Port/actuator/prometheus" -TimeoutSec 5 -ErrorAction Stop
        $content = $response.Content
        
        $metricsFound = 0
        $metricsTotal = $metricsToCheck[$Module].Count
        
        Write-Host ""
        Write-Host "  检查指标:" -ForegroundColor Cyan
        
        foreach ($metric in $metricsToCheck[$Module]) {
            if ($content -match $metric) {
                Write-Host "    [✓] $metric" -ForegroundColor Green
                $metricsFound++
                
                # 提取指标值
                if ($content -match "$metric\{.*?\}\s+([\d.]+)") {
                    $value = $Matches[1]
                    Write-Host "        当前值: $value" -ForegroundColor Gray
                }
            } else {
                Write-Host "    [✗] $metric (未找到)" -ForegroundColor Red
            }
        }
        
        Write-Host ""
        Write-Host "  指标覆盖率: $metricsFound/$metricsTotal" -ForegroundColor $(if ($metricsFound -eq $metricsTotal) { "Green" } else { "Yellow" })
        
        return $metricsFound -eq $metricsTotal
    } catch {
        Write-Host "  [✗] 无法获取 Prometheus 指标" -ForegroundColor Red
        Write-Host "      错误: $($_.Exception.Message)" -ForegroundColor Yellow
        return $false
    }
}

# 函数：测试 Grafana 连接
function Test-GrafanaConnection {
    Write-Host ""
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "检查 Grafana 服务" -ForegroundColor Cyan
    Write-Host "======================================" -ForegroundColor Cyan
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:3000/api/health" -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "[✓] Grafana 运行正常 (http://localhost:3000)" -ForegroundColor Green
            Write-Host ""
            Write-Host "    Dashboard 导入步骤:" -ForegroundColor Cyan
            Write-Host "    1. 访问 http://localhost:3000" -ForegroundColor Gray
            Write-Host "    2. 点击 '+' → 'Import'" -ForegroundColor Gray
            Write-Host "    3. 上传文件: monitoring/grafana/dashboards/business-metrics.json" -ForegroundColor Gray
            return $true
        }
    } catch {
        Write-Host "[✗] Grafana 未运行 (http://localhost:3000)" -ForegroundColor Yellow
        Write-Host "    提示: 使用 docker-compose 启动 Grafana 服务" -ForegroundColor Yellow
        return $false
    }
    return $false
}

# 主执行流程
Write-Host "开始验证业务监控指标..." -ForegroundColor Cyan
Write-Host ""

$allServicesHealthy = $true
$allMetricsAvailable = $true

foreach ($service in $services) {
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "检查 $($service.Name)" -ForegroundColor Cyan
    Write-Host "======================================" -ForegroundColor Cyan
    
    $isHealthy = Test-ServiceHealth -ServiceName $service.Name -Port $service.Port
    
    if ($isHealthy) {
        $hasMetrics = Test-PrometheusMetrics -ServiceName $service.Name -Port $service.Port -Module $service.Module
        if (-not $hasMetrics) {
            $allMetricsAvailable = $false
        }
    } else {
        $allServicesHealthy = $false
        $allMetricsAvailable = $false
    }
    
    Write-Host ""
}

# 检查 Grafana
$grafanaAvailable = Test-GrafanaConnection

# 总结
Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "验证总结" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

if ($allServicesHealthy -and $allMetricsAvailable) {
    Write-Host "[✓] 所有服务运行正常且监控指标可用" -ForegroundColor Green
} elseif ($allServicesHealthy) {
    Write-Host "[!] 服务运行正常但部分监控指标缺失" -ForegroundColor Yellow
} else {
    Write-Host "[✗] 部分服务未运行" -ForegroundColor Red
}

if ($grafanaAvailable) {
    Write-Host "[✓] Grafana 可用" -ForegroundColor Green
} else {
    Write-Host "[!] Grafana 未运行（可选）" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "快速测试命令" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "# 查看所有 EVCS 业务指标:" -ForegroundColor Gray
Write-Host 'curl http://localhost:8083/actuator/prometheus | Select-String "evcs_"' -ForegroundColor Yellow
Write-Host ""
Write-Host "# 查看特定指标详情:" -ForegroundColor Gray
Write-Host "curl http://localhost:8083/actuator/metrics/evcs.order.created.total | ConvertFrom-Json | ConvertTo-Json -Depth 10" -ForegroundColor Yellow
Write-Host ""
Write-Host "# 访问 Grafana Dashboard:" -ForegroundColor Gray
Write-Host "Start-Process http://localhost:3000" -ForegroundColor Yellow
Write-Host ""

# 退出码
if ($allServicesHealthy -and $allMetricsAvailable) {
    exit 0
} else {
    exit 1
}
