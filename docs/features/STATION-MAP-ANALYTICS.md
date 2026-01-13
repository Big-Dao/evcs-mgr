# 充电站地图分析功能说明

> **功能版本**: v1.0  
> **创建日期**: 2025-12-18  
> **维护者**: 开发团队  

## 功能概述

充电站地图分析功能为管理后台提供基于城市级别的订单数据可视化分析能力，帮助运营人员快速了解不同城市的业务分布和运营状况。

### 核心功能
1. **城市级别订单统计** - 按省份和城市聚合订单数据
2. **数据可视化** - 使用双柱状图展示省份和城市订单分布
3. **时间范围筛选** - 支持按日期范围过滤统计数据
4. **数据概览** - 显示总订单数、覆盖城市数、总充电量、总金额
5. **排行榜** - TOP 10 城市订单量排名

## 技术实现

### 后端 (evcs-order模块)
- **DTO**: `CityOrderStatistics` - 城市订单统计数据
- **Mapper**: `ChargingOrderMapper.getCityOrderStatistics()` - SQL聚合查询
- **Service**: `IChargingOrderService.getCityOrderStatistics()` - 业务逻辑
- **Controller**: `GET /order/statistics/city` - RESTful API端点
- **测试**: `CityOrderStatisticsTest` - 单元测试覆盖

### 前端 (evcs-admin模块)
- **API**: `order.ts` - 接口定义
- **组件**: `StationMapAnalytics.vue` - 页面组件
- **路由**: `/stations/map-analytics` - 访问路径
- **可视化**: ECharts 5.5.0 双柱状图

## 安全特性
- 多租户数据隔离（@DataScope注解）
- 租户上下文管理（TenantContext）
- SQL注入防护（MyBatis参数化查询）
- 权限控制（需要管理后台权限）

## 使用指南

### 访问页面
1. 登录管理后台
2. 导航到 "地图分析" 菜单
3. 查看省份和城市订单分布

### 筛选数据
- 使用日期范围选择器筛选特定时间段
- 点击刷新按钮更新数据

## 性能优化建议

建议添加数据库索引：
```sql
CREATE INDEX idx_order_tenant_station_time 
ON charging_order(tenant_id, station_id, start_time);

CREATE INDEX idx_station_tenant_city 
ON charging_station(tenant_id, province, city);
```

## 已知限制
1. 不支持实时更新（需手动刷新）
2. 暂无下钻功能
3. 固定时间格式（日期范围）

## 未来改进
1. 实时数据推送
2. 交互式地图钻取
3. 集成真实地图组件
4. 导出Excel/PDF报表

---

**最后更新**: 2025-12-18  
**文档版本**: v1.0
