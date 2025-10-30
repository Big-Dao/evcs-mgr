# 测试所有新实现的API
Write-Host "=== 开始测试所有新API ===" -ForegroundColor Cyan

# 1. 登录获取token
Write-Host "`n[1/4] 测试登录..." -ForegroundColor Yellow
$loginBody = @{username="admin"; password="password"; tenantCode="SYSTEM"} | ConvertTo-Json
try {
    $loginResp = Invoke-RestMethod -Uri http://localhost:8080/auth/login -Method Post -Body $loginBody -ContentType 'application/json'
    if ($loginResp.success) {
        $token = $loginResp.data.accessToken
        Write-Host "✓ 登录成功! 用户: $($loginResp.data.user.realName)" -ForegroundColor Green
    } else {
        Write-Host "✗ 登录失败: $($loginResp.message)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ 登录失败: $($_.ErrorDetails.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{Authorization="Bearer $token"}

# 2. 测试角色列表API
Write-Host "`n[2/4] 测试角色列表API..." -ForegroundColor Yellow
try {
    $roleResp = Invoke-RestMethod -Uri http://localhost:8080/auth/role/list -Method Get -Headers $headers
    if ($roleResp.success) {
        Write-Host "✓ 成功! 返回 $($roleResp.data.Count) 个角色:" -ForegroundColor Green
        $roleResp.data | Format-Table -Property roleName,roleCode,status -AutoSize
    } else {
        Write-Host "✗ 失败: $($roleResp.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 失败: $_" -ForegroundColor Red
}

# 3. 测试菜单列表API
Write-Host "`n[3/4] 测试菜单列表API..." -ForegroundColor Yellow
try {
    $menuResp = Invoke-RestMethod -Uri http://localhost:8080/auth/menu/list -Method Get -Headers $headers
    if ($menuResp.success) {
        Write-Host "✓ 成功! 返回 $($menuResp.data.Count) 个菜单项 (显示前5个):" -ForegroundColor Green
        $menuResp.data | Select-Object -First 5 | Format-Table -Property menuName,menuType,visible -AutoSize
    } else {
        Write-Host "✗ 失败: $($menuResp.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 失败: $_" -ForegroundColor Red
}

# 4. 测试Dashboard站点排名API
Write-Host "`n[4/4] 测试Dashboard站点排名API..." -ForegroundColor Yellow
try {
    $rankResp = Invoke-RestMethod -Uri http://localhost:8080/dashboard/station-ranking -Method Get -Headers $headers
    if ($rankResp.success) {
        Write-Host "✓ 成功! 返回 $($rankResp.data.Count) 个站点:" -ForegroundColor Green
        $rankResp.data | Format-Table -Property stationName,orderCount,percentage -AutoSize
    } else {
        Write-Host "✗ 失败: $($rankResp.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 所有测试完成 ===" -ForegroundColor Cyan
