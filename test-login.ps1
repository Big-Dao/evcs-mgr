# 测试登录脚本
param(
    [string]$username = "admin",
    [string]$password = "password",
    [string]$tenantCode = "SYSTEM"
)

$bodyObj = @{
    username = $username
    password = $password
    tenantCode = $tenantCode
}

Write-Host "Testing login for user: $username, tenant: $tenantCode"

try {
    # 使用-Body直接传递对象，PowerShell会自动转换为JSON
    $response = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method POST -Body ($bodyObj | ConvertTo-Json) -ContentType "application/json"
    
    if ($response.success) {
        Write-Host "✓ Login successful!" -ForegroundColor Green
        Write-Host "Token: $($response.data.accessToken.Substring(0, 30))..."
        return $response.data.accessToken
    } else {
        Write-Host "✗ Login failed: $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Error: $_" -ForegroundColor Red
    Write-Host "Response: $($_.ErrorDetails.Message)" -ForegroundColor Yellow
}
