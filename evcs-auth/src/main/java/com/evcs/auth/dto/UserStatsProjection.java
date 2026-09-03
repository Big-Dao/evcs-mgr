package com.evcs.auth.dto;

/**
 * 用户统计投影（内部 API 返回的最小字段集）。
 */
public record UserStatsProjection(
        Long userId,
        String username
) {
}
