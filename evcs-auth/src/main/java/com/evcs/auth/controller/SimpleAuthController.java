package com.evcs.auth.controller;

import com.evcs.auth.controller.dto.LoginRequest;
import com.evcs.auth.controller.dto.LoginResponse;
import com.evcs.auth.service.IAuthService;
import com.evcs.common.result.Result;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SimpleAuthController {

    private final IAuthService authService;
    private final JwtUtil jwtUtil;

    @GetMapping("/test")
    public String test() {
        return "SimpleAuthController is working!";
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getIdentifier(), request.getPassword());
        return Result.success("登录成功", response);
    }

    @GetMapping("/userinfo")
    public Result<Map<String, Object>> userInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader
    ) {
        Long userId = parseLong(userIdHeader);
        Long tenantId = parseLong(tenantIdHeader);

        String token = extractBearerToken(authorization);
        if (token != null) {
            if (userId == null) {
                userId = jwtUtil.getUserId(token);
            }
            if (tenantId == null) {
                tenantId = jwtUtil.getTenantId(token);
            }
        }

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        if (userId != null) {
            TenantContext.setUserId(userId);
        }

        Map<String, Object> userInfo = authService.getUserInfo(userId, tenantId);
        return Result.success("查询成功", userInfo);
    }

    private static Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String prefix = "Bearer ";
        if (authorization.startsWith(prefix)) {
            String token = authorization.substring(prefix.length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }
        return null;
    }
}
