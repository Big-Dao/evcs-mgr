package com.evcs.station.controller.internal;

import com.evcs.common.result.Result;
import com.evcs.station.dto.ChargerBasicInfo;
import com.evcs.station.service.IChargerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 充电器信息内部查询端点（服务间调用）。
 *
 * <p>仅供内部服务（protocol 等）解析充电器归属使用：
 * 网关在边缘封锁 /internal/api/**，服务内由 InternalApiTokenFilter 校验共享令牌。
 */
@RestController
@RequestMapping("/internal/api/v1/chargers")
@RequiredArgsConstructor
public class ChargerInternalController {

    private final IChargerService chargerService;

    @GetMapping("/by-id/{chargerId}")
    public Result<ChargerBasicInfo> byId(@PathVariable("chargerId") Long chargerId) {
        return Result.success(chargerService.getBasicInfoById(chargerId));
    }

    @GetMapping("/by-code/{chargerCode}")
    public Result<ChargerBasicInfo> byCode(@PathVariable("chargerCode") String chargerCode) {
        return Result.success(chargerService.getBasicInfoByCode(chargerCode));
    }
}
