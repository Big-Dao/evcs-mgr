package com.evcs.payment.service.reconciliation;

import com.alipay.api.AlipayApiException;
import com.evcs.payment.config.AlipayConfig;
import com.evcs.payment.config.MockPaymentMetricsConfig;
import com.evcs.payment.config.TestRedisConfig;
import com.evcs.payment.service.channel.AlipayClientFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 支付宝对账服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {TestRedisConfig.class, MockPaymentMetricsConfig.class})
@DisplayName("支付宝对账服务测试")
class AlipayReconciliationServiceTest {

    @MockBean
    private AlipayClientFactory alipayClientFactory;

    @MockBean
    private AlipayConfig alipayConfig;

    private AlipayReconciliationService alipayReconciliationService;

    @Test
    @DisplayName("测试金额解析功能")
    void testParseAmount() {
        // 使用反射调用私有方法测试金额解析
        try {
            java.lang.reflect.Method method = AlipayReconciliationService.class
                .getDeclaredMethod("parseAmount", String.class);
            method.setAccessible(true);

            AlipayReconciliationService service = new AlipayReconciliationService();

            // 测试正常金额
            BigDecimal result1 = (BigDecimal) method.invoke(service, "100.50");
            assertEquals(new BigDecimal("100.50"), result1);

            // 测试空字符串
            BigDecimal result2 = (BigDecimal) method.invoke(service, "");
            assertEquals(BigDecimal.ZERO, result2);

            // 测试null
            BigDecimal result3 = (BigDecimal) method.invoke(service, (String) null);
            assertEquals(BigDecimal.ZERO, result3);

            // 测试无效格式
            BigDecimal result4 = (BigDecimal) method.invoke(service, "invalid");
            assertEquals(BigDecimal.ZERO, result4);

        } catch (Exception e) {
            fail("测试金额解析功能失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试对账单记录解析")
    void testParseBillRecord() {
        try {
            java.lang.reflect.Method method = AlipayReconciliationService.class
                .getDeclaredMethod("parseBillLine", String.class);
            method.setAccessible(true);

            AlipayReconciliationService service = new AlipayReconciliationService();

            // 构造测试数据行
            String testLine = "2024110200001000000000000001,2024110222001234567890,交易支付成功,测试商品,2024-11-02 10:00:00,2024-11-02 10:05:00,store001,测试门店,cashier001,terminal001,user@example.com,100.00,0.00,0.00,0.00,0.00,0.00,0.00,,0.00,,0.00,0.00,0.00,0.00,0.60";

            AlipayReconciliationService.AlipayBillRecord record =
                (AlipayReconciliationService.AlipayBillRecord) method.invoke(service, testLine);

            assertNotNull(record);
            assertEquals("2024110200001000000000000001", record.getOutTradeNo());
            assertEquals("2024110222001234567890", record.getTradeNo());
            assertEquals("交易支付成功", record.getBusinessType());
            assertEquals("测试商品", record.getGoodsName());
            assertEquals("2024-11-02 10:00:00", record.getCreateTime());
            assertEquals("2024-11-02 10:05:00", record.getFinishTime());
            assertEquals(new BigDecimal("100.00"), record.getTotalAmount());
            assertEquals(new BigDecimal("0.60"), record.getServiceFee());

        } catch (Exception e) {
            fail("测试对账单记录解析失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试对账单内容解析")
    void testParseBillContent() {
        try {
            java.lang.reflect.Method method = AlipayReconciliationService.class
                .getDeclaredMethod("parseBillContent", String.class);
            method.setAccessible(true);

            AlipayReconciliationService service = new AlipayReconciliationService();

            // 构造测试对账单内容
            String billContent = "# 支付宝交易明细账单\n" +
                "# 账单时间：2024-11-02\n" +
                "# 商户订单号,支付宝交易号,业务类型,商品名称,创建时间,完成时间,门店编号,门店名称,操作员,终端号,对方账号,订单金额,商家红包,集分宝,支付宝红包,商家优惠,商家优惠金额,券核销金额,券名称,卡消费金额,卡品牌,分期付款金额,分期付款手续费,分期付款净额,退款金额,资金服务费,实收金额,备注,分润明细\n" +
                "2024110200001000000000000001,2024110222001234567890,交易支付成功,测试商品1,2024-11-02 10:00:00,2024-11-02 10:05:00,store001,测试门店,cashier001,terminal001,user1@example.com,100.00,0.00,0.00,0.00,0.00,0.00,0.00,,0.00,,0.00,0.00,0.00,0.00,0.60,99.40,测试交易1,\n" +
                "2024110200001000000000000002,2024110222001234567891,交易支付成功,测试商品2,2024-11-02 11:00:00,2024-11-02 11:05:00,store002,测试门店,cashier002,terminal002,user2@example.com,200.00,0.00,0.00,0.00,0.00,0.00,0.00,,0.00,,0.00,0.00,0.00,0.00,1.20,198.80,测试交易2,\n";

            @SuppressWarnings("unchecked")
            List<AlipayReconciliationService.AlipayBillRecord> records =
                (List<AlipayReconciliationService.AlipayBillRecord>) method.invoke(service, billContent);

            assertNotNull(records);
            assertEquals(2, records.size());

            // 验证第一条记录
            AlipayReconciliationService.AlipayBillRecord record1 = records.get(0);
            assertEquals("2024110200001000000000000001", record1.getOutTradeNo());
            assertEquals(new BigDecimal("100.00"), record1.getTotalAmount());

            // 验证第二条记录
            AlipayReconciliationService.AlipayBillRecord record2 = records.get(1);
            assertEquals("2024110200001000000000000002", record2.getOutTradeNo());
            assertEquals(new BigDecimal("200.00"), record2.getTotalAmount());

        } catch (Exception e) {
            fail("测试对账单内容解析失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试对账单记录字段访问")
    void testBillRecordGettersSetters() {
        AlipayReconciliationService.AlipayBillRecord record = new AlipayReconciliationService.AlipayBillRecord();

        // 测试setter和getter
        record.setOutTradeNo("TEST_OUT_TRADE_NO");
        record.setTradeNo("TEST_TRADE_NO");
        record.setBusinessType("交易支付成功");
        record.setTotalAmount(new BigDecimal("100.00"));

        assertEquals("TEST_OUT_TRADE_NO", record.getOutTradeNo());
        assertEquals("TEST_TRADE_NO", record.getTradeNo());
        assertEquals("交易支付成功", record.getBusinessType());
        assertEquals(new BigDecimal("100.00"), record.getTotalAmount());
    }
}