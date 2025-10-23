# JMeter 快速验证测试 - 低负载短时测试
# 用途: 验证环境配置是否正确，不会对系统造成压力

param(
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "=== JMeter 快速验证测试 ===" -ForegroundColor Cyan
Write-Host ""

# 创建临时测试计划
$TempTestPlan = "quick-verify-test.jmx"

$QuickTestContent = @'
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="快速验证测试" enabled="true">
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.name">BASE_URL</stringProp>
            <stringProp name="Argument.value">http://localhost:8080</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ConfigTestElement guiclass="HttpDefaultsGui" testclass="ConfigTestElement" testname="HTTP默认配置" enabled="true">
        <stringProp name="HTTPSampler.domain">localhost</stringProp>
        <stringProp name="HTTPSampler.port">8080</stringProp>
        <stringProp name="HTTPSampler.protocol">http</stringProp>
        <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
        <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
      </ConfigTestElement>
      <hashTree/>
      
      <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP头管理" enabled="true">
        <collectionProp name="HeaderManager.headers">
          <elementProp name="" elementType="Header">
            <stringProp name="Header.name">Content-Type</stringProp>
            <stringProp name="Header.value">application/json</stringProp>
          </elementProp>
          <elementProp name="" elementType="Header">
            <stringProp name="Header.name">X-Tenant-Id</stringProp>
            <stringProp name="Header.value">1</stringProp>
          </elementProp>
        </collectionProp>
      </HeaderManager>
      <hashTree/>
      
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="验证线程组" enabled="true">
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">5</stringProp>
        </elementProp>
        <stringProp name="ThreadGroup.num_threads">5</stringProp>
        <stringProp name="ThreadGroup.ramp_time">5</stringProp>
        <boolProp name="ThreadGroup.scheduler">false</boolProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET /orders/page" enabled="true">
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="page" elementType="HTTPArgument">
                <stringProp name="Argument.name">page</stringProp>
                <stringProp name="Argument.value">1</stringProp>
                <boolProp name="HTTPArgument.use_equals">true</boolProp>
              </elementProp>
              <elementProp name="size" elementType="HTTPArgument">
                <stringProp name="Argument.name">size</stringProp>
                <stringProp name="Argument.value">10</stringProp>
                <boolProp name="HTTPArgument.use_equals">true</boolProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <stringProp name="HTTPSampler.path">/orders/page</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
          <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
        </HTTPSamplerProxy>
        <hashTree/>
      </hashTree>
      
      <ResultCollector guiclass="SummaryReport" testclass="ResultCollector" testname="Summary Report" enabled="true">
        <boolProp name="ResultCollector.error_logging">false</boolProp>
        <objProp>
          <name>saveConfig</name>
          <value class="SampleSaveConfiguration">
            <time>true</time>
            <latency>true</latency>
            <timestamp>true</timestamp>
            <success>true</success>
            <label>true</label>
            <code>true</code>
            <message>true</message>
          </value>
        </objProp>
        <stringProp name="filename">results/quick-verify.jtl</stringProp>
      </ResultCollector>
      <hashTree/>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
'@

# 保存临时测试计划
$QuickTestContent | Out-File -FilePath $TempTestPlan -Encoding UTF8

Write-Host "执行快速验证测试 (5用户 x 5次循环 = 25请求)..." -ForegroundColor Yellow
Write-Host ""

# 执行测试
jmeter -n -t $TempTestPlan -l results/quick-verify.jtl -JBASE_URL=$BaseUrl

Write-Host ""
Write-Host "=== 验证完成 ===" -ForegroundColor Green
Write-Host ""

# 分析结果
if (Test-Path "results/quick-verify.jtl") {
    $Results = Get-Content "results/quick-verify.jtl" | Select-Object -Skip 1
    $TotalRequests = $Results.Count
    $SuccessRequests = ($Results | Where-Object { $_ -match ",200," }).Count
    
    Write-Host "总请求数: $TotalRequests" -ForegroundColor Gray
    Write-Host "成功请求: $SuccessRequests" -ForegroundColor Green
    Write-Host "失败请求: $($TotalRequests - $SuccessRequests)" -ForegroundColor $(if ($TotalRequests -eq $SuccessRequests) { "Gray" } else { "Red" })
    
    if ($TotalRequests -eq $SuccessRequests -and $TotalRequests -gt 0) {
        Write-Host ""
        Write-Host "✅ 环境验证成功！可以执行完整性能测试" -ForegroundColor Green
        Write-Host ""
        Write-Host "执行完整测试: .\run-test.ps1" -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "⚠️  环境验证失败，请检查:" -ForegroundColor Yellow
        Write-Host "1. Docker 服务是否运行" -ForegroundColor Gray
        Write-Host "2. EVCS 服务是否启动完成" -ForegroundColor Gray
        Write-Host "3. 端口 8080 是否可访问" -ForegroundColor Gray
    }
}

# 清理临时文件
Remove-Item $TempTestPlan -ErrorAction SilentlyContinue
