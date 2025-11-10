package com.evcs.auth.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignRolesRequest {

    @NotEmpty(message = "角色列表不能为空")
    private List<Long> roleIds;
}

