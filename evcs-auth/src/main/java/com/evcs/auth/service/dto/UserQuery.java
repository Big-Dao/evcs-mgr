package com.evcs.auth.service.dto;

import lombok.Data;

@Data
public class UserQuery {
    private String username;
    private String loginIdentifier;
    private String realName;
    private Integer status;
    private long current = 1;
    private long size = 10;
}
