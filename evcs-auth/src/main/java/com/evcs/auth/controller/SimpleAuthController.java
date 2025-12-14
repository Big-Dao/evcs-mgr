package com.evcs.auth.controller;

import com.evcs.auth.controller.dto.LoginRequest;
import com.evcs.auth.controller.dto.LoginResponse;
import com.evcs.auth.service.IAuthService;
import com.evcs.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SimpleAuthController {

    private final IAuthService authService;

    @GetMapping("/test")
    public String test() {
        return "SimpleAuthController is working!";
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getIdentifier(), request.getPassword());
        return Result.success("登录成功", response);
    }
}
