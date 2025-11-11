package com.evcs.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "登录标识不能为空")
    private String identifier;

    @NotBlank(message = "密码不能为空")
    private String password;
}
