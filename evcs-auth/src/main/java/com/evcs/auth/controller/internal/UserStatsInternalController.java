package com.evcs.auth.controller.internal;

import com.evcs.auth.dto.UserStatsProjection;
import com.evcs.auth.service.UserStatsService;
import com.evcs.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户统计内部查询端点（服务间调用）。
 *
 * <p>供 tenant 仪表盘使用：网关在边缘封锁 /internal/api/**，
 * 服务内由 InternalApiTokenFilter 校验共享令牌。
 */
@RestController
@RequestMapping("/internal/api/v1/stats/users")
@RequiredArgsConstructor
public class UserStatsInternalController {

    private final UserStatsService userStatsService;

    @GetMapping("/active-count")
    public Result<Map<String, Long>> activeCount(@RequestParam("tenantIds") List<Long> tenantIds) {
        return Result.success(Map.of("count", userStatsService.countActiveUsers(tenantIds)));
    }

    @GetMapping("/usernames")
    public Result<List<UserStatsProjection>> usernames(@RequestParam("ids") List<Long> ids) {
        return Result.success(userStatsService.getUsernamesByIds(ids));
    }
}
