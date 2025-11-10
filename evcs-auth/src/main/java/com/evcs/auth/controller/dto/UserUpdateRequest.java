package com.evcs.auth.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(max = 50)
    private String username;

    @Size(max = 50)
    private String realName;

    @Size(max = 20)
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer gender;

    private Integer status;

    private Integer userType;
}

