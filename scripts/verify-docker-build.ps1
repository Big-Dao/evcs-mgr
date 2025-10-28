# Docker 构建验证脚本

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Docker 模块构建验证工具" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 需要测试的服务模块
$services = @(
    @{Name="Protocol Service"; Module="evcs-protocol"; DockerService="protocol-service"}
    @{Name="Station Service"; Module="evcs-station"; DockerService="station-service"}
    @{Name="Order Service"; Module="evcs-order"; DockerService="order-service"}
)

# 函数：测试 Gradle 构建
function Test-GradleBuild {
    param($Module)
    
    Write-Host "测试 Gradle 构建: $Module" -ForegroundColor Cyan
    
    try {
        $result = & .\gradlew ":$Module:build" -x test 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[✓] Gradle 构建成功" -ForegroundColor Green
            return $true
        } else {
            Write-Host "[✗] Gradle 构建失败" -ForegroundColor Red
            Write-Host "    错误信息:" -ForegroundColor Yellow
            $result | Select-String "error:" | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
            return $false
        }
    } catch {
        Write-Host "[✗] Gradle 构建异常: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# 函数：测试 Docker 构建（快速验证，仅编译阶段）
function Test-DockerBuild {
    param($ServiceName, $DockerService)
    
    Write-Host "测试 Docker 构建: $ServiceName" -ForegroundColor Cyan
    
    try {
        # 仅构建到 build 阶段，不构建完整镜像
        $result = docker-compose build --no-cache $DockerService 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[✓] Docker 构建成功" -ForegroundColor Green
            return $true
        } else {
            Write-Host "[✗] Docker 构建失败" -ForegroundColor Red
            
            # 提取编译错误信息
            $errors = $result | Select-String "error:"
            if ($errors.Count -gt 0) {
                Write-Host "    编译错误:" -ForegroundColor Yellow
                $errors | Select-Object -First 5 | ForEach-Object { 
                    Write-Host "    $_" -ForegroundColor Red 
                }
                if ($errors.Count -gt 5) {
                    Write-Host "    ... 还有 $($errors.Count - 5) 个错误" -ForegroundColor Yellow
                }
            }
            
            return $false
        }
    } catch {
        Write-Host "[✗] Docker 构建异常: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# 主执行流程
Write-Host "开始验证模块构建..." -ForegroundColor Cyan
Write-Host ""

$allPassed = $true
$results = @()

foreach ($service in $services) {
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "验证: $($service.Name)" -ForegroundColor Cyan
    Write-Host "======================================" -ForegroundColor Cyan
    
    # 测试 Gradle 构建
    $gradleSuccess = Test-GradleBuild -Module $service.Module
    
    # 如果 Gradle 成功，再测试 Docker 构建
    $dockerSuccess = $false
    if ($gradleSuccess) {
        Write-Host ""
        $dockerSuccess = Test-DockerBuild -ServiceName $service.Name -DockerService $service.DockerService
    } else {
        Write-Host "[!] 跳过 Docker 构建（Gradle 构建失败）" -ForegroundColor Yellow
    }
    
    $results += @{
        Service = $service.Name
        GradleSuccess = $gradleSuccess
        DockerSuccess = $dockerSuccess
    }
    
    if (-not ($gradleSuccess -and $dockerSuccess)) {
        $allPassed = $false
    }
    
    Write-Host ""
}

# 生成总结报告
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "验证总结" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "模块构建状态:" -ForegroundColor Cyan
Write-Host ""
Write-Host "| 服务                | Gradle | Docker |" -ForegroundColor Gray
Write-Host "|---------------------|--------|--------|" -ForegroundColor Gray

foreach ($result in $results) {
    $gradleStatus = if ($result.GradleSuccess) { "✓" } else { "✗" }
    $dockerStatus = if ($result.DockerSuccess) { "✓" } else { "✗" }
    
    $gradleColor = if ($result.GradleSuccess) { "Green" } else { "Red" }
    $dockerColor = if ($result.DockerSuccess) { "Green" } else { "Red" }
    
    Write-Host "| " -NoNewline -ForegroundColor Gray
    Write-Host $($result.Service).PadRight(19) -NoNewline -ForegroundColor Gray
    Write-Host "| " -NoNewline -ForegroundColor Gray
    Write-Host $gradleStatus.PadRight(7) -NoNewline -ForegroundColor $gradleColor
    Write-Host "| " -NoNewline -ForegroundColor Gray
    Write-Host $dockerStatus.PadRight(7) -NoNewline -ForegroundColor $dockerColor
    Write-Host "|" -ForegroundColor Gray
}

Write-Host ""

if ($allPassed) {
    Write-Host "[✓] 所有模块构建验证通过" -ForegroundColor Green
    Write-Host ""
    Write-Host "建议:" -ForegroundColor Cyan
    Write-Host "  1. 可以安全地推送代码到 Git 仓库" -ForegroundColor Gray
    Write-Host "  2. CI/CD 流水线应该能够成功构建" -ForegroundColor Gray
    Write-Host "  3. Docker 镜像可以正常部署" -ForegroundColor Gray
} else {
    Write-Host "[✗] 部分模块构建失败，需要修复" -ForegroundColor Red
    Write-Host ""
    Write-Host "建议:" -ForegroundColor Cyan
    Write-Host "  1. 检查失败模块的依赖配置" -ForegroundColor Gray
    Write-Host "  2. 查看完整的错误日志" -ForegroundColor Gray
    Write-Host "  3. 参考文档: docs/development/DOCKER-BUILD-FIX.md" -ForegroundColor Gray
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "快速命令参考" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "# 构建单个模块:" -ForegroundColor Gray
Write-Host '.\gradlew :evcs-station:build -x test' -ForegroundColor Yellow
Write-Host ""
Write-Host "# 构建所有模块:" -ForegroundColor Gray
Write-Host '.\gradlew build -x test' -ForegroundColor Yellow
Write-Host ""
Write-Host "# 构建 Docker 镜像:" -ForegroundColor Gray
Write-Host 'docker-compose build station-service' -ForegroundColor Yellow
Write-Host ""
Write-Host "# 查看模块依赖关系:" -ForegroundColor Gray
Write-Host '.\gradlew :evcs-station:dependencies' -ForegroundColor Yellow
Write-Host ""

# 退出码
if ($allPassed) {
    exit 0
} else {
    exit 1
}
