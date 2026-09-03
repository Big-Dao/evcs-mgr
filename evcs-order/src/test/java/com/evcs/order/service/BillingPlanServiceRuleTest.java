package com.evcs.order.service;

import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.order.entity.BillingPlan;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 计费计划业务规则测试。
 *
 * <p>规则（原散落在 Controller 中）必须由服务层执行：
 * 每站点启用中的计划不超过 16 个；默认计划在同站点内唯一。
 */
@SpringBootTest(classes = {com.evcs.order.OrderServiceApplication.class,
        com.evcs.order.config.TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureWebMvc
@DisplayName("计费计划业务规则")
class BillingPlanServiceRuleTest extends BaseServiceTest {

    private static final Long STATION_ID = 9001L;

    @Resource
    private IBillingPlanService planService;

    private BillingPlan enabledPlan(String name, Integer isDefault) {
        BillingPlan plan = new BillingPlan();
        plan.setName(name);
        plan.setCode("RULE-" + System.nanoTime());
        plan.setStationId(STATION_ID);
        plan.setStatus(1);
        plan.setIsDefault(isDefault);
        return plan;
    }

    @Test
    @DisplayName("创建 - 每站点启用计划达到16个后应拒绝")
    void createShouldDenyWhenStationEnabledPlanLimitReached() {
        for (int i = 1; i <= 16; i++) {
            BillingPlan plan = enabledPlan("限流-" + i, 0);
            assertTrue(planService.createPlan(plan).success(), "前16个应创建成功");
        }

        IBillingPlanService.PlanWriteOutcome outcome = planService.createPlan(enabledPlan("第17个", 0));

        assertFalse(outcome.success(), "第17个启用计划应被拒绝");
        assertNotNull(outcome.error());
        assertTrue(outcome.error().contains("16"));
    }

    @Test
    @DisplayName("创建 - 新默认计划应取消同站点其他默认")
    void createShouldResetOtherDefaultPlans() {
        BillingPlan first = enabledPlan("默认A", 1);
        assertTrue(planService.createPlan(first).success());

        BillingPlan second = enabledPlan("默认B", 1);
        assertTrue(planService.createPlan(second).success());

        BillingPlan reloadedFirst = planService.getById(first.getId());
        assertEquals(0, reloadedFirst.getIsDefault(), "旧的默认计划应被取消默认标记");
        assertEquals(1, planService.getById(second.getId()).getIsDefault());
    }

    @Test
    @DisplayName("更新 - 违反默认唯一规则外的正常更新应失效缓存并保持规则")
    void updateShouldApplyAndKeepRules() {
        BillingPlan plan = enabledPlan("更新目标", 0);
        assertTrue(planService.createPlan(plan).success());

        plan.setName("更新后的名字");
        IBillingPlanService.PlanWriteOutcome outcome = planService.updatePlan(plan);

        assertTrue(outcome.success());
        assertEquals("更新后的名字", planService.getById(plan.getId()).getName());
    }

    @Test
    @DisplayName("设为默认 - 应取消同站点其他默认计划")
    void setDefaultShouldResetOthers() {
        BillingPlan a = enabledPlan("默认候选A", 1);
        assertTrue(planService.createPlan(a).success());
        BillingPlan b = enabledPlan("默认候选B", 0);
        assertTrue(planService.createPlan(b).success());

        IBillingPlanService.PlanWriteOutcome outcome = planService.setDefaultPlan(b.getId(), STATION_ID);

        assertTrue(outcome.success());
        assertEquals(1, planService.getById(b.getId()).getIsDefault());
        assertEquals(0, planService.getById(a.getId()).getIsDefault());
    }

    @Test
    @DisplayName("设为默认 - 计划不存在应返回失败")
    void setDefaultShouldFailWhenPlanMissing() {
        IBillingPlanService.PlanWriteOutcome outcome = planService.setDefaultPlan(999999L, STATION_ID);

        assertFalse(outcome.success());
        assertNull(outcome.plan());
    }
}
