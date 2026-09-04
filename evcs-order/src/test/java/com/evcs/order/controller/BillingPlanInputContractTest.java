package com.evcs.order.controller;

import com.evcs.common.test.base.BaseControllerTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.order.dto.OrderDTO;
import com.evcs.common.tenant.TenantContext;
import com.evcs.order.entity.BillingPlan;
import com.evcs.order.service.IBillingPlanService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 计费计划输入契约测试（批量赋值防护）：version/deleted/审计字段不得由请求体注入。
 */
@SpringBootTest(classes = {com.evcs.order.OrderServiceApplication.class,
        com.evcs.order.config.TestConfig.class})
@AutoConfigureMockMvc
@DisplayName("计费计划输入契约")
class BillingPlanInputContractTest extends BaseControllerTest {

    @Resource
    private IBillingPlanService planService;

    @Test
    @DisplayName("创建计划 - 内部字段（version/deleted/tenantId）注入应被忽略")
    void createShouldIgnoreInternalFields() throws Exception {
        String payload = """
            {
                "name": "输入契约计划",
                "code": "INPUT-CTRACT-PLAN",
                "stationId": 9002,
                "status": 1,
                "version": 99,
                "deleted": 1,
                "tenantId": 99,
                "createBy": 777
            }
            """;

        mockMvc.perform(post("/billing/plans")
                        .header("X-Tenant-Id", "1")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("INPUT-CTRACT-PLAN"));

        TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID); // 拦截器请求后清空了上下文，查询前重设
        BillingPlan saved = planService.getOne(
                new QueryWrapper<BillingPlan>().eq("code", "INPUT-CTRACT-PLAN"));
        assertNotNull(saved);
        assertNotEquals(99, saved.getVersion(), "version 不得由调用方指定");
        assertNotEquals(1, saved.getDeleted(), "deleted 不得由调用方指定");
        assertNotEquals(99L, saved.getTenantId(), "租户归属必须来自上下文");
    }
}
