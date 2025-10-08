package com.evcs.tenant.test;

import com.evcs.common.tenant.TenantContext;
import com.evcs.tenant.entity.SysTenant;
import com.evcs.tenant.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 多租户隔离测试类
 * 验证租户数据隔离是否正常工作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantIsolationTest implements CommandLineRunner {

    private final ISysTenantService tenantService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始执行多租户隔离测试...");
        
        try {
            // 测试1：创建测试租户
            testCreateTenants();
            
            // 测试2：测试租户上下文隔离
            testTenantContextIsolation();
            
            // 测试3：测试数据权限隔离
            testDataScopeIsolation();
            
            log.info("多租户隔离测试完成");
        } catch (Exception e) {
            log.error("多租户隔离测试失败", e);
        }
    }

    /**
     * 测试创建租户
     */
    private void testCreateTenants() {
        log.info("=== 测试1：创建测试租户 ===");
        
        // 设置系统管理员上下文
        TenantContext.setCurrentTenantId(1L);
        
        try {
            // 创建父租户
            SysTenant parentTenant = new SysTenant();
            parentTenant.setTenantCode("PARENT_001");
            parentTenant.setTenantName("父租户1");
            parentTenant.setContactPerson("张三");
            parentTenant.setContactPhone("13800138001");
            parentTenant.setContactEmail("zhangsan@parent.com");
            parentTenant.setStatus(1);
            parentTenant.setCreateTime(LocalDateTime.now());
            parentTenant.setCreateBy(1L);
            
            boolean result1 = tenantService.saveTenant(parentTenant);
            log.info("创建父租户结果: {}, 租户ID: {}", result1, parentTenant.getTenantId());
            
            // 创建子租户
            SysTenant childTenant = new SysTenant();
            childTenant.setTenantCode("CHILD_001");
            childTenant.setTenantName("子租户1");
            childTenant.setParentId(parentTenant.getTenantId());
            childTenant.setContactPerson("李四");
            childTenant.setContactPhone("13800138002");
            childTenant.setContactEmail("lisi@child.com");
            childTenant.setStatus(1);
            childTenant.setCreateTime(LocalDateTime.now());
            childTenant.setCreateBy(1L);
            
            boolean result2 = tenantService.saveTenant(childTenant);
            log.info("创建子租户结果: {}, 租户ID: {}", result2, childTenant.getTenantId());
            
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 测试租户上下文隔离
     */
    private void testTenantContextIsolation() {
        log.info("=== 测试2：测试租户上下文隔离 ===");
        
        // 测试租户1上下文
        TenantContext.setCurrentTenantId(1L);
        try {
            Long tenantId1 = TenantContext.getCurrentTenantId();
            log.info("当前租户上下文 - 租户ID: {}", tenantId1);
            
            // 在子线程中测试上下文隔离
            Thread thread1 = new Thread(() -> {
                TenantContext.setCurrentTenantId(2L);
                Long threadTenantId = TenantContext.getCurrentTenantId();
                log.info("子线程1 - 租户ID: {}", threadTenantId);
            });
            
            Thread thread2 = new Thread(() -> {
                TenantContext.setCurrentTenantId(3L);
                Long threadTenantId = TenantContext.getCurrentTenantId();
                log.info("子线程2 - 租户ID: {}", threadTenantId);
            });
            
            thread1.start();
            thread2.start();
            
            thread1.join();
            thread2.join();
            
            // 验证主线程上下文不受影响
            Long mainTenantId = TenantContext.getCurrentTenantId();
            log.info("主线程 - 租户ID: {}", mainTenantId);
            
        } catch (InterruptedException e) {
            log.error("线程测试异常", e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 测试数据权限隔离
     */
    private void testDataScopeIsolation() {
        log.info("=== 测试3：测试数据权限隔离 ===");
        
        // 以系统管理员身份查询所有租户
        TenantContext.setCurrentTenantId(1L);
        try {
            List<Long> allChildren = tenantService.getTenantChildren(1L);
            log.info("系统管理员可查看的租户: {}", allChildren);
        } finally {
            TenantContext.clear();
        }
        
        // 以普通租户身份查询（应该只能看到自己和子租户）
        TenantContext.setCurrentTenantId(2L);
        try {
            List<Long> tenantChildren = tenantService.getTenantChildren(2L);
            log.info("租户2可查看的租户: {}", tenantChildren);
        } finally {
            TenantContext.clear();
        }
    }
}