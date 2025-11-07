package com.evcs.auth.controller;

import com.evcs.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * 提供多种健康检查端点以支持不同的监控工具
 */
@Tag(name = "健康检查", description = "服务健康状态监控接口")
@RestController
public class HealthController {

    /**
     * 通用健康检查
     */
    @Operation(summary = "通用健康检查", description = "基础健康状态检查")
    @GetMapping
    public Result<String> health() {
        return Result.success("认证服务运行正常", "OK");
    }

    /**
     * 健康检查详细状态
     */
    @Operation(summary = "详细健康检查", description = "返回详细的健康状态信息")
    @GetMapping("/detailed")
    public Result<Object> detailedHealth() {
        return Result.success("认证服务运行正常",
            java.util.Map.of(
                "status", "UP",
                "service", "evcs-auth",
                "timestamp", System.currentTimeMillis()
            )
        );
    }

    /**
     * 存活性检查 (K8s风格)
     */
    @Operation(summary = "存活性检查", description = "Kubernetes存活性探针")
    @GetMapping("/live")
    public Result<String> liveness() {
        return Result.success("服务存活", "alive");
    }

    /**
     * 就绪性检查 (K8s风格)
     */
    @Operation(summary = "就绪性检查", description = "Kubernetes就绪性探针")
    @GetMapping("/ready")
    public Result<String> readiness() {
        return Result.success("服务就绪", "ready");
    }
}