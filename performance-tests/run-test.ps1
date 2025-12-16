# JMeter 测试执行脚本
# 用途: 快速执行 JVM 性能基线测试

param(
    [string]$Scenario = "all",
    [string]$BaseUrl = "http://192.168.20.235:30080",
    [int]$Duration = 600
)

Write-Host "=== EVCS JVM 性能测试 ===" -ForegroundColor Cyan
Write-Host ""

# 检查 JMeter 是否可用
try {
    $jmeterVersion = jmeter --version 2>&1 | Select-String "Version"
    Write-Host "✅ JMeter 版本: $jmeterVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ JMeter 未安装或未配置到 PATH" -ForegroundColor Red
    exit 1
}

# 检查环境状态 (Docker 或 K8s)
Write-Host ""
Write-Host "检查环境状态..." -ForegroundColor Yellow

$envReady = $false

# 1. 尝试检查 K8s
if (Get-Command kubectl -ErrorAction SilentlyContinue) {
    $k8sPods = kubectl get pods -n evcs --field-selector=status.phase=Running 2>&1
    if ($LASTEXITCODE -eq 0 -and ($k8sPods | Select-String "evcs-")) {
        Write-Host "✅ K8s 环境检测到运行中的 Pod" -ForegroundColor Green
        $envReady = $true
    }
}

# 2. 如果 K8s 未就绪，检查 Docker
if (-not $envReady) {
    try {
        $dockerStatus = docker ps --format "{{.Names}}" 2>&1 | Select-String "evcs-"
        if ($dockerStatus) {
            Write-Host "✅ Docker 环境检测到运行中的容器" -ForegroundColor Green
            $envReady = $true
        }
    } catch {
        # Docker 可能未安装或未运行
    }
}

if (-not $envReady) {
    Write-Host "⚠️  未检测到运行中的 EVCS 服务 (Docker 或 K8s)" -ForegroundColor Yellow
    Write-Host "请确保环境已启动: " -ForegroundColor Gray
    Write-Host "  - K8s: ./k8s/deploy.sh" -ForegroundColor Gray
    Write-Host "  - Docker: docker-compose up -d" -ForegroundColor Gray
    # 不强制退出，允许用户尝试连接远程环境
    Write-Host "将尝试连接目标地址: $BaseUrl" -ForegroundColor Yellow
}

# 设置测试参数
$TestPlan = "jvm-tuning-test.jmx"
$ResultsDir = "results"
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$ReportDir = "$ResultsDir/report-$Timestamp"

Write-Host ""
Write-Host "=== 测试配置 ===" -ForegroundColor Cyan
Write-Host "测试计划: $TestPlan" -ForegroundColor Gray
Write-Host "目标地址: $BaseUrl" -ForegroundColor Gray
Write-Host "测试场景: $Scenario" -ForegroundColor Gray
Write-Host "测试时长: $Duration 秒" -ForegroundColor Gray
Write-Host "报告目录: $ReportDir" -ForegroundColor Gray
Write-Host ""

# 等待用户确认
Write-Host "按任意键开始测试，或 Ctrl+C 取消..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

Write-Host ""
Write-Host "=== 开始执行测试 ===" -ForegroundColor Cyan

# 执行 JMeter 测试
$JMeterCmd = @(
    "-n",  # 非 GUI 模式
    "-t", $TestPlan,  # 测试计划文件
    "-l", "$ResultsDir/results-$Timestamp.jtl",  # 结果文件
    "-e",  # 生成报告
    "-o", $ReportDir,  # 报告输出目录
    "-JBASE_URL=$BaseUrl"  # 设置基础 URL
)

# 根据场景禁用其他线程组
switch ($Scenario) {
    "scenario1" {
        Write-Host "仅执行场景1: 订单创建 (500 TPS)" -ForegroundColor Yellow
        $JMeterCmd += "-Jscenario2.enabled=false", "-Jscenario3.enabled=false"
    }
    "scenario2" {
        Write-Host "仅执行场景2: 订单查询 (1000 TPS)" -ForegroundColor Yellow
        $JMeterCmd += "-Jscenario1.enabled=false", "-Jscenario3.enabled=false"
    }
    "scenario3" {
        Write-Host "仅执行场景3: 状态更新 (2000 TPS)" -ForegroundColor Yellow
        $JMeterCmd += "-Jscenario1.enabled=false", "-Jscenario2.enabled=false"
    }
    default {
        Write-Host "执行全部场景" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "执行命令: jmeter $($JMeterCmd -join ' ')" -ForegroundColor Gray
Write-Host ""

# 执行测试
$StartTime = Get-Date
& jmeter $JMeterCmd

$EndTime = Get-Date
$ElapsedTime = ($EndTime - $StartTime).TotalSeconds

Write-Host ""
Write-Host "=== 测试完成 ===" -ForegroundColor Green
Write-Host "执行时长: $([math]::Round($ElapsedTime, 2)) 秒" -ForegroundColor Gray
Write-Host "结果文件: $ResultsDir/results-$Timestamp.jtl" -ForegroundColor Gray
Write-Host "HTML 报告: $ReportDir/index.html" -ForegroundColor Gray
Write-Host ""

# 自动打开报告
$ReportPath = Resolve-Path "$ReportDir/index.html" -ErrorAction SilentlyContinue
if ($ReportPath) {
    Write-Host "正在打开 HTML 报告..." -ForegroundColor Yellow
    Start-Process $ReportPath
} else {
    Write-Host "⚠️  报告生成失败，请检查测试日志" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "下一步:" -ForegroundColor Cyan
Write-Host "1. 查看 HTML 报告分析性能指标" -ForegroundColor Gray
Write-Host "2. 检查 JFR 文件: logs/jfr/*.jfr" -ForegroundColor Gray
Write-Host "3. 查看 Prometheus 指标: http://localhost:9090" -ForegroundColor Gray
Write-Host "4. 对比 GC 日志和响应时间，制定优化方案" -ForegroundColor Gray
