package com.evcs.auth.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50)
    private String username;

    @NotBlank(message = "登录标识不能为空")
    @Size(max = 100)
    private String loginIdentifier;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64)
    private String password;

    @Size(max = 50)
    private String realName;

    @Size(max = 20)
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer gender;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private Integer userType;

    private Long tenantId;
}
