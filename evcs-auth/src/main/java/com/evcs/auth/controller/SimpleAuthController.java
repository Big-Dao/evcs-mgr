package com.evcs.auth.controller;

import com.evcs.auth.controller.dto.LoginRequest;
import com.evcs.auth.entity.SysUser;
import com.evcs.auth.service.ISysUserService;
import com.evcs.common.result.Result;
import com.evcs.common.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SimpleAuthController {

    private final ISysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expire:7200}")
    private Long jwtExpireSeconds;

    @GetMapping("/test")
    public String test() {
        return "SimpleAuthController is working!";
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = userService.getByUsername(request.getUsername(), request.getTenantId());
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            return Result.failure(401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.failure(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());
        List<String> roles = userService.listRoleCodes(user.getId());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("tenantId", user.getTenantId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("status", user.getStatus());
        userInfo.put("userType", user.getUserType());
        if (!CollectionUtils.isEmpty(roles)) {
            userInfo.put("roles", roles);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", jwtExpireSeconds);
        data.put("user", userInfo);

        Instant expiresAt = jwtUtil.getExpiration(token);
        if (expiresAt != null) {
            long seconds = Math.max(0, ChronoUnit.SECONDS.between(Instant.now(), expiresAt));
            data.put("expiresIn", seconds);
        }

        return Result.success("登录成功", data);
    }
}
