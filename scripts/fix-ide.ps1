# EVCS Manager - IDE 修复脚本
# 用于修复 VS Code Java 扩展的编译错误问题

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "EVCS Manager - IDE 修复脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 清理 Gradle 缓存
Write-Host "[步骤 1/6] 清理 Gradle 缓存..." -ForegroundColor Yellow
./gradlew clean --refresh-dependencies
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Gradle 缓存清理成功" -ForegroundColor Green
} else {
    Write-Host "✗ Gradle 缓存清理失败" -ForegroundColor Red
}
Write-Host ""

# 2. 清理 bin 目录
Write-Host "[步骤 2/6] 清理 bin 目录..." -ForegroundColor Yellow
Get-ChildItem -Path . -Filter "bin" -Recurse -Directory -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "✓ bin 目录清理完成" -ForegroundColor Green
Write-Host ""

# 3. 清理 Eclipse 配置文件
Write-Host "[步骤 3/6] 清理 Eclipse 配置文件..." -ForegroundColor Yellow
Get-ChildItem -Path . -Filter ".classpath" -Recurse -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path . -Filter ".project" -Recurse -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path . -Filter ".settings" -Recurse -Directory -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "✓ Eclipse 配置文件清理完成" -ForegroundColor Green
Write-Host ""

# 4. 重新构建项目
Write-Host "[步骤 4/6] 重新构建项目..." -ForegroundColor Yellow
./gradlew build -x test
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ 项目构建成功" -ForegroundColor Green
} else {
    Write-Host "✗ 项目构建失败" -ForegroundColor Red
}
Write-Host ""

# 5. 清理 Java Language Server 缓存
Write-Host "[步骤 5/6] 清理 Java Language Server 缓存..." -ForegroundColor Yellow
$javaServerCache = "$env:USERPROFILE\.vscode\extensions\redhat.java-*\server\config_*"
if (Test-Path $javaServerCache) {
    Remove-Item -Path $javaServerCache -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "✓ Java Language Server 缓存清理完成" -ForegroundColor Green
} else {
    Write-Host "○ 未找到 Java Language Server 缓存" -ForegroundColor Gray
}
Write-Host ""

# 6. 提示手动操作
Write-Host "[步骤 6/6] 完成后续操作..." -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "修复完成！请执行以下手动操作：" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 在 VS Code 中按 Ctrl+Shift+P" -ForegroundColor White
Write-Host "2. 输入并选择: 'Java: Clean Java Language Server Workspace'" -ForegroundColor White
Write-Host "3. 选择 'Reload and delete'" -ForegroundColor White
Write-Host "4. 等待 VS Code 重新加载" -ForegroundColor White
Write-Host "5. 等待 Java 项目同步完成（查看右下角进度）" -ForegroundColor White
Write-Host ""
Write-Host "如果问题仍然存在：" -ForegroundColor Yellow
Write-Host "- 按 Ctrl+Shift+P，选择 'Java: Force Java Compilation - Full'" -ForegroundColor White
Write-Host "- 重启 VS Code" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
