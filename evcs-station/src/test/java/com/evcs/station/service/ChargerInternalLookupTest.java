package com.evcs.station.service;

import com.evcs.common.test.base.BaseServiceTest;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.common.tenant.TenantContext;
import com.evcs.station.dto.ChargerBasicInfo;
import com.evcs.station.entity.Charger;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 充电器信息内部解析（跨服务查找）测试。
 *
 * <p>protocol 服务代表物理设备查询充电器，调用线程没有租户上下文；
 * 充电器编码/ID 全局唯一，按唯一键解析属于租户安全操作，
 * 应在受控禁用租户过滤的前提下完成，而不是抛出租户缺失异常导致解析永远失败。
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class,
        com.evcs.station.config.TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureWebMvc
@DisplayName("充电器内部信息解析")
class ChargerInternalLookupTest extends BaseServiceTest {

    @Resource
    private IChargerService chargerService;

    @AfterEach
    void restoreTenantFilter() {
        CustomTenantLineHandler.enableTenantFilter();
        TenantContext.clear();
    }

    private Charger givenCharger(String code) {
        Charger charger = new Charger();
        charger.setChargerCode(code);
        charger.setChargerName("解析测试桩");
        charger.setStationId(11L);
        charger.setStatus(1);
        assertNotNull(chargerService.save(charger), "测试数据应保存成功");
        return charger;
    }

    @Test
    @DisplayName("按编码解析 - 无租户上下文时应返回充电器基础信息")
    void shouldResolveByCodeWithoutTenantContext() {
        Charger saved = givenCharger("INTERNAL-RESOLVE-CODE-001");
        TenantContext.clear();

        ChargerBasicInfo info = chargerService.getBasicInfoByCode("INTERNAL-RESOLVE-CODE-001");

        assertNotNull(info, "无租户上下文时按全局唯一编码解析应命中");
        assertEquals(saved.getId(), info.getId());
        assertEquals(11L, info.getStationId());
        assertEquals(DEFAULT_TENANT_ID, info.getTenantId(), "租户ID应来自数据行而非兜底值");
    }

    @Test
    @DisplayName("按ID解析 - 无租户上下文时应返回充电器基础信息")
    void shouldResolveByIdWithoutTenantContext() {
        Charger saved = givenCharger("INTERNAL-RESOLVE-CODE-002");
        TenantContext.clear();

        ChargerBasicInfo info = chargerService.getBasicInfoById(saved.getId());

        assertNotNull(info, "无租户上下文时按主键解析应命中");
        assertEquals(saved.getId(), info.getId());
        assertEquals("INTERNAL-RESOLVE-CODE-002", info.getChargerCode());
    }

    @Test
    @DisplayName("按编码解析 - 不存在的编码应返回null而不是异常")
    void shouldReturnNullForUnknownCode() {
        TenantContext.clear();

        assertNull(chargerService.getBasicInfoByCode("NO-SUCH-CHARGER-CODE"));
    }
}
