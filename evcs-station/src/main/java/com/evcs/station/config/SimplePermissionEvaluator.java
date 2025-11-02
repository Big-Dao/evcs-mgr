package com.evcs.station.config;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 简单的权限评估器：只要用户已认证就放行。
 * 后续可扩展为基于角色/权限的精细控制。
 */
@Component
public class SimplePermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return isAuthenticated(authentication);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return isAuthenticated(authentication);
    }

    private boolean isAuthenticated(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.isAccountNonLocked() && userDetails.isEnabled();
        }
        return authentication.isAuthenticated();
    }
}
