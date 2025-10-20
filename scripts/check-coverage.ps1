# EVCS Manager - 测试覆盖率汇总脚本
# 用于查看各模块的测试覆盖率

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "EVCS Manager - 测试覆盖率汇总" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$modules = @(
    "evcs-auth",
    "evcs-common",
    "evcs-gateway",
    "evcs-integration",
    "evcs-order",
    "evcs-payment",
    "evcs-protocol",
    "evcs-station",
    "evcs-tenant"
)

$totalCoverage = @()

foreach ($module in $modules) {
    $reportPath = "$module\build\reports\jacoco\index.html"
    
    if (Test-Path $reportPath) {
        $content = Get-Content $reportPath -Raw
        
        # 提取覆盖率百分比（指令覆盖率）
        if ($content -match 'Total.*?(\d+)%') {
            $coverage = $matches[1]
            $totalCoverage += [PSCustomObject]@{
                Module = $module
                Coverage = [int]$coverage
            }
            
            # 根据覆盖率显示不同颜色
            if ([int]$coverage -ge 80) {
                Write-Host "✓ $module : " -NoNewline -ForegroundColor Green
                Write-Host "$coverage%" -ForegroundColor Green
            } elseif ([int]$coverage -ge 50) {
                Write-Host "○ $module : " -NoNewline -ForegroundColor Yellow
                Write-Host "$coverage%" -ForegroundColor Yellow
            } else {
                Write-Host "✗ $module : " -NoNewline -ForegroundColor Red
                Write-Host "$coverage%" -ForegroundColor Red
            }
        } else {
            Write-Host "? $module : 无法解析覆盖率" -ForegroundColor Gray
        }
    } else {
        Write-Host "- $module : 未找到报告" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

if ($totalCoverage.Count -gt 0) {
    $avgCoverage = ($totalCoverage | Measure-Object -Property Coverage -Average).Average
    
    Write-Host "平均覆盖率: " -NoNewline
    if ($avgCoverage -ge 80) {
        Write-Host "$([math]::Round($avgCoverage, 1))% ✓" -ForegroundColor Green
    } elseif ($avgCoverage -ge 50) {
        Write-Host "$([math]::Round($avgCoverage, 1))% ○" -ForegroundColor Yellow
    } else {
        Write-Host "$([math]::Round($avgCoverage, 1))% ✗" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "目标: 80% (P3阶段)" -ForegroundColor Cyan
    
    if ($avgCoverage -lt 80) {
        $gap = 80 - $avgCoverage
        Write-Host "差距: $([math]::Round($gap, 1))%" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "详细报告位置:" -ForegroundColor White
Write-Host ""

foreach ($module in $modules) {
    $reportPath = "$module\build\reports\jacoco\index.html"
    if (Test-Path $reportPath) {
        Write-Host "  $module : " -NoNewline -ForegroundColor Gray
        Write-Host "$reportPath" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "快速打开报告:" -ForegroundColor Yellow
Write-Host "  Invoke-Item <模块>\build\reports\jacoco\test\html\index.html" -ForegroundColor White
Write-Host ""
