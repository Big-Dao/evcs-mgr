# 测试数据说明

本目录包含了充电站管理平台的测试数据SQL脚本。

## 文件说明

### 1. test_data.sql
**小规模测试数据** - 适合功能开发和调试

- **租户**: 1个（系统默认租户 ID=1）
- **用户**: 4个（admin + 3个测试用户）
- **充电站**: 5个
  - 市中心充电站（8个桩）
  - 商业中心充电站（6个桩）
  - 住宅区充电站（10个桩）
  - 高速服务区充电站（12个桩）
  - 办公楼充电站（4个桩）
- **充电桩**: 40个（含快充、慢充、超充）
- **计费方案**: 3个
- **充电订单**: 13个今日订单

**使用场景**:
- 本地开发测试
- 功能验证
- UI界面调试

**导入方法**:
```powershell
Get-Content sql/test_data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
```

### 2. large_scale_test_data.sql
**大规模测试数据** - 适合性能测试和压力测试

#### 数据规模
- **平台方**: 2个
  - 华南充电平台
  - 华东充电平台
- **运营商**: 18个（华南8个、华东10个）
- **充电站**: ~190个（每个运营商2-24个站点）
- **充电桩**: ~5,800个（每个站点2-120个桩）
- **用户**: ~108个（每个运营商3-8个用户）
- **充电订单**: ~1,000个今日订单

#### 数据特点
- **充电桩状态分布**:
  - 10% 离线
  - 60% 空闲
  - 25% 充电中
  - 5% 故障
  
- **充电桩类型**:
  - 60% 快充(DC): 60-180kW
  - 40% 慢充(AC): 7-11kW

- **站点规模分布**:
  - 30% 小站点: 2-10个桩
  - 50% 中站点: 10-40个桩
  - 20% 大站点: 40-120个桩

- **订单状态**:
  - 80% 已完成已支付
  - 20% 充电中

#### 租户层级结构
```
华南充电平台 (ID: 100)
├── 深圳特来电运营 (ID: 101)
├── 深圳星星充电 (ID: 102)
├── 广州南方电网 (ID: 103)
├── 东莞普天充电 (ID: 104)
├── 佛山蔚来能源 (ID: 105)
├── 珠海小鹏充电 (ID: 106)
├── 惠州比亚迪充电 (ID: 107)
└── 中山理想充电 (ID: 108)

华东充电平台 (ID: 200)
├── 上海国家电网 (ID: 201)
├── 杭州星星充电 (ID: 202)
├── 南京特来电 (ID: 203)
├── 苏州蔚来能源 (ID: 204)
├── 无锡小鹏充电 (ID: 205)
├── 宁波理想充电 (ID: 206)
├── 温州比亚迪充电 (ID: 207)
├── 嘉兴普天充电 (ID: 208)
├── 南通极狐充电 (ID: 209)
└── 常州问界充电 (ID: 210)
```

**使用场景**:
- 性能压测基础数据
- 多租户隔离测试
- 大数据量查询优化
- 分页功能验证
- Dashboard统计性能测试

**导入方法**:
```powershell
# 注意：此脚本会清理ID>=100的租户数据，请谨慎使用
Get-Content sql/large_scale_test_data.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
```

**执行时间**: 约2-5分钟（取决于系统性能）

#### 运营商数据示例
| 运营商 | 充电站 | 充电桩 | 今日订单 | 今日充电量(kWh) | 今日收入(元) |
|--------|--------|--------|----------|----------------|--------------|
| 上海国家电网 | 21 | 739 | 115 | 6,147.93 | 6,842.37 |
| 杭州星星充电 | 18 | 648 | 118 | 5,631.29 | 6,970.53 |
| 中山理想充电 | 23 | 646 | 110 | 5,213.58 | 7,522.69 |
| 常州问界充电 | 17 | 613 | 99 | 4,499.45 | 5,782.09 |
| 温州比亚迪充电 | 11 | 438 | 83 | 3,600.73 | 4,107.07 |

### 3. fix_charger_table.sql
**修复脚本** - 修复charger表结构

如果在数据导入过程中遇到charger表不存在的错误，使用此脚本修复。

**使用方法**:
```powershell
Get-Content sql/fix_charger_table.sql | docker exec -i evcs-postgres psql -U postgres -d evcs_mgr
```

## 数据查询示例

### 查看所有租户统计
```sql
SELECT 
    t.tenant_name,
    (SELECT COUNT(*) FROM charging_station WHERE tenant_id=t.id AND deleted=0) as stations,
    (SELECT COUNT(*) FROM charger WHERE tenant_id=t.id AND deleted=0) as chargers,
    (SELECT COUNT(*) FROM charging_order WHERE tenant_id=t.id AND deleted=0 
     AND start_time >= date_trunc('day', CURRENT_TIMESTAMP)) as today_orders
FROM sys_tenant t
WHERE t.deleted=0 AND t.id >= 100
ORDER BY t.id;
```

### 查看充电桩状态分布
```sql
SELECT 
    CASE status 
        WHEN 0 THEN '离线'
        WHEN 1 THEN '空闲'
        WHEN 2 THEN '充电中'
        WHEN 3 THEN '故障'
    END as status_name,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM charger WHERE deleted=0), 2) as percentage
FROM charger 
WHERE deleted=0
GROUP BY status
ORDER BY status;
```

### 查看今日订单汇总
```sql
SELECT 
    COUNT(*) as total_orders,
    COUNT(*) FILTER (WHERE status = 11) as paid_orders,
    COUNT(*) FILTER (WHERE status = 1) as charging_orders,
    ROUND(SUM(energy)::numeric, 2) as total_energy_kwh,
    ROUND(SUM(amount)::numeric, 2) as total_revenue_yuan
FROM charging_order 
WHERE deleted = 0 
  AND start_time >= date_trunc('day', CURRENT_TIMESTAMP);
```

## 注意事项

1. **数据隔离**: 测试数据使用租户ID >= 100，不会影响租户ID=1（系统默认租户）的数据
2. **密码**: 所有测试用户的密码都是 `password`
3. **执行顺序**: 建议先导入 `test_data.sql` 测试基本功能，再使用 `large_scale_test_data.sql` 进行性能测试
4. **清理数据**: `large_scale_test_data.sql` 会先清理ID>=100的租户数据，然后重新生成
5. **性能影响**: 大规模数据导入会占用较多的磁盘空间和内存，请确保系统资源充足

## 故障排查

### 问题1: charger表不存在
**解决方案**: 先执行 `fix_charger_table.sql`

### 问题2: 外键约束错误
**原因**: 表创建顺序问题
**解决方案**: 按顺序执行：
1. `sql/init.sql`
2. `sql/charging_station_tables.sql` 或 `fix_charger_table.sql`
3. `sql/evcs_order_tables.sql`
4. `sql/test_data.sql` 或 `large_scale_test_data.sql`

### 问题3: 数据导入超时
**原因**: 数据量太大
**解决方案**: 调整数据规模参数或分批导入

## 性能基准

基于 `large_scale_test_data.sql` 的性能参考（仅供参考）:

- **数据生成时间**: 2-5分钟
- **总数据量**: 约6,000条充电桩记录 + 1,000条订单记录
- **磁盘占用**: ~50-100MB
- **Dashboard查询响应**: <100ms（带索引）
- **充电站列表查询**: <50ms（20条/页）
- **充电桩状态更新**: <10ms（单条）

## 相关文档

- [技术设计文档](../docs/TECHNICAL-DESIGN.md)
- [租户隔离说明](../README-TENANT-ISOLATION.md)
- [开发指南](../docs/DEVELOPER-GUIDE.md)
