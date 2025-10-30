package com.evcs.common.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.evcs.common.exception.TenantContextMissingException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Arrays;
import java.util.List;

import net.sf.jsqlparser.schema.Column;

/**
 * MyBatis Plus 多租户处理器
 * 自动在SQL中添加租户条件，实现数据库级别的数据隔离
 */
@Slf4j
public class CustomTenantLineHandler implements TenantLineHandler {
    
    /**
     * 线程本地变量：用于临时禁用租户拦截器
     * 当设置为 true 时，租户拦截器将跳过所有表的租户过滤
     */
    private static final ThreadLocal<Boolean> IGNORE_TENANT_FILTER = new ThreadLocal<>();
    
    /**
     * 禁用租户拦截器
     * 在需要跨租户查询（如父子租户聚合）时调用
     */
    public static void disableTenantFilter() {
        IGNORE_TENANT_FILTER.set(true);
        log.debug("已禁用租户拦截器");
    }
    
    /**
     * 启用租户拦截器（恢复默认行为）
     */
    public static void enableTenantFilter() {
        IGNORE_TENANT_FILTER.remove();
        log.debug("已启用租户拦截器");
    }
    
    /**
     * 检查是否禁用了租户拦截器
     */
    public static boolean isTenantFilterDisabled() {
        Boolean disabled = IGNORE_TENANT_FILTER.get();
        return disabled != null && disabled;
    }
    
    /**
     * 不需要进行租户隔离的表名列表
     */
    private static final List<String> IGNORE_TABLES = Arrays.asList(
        // 系统管理表（系统级别数据，不需要租户隔离）
        "sys_tenant",           // 租户表本身
        "sys_dict_type",        // 字典类型表
        "sys_dict_data",        // 字典数据表
        "sys_config",           // 系统配置表
        "sys_log",              // 系统日志表
        
        // 基础数据表（如果有全局共享的基础数据）
        "base_region",          // 行政区域表
        "base_bank",            // 银行信息表
        
        // 统计分析表（如果需要跨租户统计）
        "stat_global",          // 全局统计表
        
        // 临时表或缓存表
        "temp_",               // 临时表前缀
        "cache_"               // 缓存表前缀
    );
    
    /**
     * 获取租户字段名
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
    
    /**
     * 获取租户ID值
     */
    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.error("租户上下文中未找到租户ID，无法执行SQL操作");
            throw new TenantContextMissingException("执行数据库操作时租户上下文缺失，请确保已正确设置租户信息");
        }
        
        log.debug("SQL租户过滤 - 租户ID: {}", tenantId);
        return new LongValue(tenantId);
    }
    
    /**
     * 判断是否忽略租户隔离
     */
    @Override
    public boolean ignoreTable(String tableName) {
        // 检查是否全局禁用了租户拦截器
        if (isTenantFilterDisabled()) {
            log.debug("租户拦截器已禁用，跳过表 {} 的租户过滤", tableName);
            return true;
        }
        
        // 检查表名是否在忽略列表中
        for (String ignoreTable : IGNORE_TABLES) {
            if (tableName.equals(ignoreTable) || 
                (ignoreTable.endsWith("_") && tableName.startsWith(ignoreTable))) {
                log.debug("忽略租户隔离的表: {}", tableName);
                return true;
            }
        }
        
        // 对于系统管理员，某些操作可能需要跨租户访问
        if (TenantContext.isSystemAdmin()) {
            log.debug("系统管理员访问表: {}", tableName);
            // 系统管理员仍然需要租户隔离，除非特殊指定
            // 这里可以根据业务需求进行调整
        }
        
        log.debug("应用租户隔离的表: {}", tableName);
        return false;
    }
    
    /**
     * 在INSERT语句中是否忽略租户字段
     * 返回true表示不自动添加租户字段，由业务代码自行处理
     */
    @Override
    public boolean ignoreInsert(List<Column> columns, String tenantIdColumn) {
        // 如果INSERT语句中已经包含了租户字段，则不再自动添加
        boolean hasColumn = columns.stream()
            .map(Column::getColumnName)
            .anyMatch(column -> column.equalsIgnoreCase(tenantIdColumn));
            
        if (hasColumn) {
            log.debug("INSERT语句已包含租户字段，跳过自动添加");
            return true;
        }
        
        // 检查当前是否有租户上下文
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            log.warn("INSERT操作缺少租户上下文，可能导致数据插入异常");
            return true; // 没有租户上下文时，不自动添加租户字段
        }
        
        log.debug("INSERT语句自动添加租户字段: {} = {}", tenantIdColumn, tenantId);
        return false; // 自动添加租户字段
    }
}