package com.evcs.station.controller.internal;

import com.evcs.common.result.Result;
import com.evcs.station.dto.TenantUsageCount;
import com.evcs.station.service.StationUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户资源用量内部查询端点（服务间调用）。
 *
 * <p>供 tenant 服务配额校验使用：网关在边缘封锁 /internal/api/**，
 * 服务内由 InternalApiTokenFilter 校验共享令牌。
 */
@RestController
@RequestMapping("/internal/api/v1")
@RequiredArgsConstructor
public class UsageCountInternalController {

    private final StationUsageService stationUsageService;

    @GetMapping("/usage-counts")
    public Result<List<TenantUsageCount>> usageCounts(@RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(stationUsageService.getUsageCounts(tenantIds));
    }
}
