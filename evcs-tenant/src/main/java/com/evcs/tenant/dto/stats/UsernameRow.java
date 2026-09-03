package com.evcs.tenant.dto.stats;

/**
 * 用户名投影（来自 auth 服务内部 API 的数据）。
 */
public record UsernameRow(
        Long userId,
        String username
) {
}
