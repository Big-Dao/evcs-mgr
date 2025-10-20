package com.evcs.station.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.exception.TenantContextMissingException;
import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.IOCPPProtocolService;
import com.evcs.station.entity.Charger;
import com.evcs.station.event.ChargingStartEvent;
import com.evcs.station.event.ChargingStopEvent;
import com.evcs.station.mapper.ChargerMapper;
import com.evcs.station.mapper.StationMapper;
import com.evcs.station.service.IChargerService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 充电桩服务实现类
 */
@Slf4j
@Service
public class ChargerServiceImpl
    extends ServiceImpl<ChargerMapper, Charger>
    implements IChargerService {

    @Autowired(required = false)
    private IOCPPProtocolService ocppService;

    @Autowired(required = false)
    private ICloudChargeProtocolService cloudService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private StationMapper stationMapper;

    /**
     * 分页查询充电桩列表
     * 自动应用多租户数据隔离
     */
    @Override
    @DataScope
    public IPage<Charger> queryChargerPage(
        Page<Charger> page,
        Charger queryParam
    ) {
        QueryWrapper<Charger> wrapper = new QueryWrapper<>();

        // 根据充电桩名称查询
        if (StrUtil.isNotBlank(queryParam.getChargerName())) {
            wrapper.like("charger_name", queryParam.getChargerName());
        }

        // 根据充电桩编码查询
        if (StrUtil.isNotBlank(queryParam.getChargerCode())) {
            wrapper.like("charger_code", queryParam.getChargerCode());
        }

        // 根据充电站ID查询
        if (queryParam.getStationId() != null) {
            wrapper.eq("station_id", queryParam.getStationId());
        }

        // 根据状态查询
        if (queryParam.getStatus() != null) {
            wrapper.eq("status", queryParam.getStatus());
        }

        // 根据类型查询
        if (queryParam.getChargerType() != null) {
            wrapper.eq("charger_type", queryParam.getChargerType());
        }

        // 根据品牌查询
        if (StrUtil.isNotBlank(queryParam.getBrand())) {
            wrapper.eq("brand", queryParam.getBrand());
        }

        // 排序
        wrapper.orderByAsc("station_id").orderByAsc("charger_code");

        return this.page(page, wrapper);
    }

    /**
     * 根据充电站ID查询充电桩列表
     * 自动应用多租户数据隔离
     */
    @Override
    @DataScope
    public List<Charger> getChargersByStationId(Long stationId) {
        return baseMapper.selectByStationId(stationId);
    }

    /**
     * 新增充电桩
     * 自动设置租户ID和创建信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCharger(Charger charger) {
        // 检查充电桩编码是否重复
        if (checkChargerCodeExists(charger.getChargerCode(), null)) {
            throw new RuntimeException("充电桩编码已存在");
        }
        if (charger.getStationId() == null) {
            throw new IllegalArgumentException("stationId must not be null");
        }
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException(
                "执行充电桩保存操作时缺少租户上下文"
            );
        }
        Long userId = TenantContext.getCurrentUserId();
        if (stationMapper.selectById(charger.getStationId()) == null) {
            throw new RuntimeException("关联的充电站不存在");
        }

        // 设置租户信息
        charger.setTenantId(tenantId);
        charger.setCreateTime(LocalDateTime.now());
        charger.setCreateBy(userId != null ? userId : 0L);

        // 设置默认值
        if (charger.getStatus() == null) {
            charger.setStatus(1); // 默认空闲
        }
        if (charger.getEnabled() == null) {
            charger.setEnabled(1); // 默认启用
        }
        if (charger.getGunCount() == null) {
            charger.setGunCount(1); // 默认1个枪头
        }

        return this.save(charger);
    }

    /**
     * 更新充电桩信息
     * 数据权限：只能更新有权限的充电桩
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean updateCharger(Charger charger) {
        // 检查充电桩是否存在
        Charger existCharger = this.getById(charger.getChargerId());
        if (existCharger == null) {
            throw new RuntimeException("充电桩不存在");
        }

        // 检查编码是否重复（排除自己）
        if (
            StrUtil.isNotBlank(charger.getChargerCode()) &&
            checkChargerCodeExists(
                charger.getChargerCode(),
                charger.getChargerId()
            )
        ) {
            throw new RuntimeException("充电桩编码已存在");
        }

        // 设置更新信息
        charger.setUpdateTime(LocalDateTime.now());
        charger.setUpdateBy(TenantContext.getCurrentUserId());

        // 不允许修改租户ID和充电站ID
        charger.setTenantId(null);
        charger.setStationId(null);

        return this.updateById(charger);
    }

    /**
     * 删除充电桩（逻辑删除）
     * 数据权限：只能删除有权限的充电桩
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean deleteCharger(Long chargerId) {
        // 检查是否正在充电
        Charger charger = this.getById(chargerId);
        if (charger != null && charger.getStatus() == 2) {
            throw new RuntimeException("充电桩正在充电，无法删除");
        }

        return this.removeById(chargerId);
    }

    /**
     * 更新充电桩状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long chargerId, Integer status) {
        return (
            baseMapper.updateStatus(chargerId, status, LocalDateTime.now()) > 0
        );
    }

    /**
     * 更新充电桩实时数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRealTimeData(
        Long chargerId,
        Double power,
        Double voltage,
        Double current,
        Double temperature
    ) {
        return (
            baseMapper.updateRealTimeData(
                chargerId,
                power,
                voltage,
                current,
                temperature,
                LocalDateTime.now()
            ) >
            0
        );
    }

    /**
     * 开始充电会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean startChargingSession(
        Long chargerId,
        String sessionId,
        Long userId
    ) {
        // 检查充电桩状态
        Charger charger = this.getById(chargerId);
        if (charger == null) {
            throw new RuntimeException("充电桩不存在");
        }
        if (charger.getStatus() != 1) {
            throw new RuntimeException("充电桩状态异常，无法开始充电");
        }
        if (charger.getEnabled() != 1) {
            throw new RuntimeException("充电桩已禁用");
        }
        // 调用协议层开始充电
        boolean protoOk = invokeStartProtocol(charger, sessionId, userId);
        if (!protoOk) {
            throw new RuntimeException("协议启动失败");
        }
        boolean dbOk =
            baseMapper.startChargingSession(
                chargerId,
                sessionId,
                userId,
                LocalDateTime.now()
            ) >
            0;
        if (dbOk) {
            // 发布充电开始事件，订单服务监听此事件创建订单
            Long billingPlanId = null; // 可以从请求参数传入
            eventPublisher.publishEvent(
                new ChargingStartEvent(
                    this,
                    charger.getStationId(),
                    chargerId,
                    sessionId,
                    userId,
                    billingPlanId,
                    TenantContext.getCurrentTenantId()
                )
            );
            log.info(
                "充电会话开始，充电桩ID: {}, 会话ID: {}, 用户ID: {}",
                chargerId,
                sessionId,
                userId
            );
        }
        return dbOk;
    }

    /**
     * 结束充电会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean endChargingSession(
        Long chargerId,
        Double energy,
        Long duration
    ) {
        Charger charger = this.getById(chargerId);
        if (charger == null) {
            return false;
        }
        String sessionId = charger.getCurrentSessionId();
        invokeStopProtocol(charger);
        boolean ok =
            baseMapper.endChargingSession(chargerId, energy, duration) > 0;
        if (ok && sessionId != null) {
            // 发布充电停止事件，订单服务监听此事件完成订单
            eventPublisher.publishEvent(
                new ChargingStopEvent(
                    this,
                    sessionId,
                    energy,
                    duration,
                    TenantContext.getCurrentTenantId()
                )
            );
            log.info(
                "充电会话结束，会话ID: {}, 充电量: {}, 时长: {}",
                sessionId,
                energy,
                duration
            );
        }
        return ok;
    }

    /**
     * 更新充电进度
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateChargingProgress(
        Long chargerId,
        Double energy,
        Integer duration
    ) {
        return (
            baseMapper.updateChargingProgress(
                chargerId,
                energy,
                duration,
                LocalDateTime.now()
            ) >
            0
        );
    }

    /**
     * 检查充电桩编码是否存在
     */
    @Override
    @DataScope
    public boolean checkChargerCodeExists(String chargerCode, Long excludeId) {
        QueryWrapper<Charger> wrapper = new QueryWrapper<>();
        wrapper.eq("charger_code", chargerCode);
        // MyBatis Plus自动添加tenant_id过滤

        if (excludeId != null) {
            wrapper.ne("charger_id", excludeId);
        }

        return this.count(wrapper) > 0;
    }

    /**
     * 查询离线充电桩
     */
    @Override
    @DataScope
    public List<Charger> getOfflineChargers(Integer minutes) {
        if (minutes == null) {
            minutes = 5; // 默认5分钟
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutes);
        return baseMapper.selectOfflineChargers(threshold);
    }

    /**
     * 查询故障充电桩
     */
    @Override
    @DataScope
    public List<Charger> getFaultChargers() {
        return baseMapper.selectFaultChargers();
    }

    /**
     * 统计充电桩状态
     */
    @Override
    @DataScope
    public Map<Integer, Long> getStatusStatistics(Long tenantId) {
        List<Map<String, Object>> result = baseMapper.countByStatus(tenantId);
        return result
            .stream()
            .collect(
                Collectors.toMap(
                    map -> (Integer) map.get("status"),
                    map -> ((Number) map.get("count")).longValue()
                )
            );
    }

    /**
     * 根据协议类型查询充电桩
     */
    @Override
    @DataScope
    public List<Charger> getChargersByProtocol(String protocol) {
        return baseMapper.selectByProtocol(protocol);
    }

    /**
     * 批量更新充电桩状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean batchUpdateStatus(List<Long> chargerIds, Integer status) {
        if (chargerIds == null || chargerIds.isEmpty()) {
            return false;
        }

        QueryWrapper<Charger> wrapper = new QueryWrapper<>();
        wrapper.in("charger_id", chargerIds);

        Charger updateCharger = new Charger();
        updateCharger.setStatus(status);
        updateCharger.setUpdateTime(LocalDateTime.now());
        updateCharger.setUpdateBy(TenantContext.getCurrentUserId());

        return this.update(updateCharger, wrapper);
    }

    /**
     * 启用/停用充电桩
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean changeStatus(Long chargerId, Integer enabled) {
        Charger charger = new Charger();
        charger.setChargerId(chargerId);
        charger.setEnabled(enabled);
        charger.setUpdateTime(LocalDateTime.now());
        charger.setUpdateBy(TenantContext.getCurrentUserId());

        return this.updateById(charger);
    }

    /**
     * 重置充电桩（清除当前会话）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean resetCharger(Long chargerId) {
        Charger charger = new Charger();
        charger.setChargerId(chargerId);
        charger.setStatus(1); // 设为空闲
        charger.setCurrentSessionId(null);
        charger.setCurrentUserId(null);
        charger.setChargingStartTime(null);
        charger.setChargedEnergy(java.math.BigDecimal.ZERO);
        charger.setChargedDuration(0);
        charger.setUpdateTime(LocalDateTime.now());
        charger.setUpdateBy(TenantContext.getCurrentUserId());

        return this.updateById(charger);
    }

    private boolean invokeStartProtocol(
        Charger charger,
        String sessionId,
        Long userId
    ) {
        if (ocppService == null && cloudService == null) {
            log.warn("协议服务未配置，跳过协议启动");
            return true; // 优雅降级，允许继续
        }

        try {
            String protocols = charger.getSupportedProtocols();
            if (
                protocols != null &&
                protocols.toLowerCase().contains("ocpp") &&
                ocppService != null
            ) {
                return (Boolean) ocppService
                    .getClass()
                    .getMethod(
                        "startCharging",
                        Long.class,
                        String.class,
                        Long.class
                    )
                    .invoke(
                        ocppService,
                        charger.getChargerId(),
                        sessionId,
                        userId
                    );
            }
            if (cloudService != null) {
                return (Boolean) cloudService
                    .getClass()
                    .getMethod(
                        "startCharging",
                        Long.class,
                        String.class,
                        Long.class
                    )
                    .invoke(
                        cloudService,
                        charger.getChargerId(),
                        sessionId,
                        userId
                    );
            }
            return true; // 无可用服务时优雅降级
        } catch (Exception e) {
            log.error("调用协议启动失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private void invokeStopProtocol(Charger charger) {
        if (ocppService == null && cloudService == null) {
            log.warn("协议服务未配置，跳过协议停止");
            return; // 优雅降级
        }

        try {
            String protocols = charger.getSupportedProtocols();
            if (
                protocols != null &&
                protocols.toLowerCase().contains("ocpp") &&
                ocppService != null
            ) {
                ocppService
                    .getClass()
                    .getMethod("stopCharging", Long.class)
                    .invoke(ocppService, charger.getChargerId());
                return;
            }
            if (cloudService != null) {
                cloudService
                    .getClass()
                    .getMethod("stopCharging", Long.class)
                    .invoke(cloudService, charger.getChargerId());
            }
        } catch (Exception e) {
            log.error("调用协议停止失败: {}", e.getMessage(), e);
        }
    }
}
