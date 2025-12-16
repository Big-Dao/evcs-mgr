# JMeter 测试执行脚本
# 用途: 快速执行 JVM 性能基线测试

param(
    [string]$Scenario = "all",
    [string]$BaseUrl = "http://192.168.20.235:30080",
    [int]$Duration = 600,
    [int]$ConnectTimeoutMs = 5000,
    [int]$ResponseTimeoutMs = 30000,
    [switch]$NoPrompt
)

Write-Host "=== EVCS JVM 性能测试 ===" -ForegroundColor Cyan
Write-Host ""

# 解析 BaseUrl -> HOST/PORT/PROTOCOL（JMX 使用 -JHOST/-JPORT/-JPROTOCOL）
try {
    $uri = [System.Uri]$BaseUrl
    $HostName = $uri.Host
    $Port = $uri.Port
    $Protocol = $uri.Scheme
} catch {
    Write-Host "❌ 无效的 URL: $BaseUrl" -ForegroundColor Red
    exit 1
}

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
$TestPlan = Join-Path $PSScriptRoot "jvm-tuning-test.jmx"
$ResultsDir = "results"
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$ReportDir = "$ResultsDir/report-$Timestamp"

Write-Host ""
Write-Host "=== 测试配置 ===" -ForegroundColor Cyan
Write-Host "测试计划: $TestPlan" -ForegroundColor Gray
Write-Host "目标地址: $BaseUrl" -ForegroundColor Gray
Write-Host "测试场景: $Scenario" -ForegroundColor Gray
Write-Host "测试时长: $Duration 秒" -ForegroundColor Gray
Write-Host "连接超时: $ConnectTimeoutMs ms" -ForegroundColor Gray
Write-Host "响应超时: $ResponseTimeoutMs ms" -ForegroundColor Gray
Write-Host "报告目录: $ReportDir" -ForegroundColor Gray
Write-Host ""

# 等待用户确认
if (-not $NoPrompt) {
    Write-Host "按任意键开始测试，或 Ctrl+C 取消..." -ForegroundColor Yellow
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

Write-Host ""
Write-Host "=== 开始执行测试 ===" -ForegroundColor Cyan

# 执行 JMeter 测试
$JMeterCmd = @(
    "-n",  # 非 GUI 模式
    "-t", $TestPlan,  # 测试计划文件
    "-l", "$ResultsDir/results-$Timestamp.jtl",  # 结果文件
    "-e",  # 生成报告
    "-o", $ReportDir,  # 报告输出目录
    "-JBASE_URL=$BaseUrl",  # 保留（兼容旧逻辑/未来扩展）
    "-JHOST=$HostName",
    "-JPORT=$Port",
    "-JPROTOCOL=$Protocol",
    "-JCONNECT_TIMEOUT_MS=$ConnectTimeoutMs",
    "-JRESPONSE_TIMEOUT_MS=$ResponseTimeoutMs"
)

# 根据场景控制各线程组持续时间/爬坡时间（JMX 使用 __P(duration_*/ramp_*)）
$durationS1 = 1
$durationS2 = 1
$durationS3 = 1
$rampS1 = 1
$rampS2 = 1
$rampS3 = 1

$durationS3Default = [int][math]::Max(60, [math]::Round($Duration / 2.0))

switch ($Scenario) {
    "scenario1" {
        Write-Host "仅执行场景1: 订单创建 (500 TPS)" -ForegroundColor Yellow
        $durationS1 = $Duration
        $rampS1 = [int][math]::Max(1, [math]::Min(60, [math]::Round($Duration / 6.0)))
    }
    "scenario2" {
        Write-Host "仅执行场景2: 订单查询 (1000 TPS)" -ForegroundColor Yellow
        $durationS2 = $Duration
        $rampS2 = [int][math]::Max(1, [math]::Min(60, [math]::Round($Duration / 6.0)))
    }
    "scenario3" {
        Write-Host "仅执行场景3: 状态更新 (2000 TPS)" -ForegroundColor Yellow
        $durationS3 = $Duration
        $rampS3 = [int][math]::Max(1, [math]::Min(60, [math]::Round($Duration / 6.0)))
    }
    default {
        Write-Host "执行全部场景" -ForegroundColor Yellow
        $durationS1 = $Duration
        $durationS2 = $Duration
        $durationS3 = $durationS3Default
        $rampS1 = 60
        $rampS2 = 60
        $rampS3 = 60
    }
}

# 根据场景控制各线程组并发数（JMX 使用 __P(threads_*)）
$threadsS1 = 0
$threadsS2 = 0
$threadsS3 = 0

switch ($Scenario) {
    "scenario1" { $threadsS1 = 100 }
    "scenario2" { $threadsS2 = 200 }
    "scenario3" { $threadsS3 = 500 }
    default {
        $threadsS1 = 100
        $threadsS2 = 200
        $threadsS3 = 500
    }
}

$JMeterCmd += "-Jduration_s1=$durationS1", "-Jduration_s2=$durationS2", "-Jduration_s3=$durationS3"
$JMeterCmd += "-Jramp_s1=$rampS1", "-Jramp_s2=$rampS2", "-Jramp_s3=$rampS3"
$JMeterCmd += "-Jthreads_s1=$threadsS1", "-Jthreads_s2=$threadsS2", "-Jthreads_s3=$threadsS3"

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
    if ($IsWindows) {
        Write-Host "正在打开 HTML 报告..." -ForegroundColor Yellow
        Start-Process $ReportPath
    } else {
        Write-Host "HTML 报告已生成: $ReportPath" -ForegroundColor Gray
        Write-Host "（Linux/WSL 环境不自动打开浏览器）" -ForegroundColor Gray
    }
} else {
    Write-Host "⚠️  报告生成失败，请检查测试日志" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "下一步:" -ForegroundColor Cyan
Write-Host "1. 查看 HTML 报告分析性能指标" -ForegroundColor Gray
Write-Host "2. 检查 JFR 文件: logs/jfr/*.jfr" -ForegroundColor Gray
Write-Host "3. 查看 Prometheus 指标: http://localhost:9090" -ForegroundColor Gray
Write-Host "4. 对比 GC 日志和响应时间，制定优化方案" -ForegroundColor Gray
