package com.evcs.common.tenant;

/**
 * 租户层级校验器接口
 * 用于在Common层解耦具体的层级校验逻辑（通常需要查库）
 */
public interface HierarchyValidator {

    /**
     * 判断 targetTenantId 是否为 currentTenantId 的后代（子租户或更深层级）
     * 
     * @param currentTenantId 当前租户ID（上级）
     * @param targetTenantId  目标租户ID（可能的下级）
     * @return true 如果 target 是 current 的后代
     */
    boolean isDescendant(Long currentTenantId, Long targetTenantId);

    /**
     * 验证是否具有跨层访问权限
     * 
     * @param currentTenantId 当前租户
     * @param targetTenantId  目标租户
     * @return true if valid
     */
    default boolean validateAccess(Long currentTenantId, Long targetTenantId) {
        if (currentTenantId == null || targetTenantId == null)
            return false;
        if (currentTenantId.equals(targetTenantId))
            return true;
        return isDescendant(currentTenantId, targetTenantId);
    }
}
