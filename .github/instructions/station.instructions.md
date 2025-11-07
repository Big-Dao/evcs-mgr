---
applyTo: "evcs-station/**/*.java"
---

# evcs-station 模块开发规范

> **最后更新**: 2025-11-07 | **维护者**: 技术负责人 | **状态**: 已发布

本模块负责充电站和充电桩管理。这是最成熟的模块，可作为多租户模式的参考实现。

## 🚨 关键要求

### 1. 多租户隔离
**每个查询都必须遵守租户边界**
- 所有 Service 方法应使用 `@DataScope` 注解
- 创建新实体时始终设置 `TenantContext`
- 使用 LambdaWrapper 查询以保留租户过滤

### 2. 站点层级关系
**站点可以有父子关系**
- 验证父站点存在且属于同一租户
- 需要遍历层级的查询使用 `TENANT_HIERARCHY` 作用域
- 避免跨租户的层级访问

### 3. 实时更新
**站点和充电桩状态更新频繁**
- 使用专用 Mapper 方法更新状态，避免更新整个实体
- 考虑为频繁访问的站点数据添加缓存
- 状态更新操作应该是幂等的

### 4. 离线检测
**跟踪站点心跳以监控连接状态**
- 每次通信时更新 `last_heartbeat`
- 使用定时任务检测离线站点（>5 分钟无心跳）
- 离线状态应触发告警通知

---

## ✅ 测试准则

在修改本模块代码时，必须包含以下测试：

- ✅ 多租户场景测试，验证数据隔离
- ✅ 站点层级查询测试（父子关系）
- ✅ 离线检测场景测试
- ✅ 状态更新性能测试
- ✅ 并发访问场景测试

---

## 📝 常见模式

### Service 层 - 租户隔离

```java
// 查询方法 - 使用层级作用域
@Override
@DataScope(value = DataScopeType.TENANT_HIERARCHY)
public List<ChargingStation> findStationsByParentId(Long parentId) {
    LambdaQueryWrapper<ChargingStation> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ChargingStation::getParentId, parentId);
    return baseMapper.selectList(wrapper);
}

// 创建方法 - 自动设置租户ID
@Override
@Transactional
public Result<ChargingStation> createStation(StationDTO dto) {
    ChargingStation station = new ChargingStation();
    BeanUtils.copyProperties(dto, station);
    // BaseEntity 的 tenantId 会被 MetaObjectHandler 自动填充
    save(station);
    return Result.success(station);
}

// 状态更新 - 只更新必要字段
@Override
public void updateStationStatus(Long stationId, StationStatus status) {
    baseMapper.updateStatus(stationId, status, LocalDateTime.now());
}
```

### Controller 层 - 安全和权限

```java
// 列表查询 - 带权限和作用域
@GetMapping("/list")
@PreAuthorize("hasPermission('station:query')")
@DataScope(value = DataScopeType.TENANT_HIERARCHY)
public Result<List<ChargingStation>> list() {
    return Result.success(stationService.list());
}

// 详情查询 - 验证所有权
@GetMapping("/{id}")
@PreAuthorize("hasPermission('station:query')")
@DataScope(value = DataScopeType.TENANT)
public Result<ChargingStation> getById(@PathVariable Long id) {
    ChargingStation station = stationService.getById(id);
    if (station == null) {
        return Result.fail("站点不存在");
    }
    return Result.success(station);
}
```

---

## 🗄️ 数据库 Schema 说明

### charging_station 表
- `tenant_id` - 租户ID，必须有索引
- `parent_id` - 父站点ID，支持层级结构
- `last_heartbeat` - 最后心跳时间，用于离线检测
- `status` - 站点状态（ONLINE, OFFLINE, MAINTENANCE）

### charger 表
- `station_id` - 所属站点ID，外键关联
- `charger_status` - 充电桩状态
- `connector_type` - 接口类型

### JSON/Array 字段
- `facilities` (JSONB) - 站点设施信息
- `supported_protocols` (JSONB) - 支持的协议列表
- `operating_hours` (JSONB) - 营业时间
- `payment_methods` (数组) - 支付方式

---

## ⚠️ 重要注意事项

### 性能优化
- 站点列表查询应该分页
- 状态更新使用批量操作
- 频繁访问的数据考虑缓存

### 数据一致性
- 删除站点前检查是否有关联的充电桩
- 更新站点层级时验证不会形成循环引用
- 状态变更应该记录审计日志

### 协议集成
- 本模块订阅 RabbitMQ 的心跳和状态事件
- 心跳队列: `evcs.protocol.heartbeat`
- 状态队列: `evcs.protocol.status`

---

## 🔧 修改本模块时的检查清单

- [ ] 是否添加了 `@DataScope` 注解？
- [ ] 是否测试了多租户隔离？
- [ ] 是否考虑了站点层级关系？
- [ ] 是否验证了父站点所有权？
- [ ] 是否添加了必要的事务注解？
- [ ] 是否更新了相关测试用例？
- [ ] 是否考虑了性能影响？

---

**最后更新**: 2025-10-20

