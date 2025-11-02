package com.evcs.common.demo;

/**
 * 多租户隔离效果演示
 * 展示行级隔离方案的实际应用效果
 */
public class TenantIsolationDemo {

    /**
     * 示例1：简单的数据查询
     * 开发者编写的代码 vs 实际执行的SQL
     */
    public void demonstrateSimpleQuery() {
        // 开发者编写的代码（无需关心租户隔离）
        // List<ChargingStation> stations = stationMapper.selectList(null);
        
        // MyBatis Plus自动生成的SQL：
        // SELECT * FROM charging_station WHERE tenant_id = 100;
        
        // 如果使用Schema隔离，开发者需要写：
        // List<ChargingStation> stations = stationMapper.selectListBySchema("tenant_100");
        // 对应SQL：SELECT * FROM tenant_100.charging_station;
    }

    /**
     * 示例2：跨租户查询（管理员功能）
     */
    public void demonstrateCrossTenantQuery() {
        // 行级隔离：简单优雅
        // @DataScope(value = DataScope.Type.CHILDREN)
        // public List<ChargingStation> getAllSubTenantStations() {
        //     return stationMapper.selectList(null);
        // }
        
        // 自动生成的SQL：
        // SELECT * FROM charging_station 
        // WHERE tenant_id IN (100, 101, 102, 103); -- 当前租户及其子租户
        
        // Schema隔离需要：
        // SELECT * FROM tenant_100.charging_station
        // UNION ALL SELECT * FROM tenant_101.charging_station
        // UNION ALL SELECT * FROM tenant_102.charging_station
        // UNION ALL SELECT * FROM tenant_103.charging_station;
    }

    /**
     * 示例3：复杂的业务查询
     */
    public void demonstrateComplexQuery() {
        // 行级隔离：业务逻辑清晰
        // SELECT cs.*, c.charger_count, o.order_count
        // FROM charging_station cs
        // LEFT JOIN (
        //     SELECT station_id, COUNT(*) as charger_count 
        //     FROM charger 
        //     WHERE tenant_id = 100  -- 自动添加
        //     GROUP BY station_id
        // ) c ON cs.station_id = c.station_id
        // LEFT JOIN (
        //     SELECT station_id, COUNT(*) as order_count
        //     FROM charging_order 
        //     WHERE tenant_id = 100  -- 自动添加
        //     GROUP BY station_id
        // ) o ON cs.station_id = o.station_id
        // WHERE cs.tenant_id = 100;  -- 自动添加
        
        // Schema隔离：需要处理多个schema的JOIN
        // 复杂度呈指数级增长
    }

    /**
     * 示例4：新增租户的对比
     */
    public void demonstrateNewTenantCreation() {
        // 行级隔离：只需要在租户表插入一条记录
        // INSERT INTO sys_tenant (tenant_code, tenant_name, ...) 
        // VALUES ('TENANT_501', '新租户', ...);
        // 
        // 新租户立即可以使用所有功能，无需额外操作
        
        // Schema隔离：需要执行大量DDL
        // 1. CREATE SCHEMA tenant_501;
        // 2. CREATE TABLE tenant_501.charging_station (...); -- 重复50+次
        // 3. CREATE INDEX tenant_501.idx_station_code ON tenant_501.charging_station(station_code); -- 重复数百次
        // 4. 初始化基础数据
        // 5. 配置权限
        // 
        // 耗时可能达到分钟级别，影响用户体验
    }

    /**
     * 示例5：数据库升级的对比
     */
    public void demonstrateDatabaseUpgrade() {
        // 行级隔离：一次DDL操作
        // ALTER TABLE charging_station ADD COLUMN new_field VARCHAR(100);
        // 影响所有租户，操作简单快速
        
        // Schema隔离：需要在每个schema中执行
        // for (String schema : getAllTenantSchemas()) {  // 500个schema
        //     ALTER TABLE ${schema}.charging_station ADD COLUMN new_field VARCHAR(100);
        // }
        // 操作复杂，耗时长，容易出错
    }

    /**
     * 示例6：数据备份恢复的对比
     */
    public void demonstrateBackupRestore() {
        // 行级隔离：统一备份
        // pg_dump evcs_mgr > backup.sql
        // 一个命令备份所有数据
        
        // 恢复特定租户数据：
        // SELECT * FROM charging_station WHERE tenant_id = 100;
        
        // Schema隔离：需要分别处理
        // for (String schema : getAllTenantSchemas()) {
        //     pg_dump -n ${schema} evcs_mgr > backup_${schema}.sql
        // }
        // 500个备份文件，管理复杂
    }

    /**
     * 示例7：性能监控的对比
     */
    public void demonstratePerformanceMonitoring() {
        // 行级隔离：统一监控
        // 监控点：
        // - charging_station表的查询性能
        // - tenant_id字段的索引使用情况
        // - 总体数据量和增长趋势
        
        // Schema隔离：分散监控
        // 需要监控：
        // - tenant_001.charging_station
        // - tenant_002.charging_station
        // - ... (500个表)
        // 监控复杂度线性增长
    }
}

/**
 * 总结：为什么选择行级隔离
 * 
 * 1. 开发效率：开发者无需关心租户隔离逻辑，专注业务开发
 * 2. 运维简单：统一的表结构，简单的备份恢复，容易的升级维护
 * 3. 性能优秀：充分利用数据库索引，高效的跨租户查询
 * 4. 扩展性强：支持大量租户，快速创建，动态管理
 * 5. 业务匹配：完美支持层级租户管理和跨租户数据分析
 * 
 * Schema隔离更适合的场景：
 * - 租户数量少（<50个）
 * - 租户间完全独立，无跨租户需求
 * - 对数据隔离安全性要求极高
 * - 单租户数据量巨大，需要物理隔离
 */