package com.evcs.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus配置类
 * 配置多租户插件、分页插件、乐观锁插件等
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {
    
    /**
     * MyBatis Plus拦截器配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 1. 多租户插件（需要放在分页插件之前）
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new CustomTenantLineHandler());
        interceptor.addInnerInterceptor(tenantInterceptor);
        
        // 2. 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit(500L); // 单页最大条数限制
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // 3. 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        log.info("MyBatis Plus插件配置完成：多租户、分页、乐观锁");
        return interceptor;
    }
    
    /**
     * 自动填充配置
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new CustomMetaObjectHandler();
    }
    
    /**
     * 自定义元数据处理器
     * 自动填充创建时间、更新时间、创建者、更新者、租户ID等字段
     */
    @Slf4j
    public static class CustomMetaObjectHandler implements MetaObjectHandler {
        
        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            Long userId = TenantContext.getUserId();
            Long tenantId = TenantContext.getTenantId();
            
            // 自动填充创建时间
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            
            // 自动填充创建者和更新者
            if (userId != null) {
                this.strictInsertFill(metaObject, "createBy", Long.class, userId);
                this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
            }
            
            // 自动填充租户ID（如果实体中有tenant_id字段且未设置值）
            if (tenantId != null && getFieldValByName("tenantId", metaObject) == null) {
                this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
                log.debug("自动填充租户ID: {}", tenantId);
            }
            
            // 自动填充默认值
            this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
            this.strictInsertFill(metaObject, "version", Integer.class, 1);
        }
        
        @Override
        public void updateFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            Long userId = TenantContext.getUserId();
            
            // 自动填充更新时间
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
            
            // 自动填充更新者
            if (userId != null) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
            }
        }
    }
}