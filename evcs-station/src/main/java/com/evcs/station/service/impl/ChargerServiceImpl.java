package com.evcs.station.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.exception.TenantContextMissingException;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.api.ICloudChargeProtocolService;
import com.evcs.protocol.api.IOCPPProtocolService;
import com.evcs.station.dto.ChargerBasicInfo;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import com.evcs.station.enums.ChargerStatus;
import com.evcs.station.event.ChargingStartEvent;
import com.evcs.station.event.ChargingStopEvent;
import com.evcs.station.mapper.ChargerMapper;
import com.evcs.station.mapper.StationMapper;
import com.evcs.station.metrics.StationMetrics;
import com.evcs.station.service.IChargerService;
import com.evcs.station.state.ChargerStatusManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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

    @Autowired
    private StationMetrics stationMetrics;

    @Autowired
    private ChargerStatusManager statusManager;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Override
    public Charger getByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return this.getOne(new QueryWrapper<Charger>().eq("charger_code", code));
    }

    @Override
    public ChargerBasicInfo getBasicInfoByCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        return resolveBasicInfo(() ->
                this.getOne(new QueryWrapper<Charger>().eq("charger_code", code)));
    }

    @Override
    public ChargerBasicInfo getBasicInfoById(Long id) {
        if (id == null) {
            return null;
        }
        return resolveBasicInfo(() -> this.getById(id));
    }

    /**
     * 内部解析：调用方（protocol 服务代表物理设备查询）线程无租户上下文，
     * 而充电器主键/编码全局唯一，按唯一键在受控禁用租户过滤的前提下查询并还原租户归属。
     */
    private ChargerBasicInfo resolveBasicInfo(java.util.function.Supplier<Charger> query) {
        try {
            CustomTenantLineHandler.disableTenantFilter();
            return toBasicInfo(query.get());
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }
    }

    private ChargerBasicInfo toBasicInfo(Charger charger) {
        if (charger == null) {
            return null;
        }
        ChargerBasicInfo info = new ChargerBasicInfo();
        info.setId(charger.getId());
        info.setTenantId(charger.getTenantId());
        info.setStationId(charger.getStationId());
        info.setChargerCode(charger.getChargerCode());
        info.setChargerName(charger.getChargerName());
        return info;
    }

    /**
     * 分页查询充电桩列表
     * 自动应用多租户数据隔离
     */
    @Override
    @DataScope
    public IPage<Charger> queryChargerPage(
            Page<Charger> page,
            Charger queryParam) {
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

        IPage<Charger> result = this.page(page, wrapper);

        if (result != null && result.getRecords() != null && !result.getRecords().isEmpty()) {
            List<Long> stationIds = result.getRecords().stream()
                    .map(Charger::getStationId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (!stationIds.isEmpty()) {
                List<Station> stations = stationMapper.selectBatchIds(stationIds);
                Map<Long, String> stationCodeMap = stations.stream()
                        .collect(Collectors.toMap(Station::getStationId, Station::getStationCode));
                result.getRecords().forEach(charger -> {
                    String code = stationCodeMap.get(charger.getStationId());
                    charger.setStationCode(code);
                });
            }
        }

        return result;
    }

    /**
     * 根据充电站ID查询充电桩列表
     * 自动应用多租户数据隔离
     */
    @Override
    @DataScope
    public List<Charger> getChargersByStationId(Long stationId) {
        List<Charger> chargers = baseMapper.selectByStationId(stationId);
        if (chargers != null && !chargers.isEmpty()) {
            Station station = stationMapper.selectById(stationId);
            String stationCode = station != null ? station.getStationCode() : null;
            chargers.forEach(charger -> charger.setStationCode(stationCode));
        }
        return chargers;
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
                    "执行充电桩保存操作时缺少租户上下文");
        }
        Long userId = TenantContext.getCurrentUserId();
        Station station = stationMapper.selectById(charger.getStationId());
        if (station == null) {
            throw new RuntimeException("关联的充电站不存在");
        }

        // 设置租户信息
        charger.setTenantId(tenantId);
        charger.setStationCode(station.getStationCode()); // 设置充电站编码
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
        Charger existCharger = this.getById(charger.getId());
        if (existCharger == null) {
            throw new RuntimeException("充电桩不存在");
        }

        // 检查编码是否重复（排除自己）
        if (StrUtil.isNotBlank(charger.getChargerCode()) &&
                checkChargerCodeExists(
                        charger.getChargerCode(),
                        charger.getId())) {
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
        try {
            Charger charger = this.getById(chargerId);
            if (charger == null) {
                log.warn("Charger not found: {}", chargerId);
                return false;
            }
            ChargerStatus oldStatus = ChargerStatus.fromCode(charger.getStatus());
            ChargerStatus newStatus = ChargerStatus.fromCode(status);

            // 1. 校验状态流转
            if (!statusManager.validateTransition(oldStatus, newStatus)) {
                log.warn("Invalid status transition for charger {}: {} -> {}", chargerId, oldStatus, newStatus);
                throw new IllegalStateException("非法状态变更: " + oldStatus + " -> " + newStatus);
            }

            // 2. 更新数据库
            boolean result = (baseMapper.updateStatus(chargerId, status, LocalDateTime.now()) > 0);

            // 3. 触发副作用
            if (result) {
                statusManager.onTransition(chargerId, oldStatus, newStatus, TenantContext.getCurrentTenantId());
            }

            return result;
        } catch (IllegalStateException e) {
            throw e; // Re-throw validation exceptions
        } catch (Exception e) {
            log.error("Error updating charger status: chargerId={}, status={}", chargerId, status, e);
            throw e;
        }
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
            Double temperature) {
        return (baseMapper.updateRealTimeData(
                chargerId,
                power,
                voltage,
                current,
                temperature,
                LocalDateTime.now()) > 0);
    }

    /**
     * 更新充电桩心跳时间（幂等）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateHeartbeat(Long chargerId, LocalDateTime heartbeatTime) {
        try {
            LocalDateTime hb = heartbeatTime != null ? heartbeatTime : LocalDateTime.now();
            boolean ok = baseMapper.updateHeartbeat(chargerId, hb) > 0;
            if (ok) {
                stationMetrics.recordHeartbeatReceived();
            }
            return ok;
        } catch (Exception e) {
            log.error("Error updating charger heartbeat: chargerId={}", chargerId, e);
            throw e;
        }
    }

    /**
     * 开始充电会话
     * 使用分布式锁防止并发启动同一充电桩
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean startChargingSession(
            Long chargerId,
            String sessionId,
            Long userId) {
        String lockKey = "charger:lock:start:" + chargerId;
        RLock lock = redissonClient != null ? redissonClient.getLock(lockKey) : null;

        if (lock != null) {
            try {
                // 尝试获取锁，等待 5 秒，锁定 30 秒
                if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                    throw new RuntimeException("充电桩正在处理中，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁被中断", e);
            }
        }

        try {
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
            boolean dbOk = baseMapper.startChargingSession(
                    chargerId,
                    sessionId,
                    userId,
                    LocalDateTime.now()) > 0;
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
                                TenantContext.getCurrentTenantId()));
                log.info(
                        "充电会话开始，充电桩ID: {}, 会话ID: {}, 用户ID: {}",
                        chargerId,
                        sessionId,
                        userId);
            }
            return dbOk;
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 结束充电会话
     * 使用分布式锁防止并发停止同一充电桩
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataScope
    public boolean endChargingSession(
            Long chargerId,
            Double energy,
            Long duration) {
        String lockKey = "charger:lock:stop:" + chargerId;
        RLock lock = redissonClient != null ? redissonClient.getLock(lockKey) : null;

        if (lock != null) {
            try {
                // 尝试获取锁，等待 5 秒，锁定 30 秒
                if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                    throw new RuntimeException("充电桩正在处理中，请稍后重试");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁被中断", e);
            }
        }

        try {
            Charger charger = this.getById(chargerId);
            if (charger == null) {
                return false;
            }
            String sessionId = charger.getCurrentSessionId();
            invokeStopProtocol(charger);
            boolean ok = baseMapper.endChargingSession(chargerId, energy, duration) > 0;
            if (ok && sessionId != null) {
                // 发布充电停止事件，订单服务监听此事件完成订单
                eventPublisher.publishEvent(
                        new ChargingStopEvent(
                                this,
                                sessionId,
                                energy,
                                duration,
                                TenantContext.getCurrentTenantId()));
                log.info(
                        "充电会话结束，会话ID: {}, 充电量: {}, 时长: {}",
                        sessionId,
                        energy,
                        duration);
            }
            return ok;
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 更新充电进度
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateChargingProgress(
            Long chargerId,
            Double energy,
            Integer duration) {
        return (baseMapper.updateChargingProgress(
                chargerId,
                energy,
                duration,
                LocalDateTime.now()) > 0);
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
                                map -> ((Number) map.get("count")).longValue()));
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
        charger.setId(chargerId);
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
        charger.setId(chargerId);
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

    /**
     * 调用协议层启动充电
     * <p>严格按照协议类型调用，不支持 fallback 为 true，确保充电真实启动
     *
     * @throws RuntimeException 当协议未配置、服务不可用或协议不支持时抛出异常
     */
    private boolean invokeStartProtocol(
            Charger charger,
            String sessionId,
            Long userId) {
        // 1. 检查协议是否配置
        if (StrUtil.isBlank(charger.getSupportedProtocols())) {
            log.error("Charger {} has no supported protocols configured", charger.getChargerCode());
            throw new RuntimeException("充电桩协议未配置，无法启动充电: " + charger.getChargerCode());
        }

        cn.hutool.json.JSONObject protocols = cn.hutool.json.JSONUtil.parseObj(charger.getSupportedProtocols());

        // 2. 优先处理 OCPP 协议
        if (protocols.containsKey("ocpp")) {
            if (ocppService == null) {
                log.error("OCPP protocol service not available for charger {}", charger.getChargerCode());
                throw new RuntimeException("OCPP 协议服务不可用");
            }
            boolean result = ocppService.startCharging(charger.getId(), sessionId, userId);
            if (!result) {
                log.error("OCPP startCharging returned false for charger {}", charger.getChargerCode());
                throw new RuntimeException("OCPP 协议启动充电失败");
            }
            return true;
        }

        // 3. 处理 CloudCharge 协议
        if (protocols.containsKey("cloudCharge")) {
            if (cloudService == null) {
                log.error("CloudCharge protocol service not available for charger {}", charger.getChargerCode());
                throw new RuntimeException("CloudCharge 协议服务不可用");
            }
            boolean result = cloudService.startCharging(charger.getId(), sessionId, userId);
            if (!result) {
                log.error("CloudCharge startCharging returned false for charger {}", charger.getChargerCode());
                throw new RuntimeException("CloudCharge 协议启动充电失败");
            }
            return true;
        }

        // 4. 不支持的协议类型
        log.error("Unsupported protocol type for charger {}: {}",
                charger.getChargerCode(), charger.getSupportedProtocols());
        throw new RuntimeException("不支持的充电协议类型: " + charger.getSupportedProtocols());
    }

    private void invokeStopProtocol(Charger charger) {
        if (StrUtil.isBlank(charger.getSupportedProtocols())) {
            return;
        }

        cn.hutool.json.JSONObject protocols = cn.hutool.json.JSONUtil.parseObj(charger.getSupportedProtocols());

        try {
            if (protocols.containsKey("ocpp")) {
                if (ocppService != null) {
                    ocppService.stopCharging(charger.getId());
                }
            } else if (protocols.containsKey("cloudCharge")) {
                if (cloudService != null) {
                    cloudService.stopCharging(charger.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to stop charging via protocol for charger {}", charger.getChargerCode(), e);
        }
    }
}
