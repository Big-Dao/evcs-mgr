# CodeX生成示例：充电站服务

## 📝 请求示例

以下是如何向CodeX请求生成EVCS项目代码的完整示例：

### 1. Service层生成请求

```
你正在为EVCS充电站管理系统编写代码。请严格遵循以下规范：

🏗️ 架构要求：
- Spring Boot 3.2.10 + Java 21微服务架构
- 多租户数据隔离（tenant_id）
- DDD分层：Controller → Service → Repository → Entity
- 严禁跨服务直接访问数据库

📝 编码标准：
- Service: @Service, @Transactional, @Slf4j, @Cacheable/@CacheEvict, @DataScope
- 使用@Valid @RequestBody进行输入验证
- 使用log.info()而不是System.out.println()
- 继承BaseEntity，包含tenant_id

🚫 禁止模式：
- 跨服务数据库访问（如StationService中注入UserRepository）
- 硬编码敏感信息
- 空catch块或忽略异常处理
- 直接返回实体，必须使用DTO

✅ 必须包含：
- 异常处理和日志记录
- 事务管理（@Transactional）
- 缓存管理（Spring Cache注解）
- 租户上下文管理（TenantContext）

现在请为充电站管理模块生成StationService类，包含：
1. createStation(CreateStationRequest request) - 创建充电站
2. getStationById(Long id) - 根据ID查询充电站
3. getStationsByPage(int page, int size) - 分页查询充电站
4. updateStationStatus(Long id, StationStatus status) - 更新充电站状态
5. deleteStation(Long id) - 删除充电站

每个方法都要包含完整的异常处理、日志记录、缓存管理和租户隔离。
```

### 2. 预期输出示例

```java
package com.evcs.station.service.impl;

import com.evcs.common.context.TenantContext;
import com.evcs.common.dto.PageResult;
import com.evcs.common.exception.BusinessException;
import com.evcs.common.exception.StationNotFoundException;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.enums.DataScopeType;
import com.evcs.station.dto.StationDTO;
import com.evcs.station.dto.CreateStationRequest;
import com.evcs.station.entity.Station;
import com.evcs.station.entity.StationStatus;
import com.evcs.station.mapper.StationMapper;
import com.evcs.station.service.StationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 充电站服务实现类
 *
 * @author EVCS Team
 * @since 2025-11-07
 */
@Service
@Transactional
@Slf4j
public class StationServiceImpl implements StationService {

    private final StationMapper stationMapper;

    public StationServiceImpl(StationMapper stationMapper) {
        this.stationMapper = stationMapper;
    }

    @Cacheable(value = "stations", key = "#id")
    @DataScope(DataScopeType.TENANT)
    @Override
    public StationDTO getStationById(Long id) {
        log.debug("查询充电站信息: {}", id);

        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new StationNotFoundException("充电站不存在: " + id);
        }

        return StationDTO.fromEntity(station);
    }

    @Cacheable(value = "stations", key = "'page_' + #page + '_' + #size + '_' + TenantContext.getCurrentTenantId()")
    @DataScope(DataScopeType.TENANT)
    @Override
    public PageResult<StationDTO> getStationsByPage(int page, int size) {
        log.debug("分页查询充电站: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Station> stationPage = stationMapper.selectPage(pageable);

        List<StationDTO> stationDTOs = stationPage.getContent().stream()
                .map(StationDTO::fromEntity)
                .toList();

        return PageResult.<StationDTO>builder()
                .content(stationDTOs)
                .page(page)
                .size(size)
                .total(stationPage.getTotalElements())
                .totalPages(stationPage.getTotalPages())
                .build();
    }

    @CacheEvict(value = "stations", allEntries = true)
    @Transactional
    @Override
    public StationDTO createStation(CreateStationRequest request) {
        log.info("创建充电站: {}", request.getName());

        validateCreateRequest(request);

        // 检查充电站名称是否重复
        if (stationMapper.existsByName(request.getName())) {
            throw new BusinessException("充电站名称已存在: " + request.getName());
        }

        Station station = Station.builder()
                .name(request.getName())
                .address(request.getAddress())
                .capacity(request.getCapacity())
                .status(StationStatus.ACTIVE)
                .tenantId(TenantContext.getCurrentTenantId())
                .build();

        station = stationMapper.insert(station);
        log.info("创建充电站成功: {}", station.getId());

        return StationDTO.fromEntity(station);
    }

    @CacheEvict(value = "stations", allEntries = true)
    @Transactional
    @Override
    public StationDTO updateStationStatus(Long id, StationStatus status) {
        log.info("更新充电站状态: id={}, status={}", id, status);

        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new StationNotFoundException("充电站不存在: " + id);
        }

        // 状态变更业务逻辑验证
        validateStatusChange(station.getStatus(), status);

        station.setStatus(status);
        station = stationMapper.update(station);

        log.info("充电站状态更新成功: id={}, oldStatus={}, newStatus={}",
                id, station.getStatus(), status);

        return StationDTO.fromEntity(station);
    }

    @CacheEvict(value = "stations", allEntries = true)
    @Transactional
    @Override
    public void deleteStation(Long id) {
        log.info("删除充电站: {}", id);

        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new StationNotFoundException("充电站不存在: " + id);
        }

        // 检查是否可以删除（如是否有正在进行的充电订单）
        validateDeleteOperation(station);

        stationMapper.deleteById(id);
        log.info("充电站删除成功: {}", id);
    }

    /**
     * 验证创建充电站请求
     */
    private void validateCreateRequest(CreateStationRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException("充电站名称不能为空");
        }
        if (request.getName().length() > 100) {
            throw new BusinessException("充电站名称长度不能超过100个字符");
        }
        if (!StringUtils.hasText(request.getAddress())) {
            throw new BusinessException("充电站地址不能为空");
        }
        if (request.getCapacity() == null || request.getCapacity() <= 0) {
            throw new BusinessException("充电站容量必须大于0");
        }
    }

    /**
     * 验证状态变更
     */
    private void validateStatusChange(StationStatus oldStatus, StationStatus newStatus) {
        // 某些状态下不允许变更
        if (oldStatus == StationStatus.UNDER_MAINTENANCE &&
            newStatus != StationStatus.ACTIVE) {
            throw new BusinessException("维护中的充电站只能变更为活跃状态");
        }
    }

    /**
     * 验证删除操作
     */
    private void validateDeleteOperation(Station station) {
        // 检查是否有活跃的充电桩
        if (stationMapper.countActiveChargingPoles(station.getId()) > 0) {
            throw new BusinessException("充电站下存在活跃的充电桩，无法删除");
        }
    }
}
```

## 🔍 关键特性说明

### 1. 租户隔离
- 使用`TenantContext.getCurrentTenantId()`获取当前租户
- 所有查询都添加`@DataScope(DataScopeType.TENANT)`注解
- 创建实体时自动设置`tenantId`

### 2. 缓存管理
- 查询方法使用`@Cacheable`
- 更新/删除方法使用`@CacheEvict(allEntries = true)`
- 缓存键包含租户信息避免数据混淆

### 3. 异常处理
- 自定义业务异常（BusinessException）
- 自定义资源不存在异常（StationNotFoundException）
- 完整的参数验证和业务规则检查

### 4. 日志记录
- 使用`@Slf4j`注解
- 关键操作记录info级别日志
- 调试信息记录debug级别日志

### 5. 事务管理
- 类级别`@Transactional`
- 写操作方法级别额外`@Transactional`确保原子性

通过这种方式，CodeX可以生成符合EVCS项目所有规范的高质量代码。