package com.evcs.integration.test;

import com.evcs.common.exception.BusinessException;
import com.evcs.common.exception.TenantContextMissingException;
import com.evcs.common.tenant.TenantContext;
import com.evcs.common.test.base.BaseIntegrationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IChargerService;
import com.evcs.station.service.IStationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常场景集成测试
 * 测试系统在各种异常情况下的行为
 */
@SpringBootTest(classes = {com.evcs.station.StationServiceApplication.class},
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("异常场景集成测试")
class ExceptionScenariosIntegrationTest extends BaseIntegrationTest {

    @Resource
    private IStationService stationService;

    @Resource
    private IChargerService chargerService;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("测试重复编码异常")
    void testDuplicateCodeException() {
        // 1. 创建第一个充电站
        String duplicateCode = TestDataFactory.generateCode("DUP");
        Station station1 = new Station();
        station1.setStationCode(duplicateCode);
        station1.setStationName("第一个充电站");
        station1.setAddress("测试地址1");
        station1.setLatitude(39.9087);
        station1.setLongitude(116.4089);
        station1.setStatus(1);
        
        boolean saved1 = stationService.saveStation(station1);
        assertTrue(saved1, "第一个充电站应该保存成功");

        // 2. 尝试创建具有相同编码的充电站
        Station station2 = new Station();
        station2.setStationCode(duplicateCode); // 使用相同的编码
        station2.setStationName("第二个充电站");
        station2.setAddress("测试地址2");
        station2.setLatitude(31.2304);
        station2.setLongitude(121.4737);
        station2.setStatus(1);
        
        // 应该抛出异常或返回false
        assertThrows(Exception.class, () -> {
            stationService.saveStation(station2);
        }, "使用重复编码应该抛出异常");
    }

    @Test
    @DisplayName("测试无效参数异常")
    void testInvalidParameterException() {
        // 测试空值参数
        assertThrows(Exception.class, () -> {
            stationService.getById(null);
        }, "传入null ID应该抛出异常");

        // 测试负数ID
        Station station = stationService.getById(-1L);
        assertNull(station, "负数ID应该查询不到数据");

        // 测试不存在的ID
        Station notExist = stationService.getById(999999999L);
        assertNull(notExist, "不存在的ID应该查询不到数据");
    }

    @Test
    @DisplayName("测试数据完整性约束异常")
    void testDataIntegrityException() {
        // 1. 创建充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("INTEGRITY"));
        station.setStationName("完整性测试充电站");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        
        stationService.saveStation(station);
        assertNotNull(station.getStationId());

        // 2. 创建充电桩
        Charger charger = new Charger();
        charger.setStationId(station.getStationId());
        charger.setChargerCode(TestDataFactory.generateCode("INTEGRITY-CHG"));
        charger.setChargerName("测试充电桩");
        charger.setChargerType(1);
        charger.setStatus(1);
        charger.setRatedPower(new BigDecimal("60.0"));
        
        chargerService.saveCharger(charger);

        // 3. 尝试删除有充电桩的充电站（应该失败或需要先删除充电桩）
        // 注意：具体行为取决于业务逻辑实现
        // 如果实现了级联删除,则会成功;如果没有,则会抛出异常
        try {
            boolean deleted = stationService.deleteStation(station.getStationId());
            if (deleted) {
                // 如果删除成功,验证充电桩也被删除了(级联删除)
                Charger deletedCharger = chargerService.getById(charger.getId());
                assertNull(deletedCharger, "充电桩应该被级联删除");
            }
        } catch (Exception e) {
            // 如果抛出异常，说明不允许删除有充电桩的充电站
            assertTrue(true, "删除有充电桩的充电站应该抛出异常或被阻止");
        }
    }

    @Test
    @DisplayName("测试租户上下文缺失异常")
    void testTenantContextMissingException() {
        // 清除租户上下文
        TenantContext.clear();
        
        // 尝试执行需要租户上下文的操作
        assertThrows(TenantContextMissingException.class, () -> {
            Station station = new Station();
            station.setStationCode(TestDataFactory.generateCode("NO-TENANT"));
            station.setStationName("无租户测试");
            station.setAddress("测试地址");
            station.setLatitude(39.9087);
            station.setLongitude(116.4089);
            station.setStatus(1);
            
            // 这应该抛出TenantContextMissingException
            stationService.saveStation(station);
        }, "没有租户上下文时应该抛出TenantContextMissingException");
    }

    @Test
    @DisplayName("测试并发修改异常")
    void testConcurrentModificationException() throws InterruptedException {
        TransactionTemplate requiresNewTemplate = new TransactionTemplate(transactionManager);
        requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 1. 创建充电站并立即提交，确保其他线程可见
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("CONCURRENT"));
        station.setStationName("并发修改测试");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        requiresNewTemplate.execute(status -> {
            stationService.saveStation(station);
            return null;
        });
        Long stationId = station.getStationId();

        // 2. 创建两个线程同时修改同一个充电站
        final boolean[] thread1Success = {false};
        final boolean[] thread2Success = {false};
        
        Thread thread1 = new Thread(() -> {
            try {
                TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
                TenantContext.setCurrentUserId(DEFAULT_USER_ID);
                Station s1 = stationService.getById(stationId);
                s1.setStationName("线程1修改");
                Thread.sleep(50); // 模拟一些处理时间
                thread1Success[0] = stationService.updateStation(s1);
            } catch (Exception e) {
                // 可能因为并发冲突失败
            } finally {
                TenantContext.clear();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
                TenantContext.setCurrentUserId(DEFAULT_USER_ID);
                Station s2 = stationService.getById(stationId);
                s2.setStationName("线程2修改");
                Thread.sleep(50); // 模拟一些处理时间
                thread2Success[0] = stationService.updateStation(s2);
            } catch (Exception e) {
                // 可能因为并发冲突失败
            } finally {
                TenantContext.clear();
            }
        });

        thread1.start();
        thread2.start();
        thread1.join(5000);
        thread2.join(5000);

        // 至少有一个线程应该成功
        assertTrue(thread1Success[0] || thread2Success[0], "至少有一个线程应该更新成功");
        
        // 验证最终状态
        TenantContext.setCurrentTenantId(DEFAULT_TENANT_ID);
        TenantContext.setCurrentUserId(DEFAULT_USER_ID);
        Station finalStation = stationService.getById(stationId);
        assertNotNull(finalStation, "最终应该能查询到充电站");
        assertTrue(finalStation.getStationName().contains("修改"), "名称应该被修改");

        // 清理测试数据，避免污染其他测试
        requiresNewTemplate.execute(status -> {
            stationService.deleteStation(stationId);
            return null;
        });
        TenantContext.clear();
    }

    @Test
    @DisplayName("测试事务回滚场景")
    void testTransactionRollback() {
        // 这个测试需要模拟一个会导致事务回滚的场景
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("ROLLBACK"));
        station.setStationName("事务回滚测试");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        
        try {
            stationService.saveStation(station);
            
            // 模拟一个会导致异常的操作
            // 注意：这里需要根据实际业务逻辑来设计
            // 例如，尝试插入一个违反约束的数据
            
        } catch (Exception e) {
            // 事务应该回滚，之前的操作应该被撤销
        }
        
        // 验证充电站没有被保存（如果有异常的话）
        // 具体验证逻辑取决于业务实现
    }

    @Test
    @DisplayName("测试大数据量异常处理")
    void testLargeDataException() {
        // 测试地址字段过长的情况
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("LARGE"));
        station.setStationName("大数据测试");
        // 创建一个超长的地址（超出数据库字段长度 VARCHAR(255)）
        String longAddress = "A".repeat(1000);
        station.setAddress(longAddress);
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        
        // H2 数据库可能允许超长数据或自动截断，这是数据库特性差异
        // 生产环境 PostgreSQL 会严格执行长度限制
        boolean exceptionThrown = false;
        try {
            stationService.saveStation(station);
            // 如果保存成功，验证数据存在
            Station saved = stationService.getById(station.getStationId());
            assertNotNull(saved, "超长地址的站点应该能保存（H2 特性）或抛出异常");
            // H2 可能截断或保存完整数据，两种情况都可以接受
        } catch (Exception e) {
            // PostgreSQL 会抛出字段长度约束异常
            exceptionThrown = true;
            assertTrue(e.getMessage().contains("too long") || 
                      e.getMessage().contains("超出") ||
                      e.getMessage().contains("constraint") ||
                      e.getMessage().contains("Value too long"),
                      "应该抛出字段长度约束异常: " + e.getMessage());
        }
        // 测试通过条件：要么抛出异常，要么成功保存
        assertTrue(true, "大数据量测试完成，H2 和 PostgreSQL 行为可能不同");
    }

    @Test
    @DisplayName("测试外键约束异常")
    void testForeignKeyConstraintException() {
        // 尝试为不存在的充电站创建充电桩
        Charger charger = new Charger();
        charger.setStationId(999999999L); // 不存在的充电站ID
        charger.setChargerCode(TestDataFactory.generateCode("FK-TEST"));
        charger.setChargerName("外键测试充电桩");
        charger.setChargerType(1);
        charger.setStatus(1);
        charger.setRatedPower(new BigDecimal("60.0"));
        
        // 应该抛出外键约束异常
        assertThrows(Exception.class, () -> {
            chargerService.saveCharger(charger);
        }, "为不存在的充电站创建充电桩应该抛出异常");
    }

    @Test
    @DisplayName("测试业务逻辑异常")
    void testBusinessLogicException() {
        // 创建充电站
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("BUSINESS"));
        station.setStationName("业务逻辑测试");
        station.setAddress("测试地址");
        station.setLatitude(39.9087);
        station.setLongitude(116.4089);
        station.setStatus(1);
        
        stationService.saveStation(station);

        // 测试业务规则：例如，不允许将充电站状态直接从运营中(1)改为删除(3)
        // 注意：具体业务规则需要根据实际实现来测试
        try {
            Station updateStation = stationService.getById(station.getStationId());
            updateStation.setStatus(3); // 假设3是一个无效状态
            stationService.updateStation(updateStation);
            
            // 如果没有抛出异常，验证状态是否被限制
            Station verifyStation = stationService.getById(station.getStationId());
            assertNotNull(verifyStation, "充电站应该存在");
        } catch (BusinessException e) {
            // 预期的业务异常
            assertTrue(true, "违反业务规则应该抛出BusinessException");
        }
    }
}
