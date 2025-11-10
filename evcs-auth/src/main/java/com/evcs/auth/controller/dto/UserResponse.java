package com.evcs.auth.controller.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class UserResponse {
    Long id;
    String username;
    String realName;
    String phone;
    String email;
    Integer gender;
    Integer status;
    Integer userType;
    Long tenantId;
    String avatar;
    LocalDateTime createTime;
    LocalDateTime updateTime;
    List<String> roles;
}

