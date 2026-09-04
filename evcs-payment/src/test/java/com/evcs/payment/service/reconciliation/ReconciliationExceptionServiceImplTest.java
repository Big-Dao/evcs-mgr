package com.evcs.payment.service.reconciliation;

import com.evcs.payment.dto.ReconciliationExceptionItem;
import com.evcs.payment.dto.ReconciliationExceptionCandidate;
import com.evcs.payment.service.reconciliation.impl.ReconciliationExceptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReconciliationExceptionServiceImpl 单元测试
 */
class ReconciliationExceptionServiceImplTest {

    private ReconciliationExceptionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationExceptionServiceImpl();
    }

    @Test
    void detectExceptions_shouldBuildStructuredExceptions() {
        List<ReconciliationExceptionCandidate> candidates = List.of(
            ReconciliationExceptionCandidate.builder()
                .type(ReconciliationExceptionItem.ExceptionType.AMOUNT_MISMATCH)
                .description("金额差异2.50元")
                .systemTradeNo("SYS001")
                .channelTradeNo("ALI001")
                .systemAmount(new BigDecimal("100.00"))
                .channelAmount(new BigDecimal("102.50"))
                .amountDifference(new BigDecimal("2.50"))
                .systemStatus("SUCCESS")
                .channelStatus("TRADE_SUCCESS")
                .build()
        );

        List<ReconciliationExceptionItem> exceptions = service.detectExceptions("recon-1", candidates);

        assertEquals(1, exceptions.size());
        ReconciliationExceptionItem exception = exceptions.get(0);
        assertEquals(ReconciliationExceptionItem.ExceptionType.AMOUNT_MISMATCH, exception.getType());
        assertEquals(ReconciliationExceptionItem.ExceptionLevel.MEDIUM, exception.getLevel());
        assertEquals("SYS001", exception.getSystemTradeNo());
        assertEquals(new BigDecimal("2.50"), exception.getAmountDifference());
    }

    @Test
    void handleException_shouldResolveAndAddRemark() {
        ReconciliationExceptionItem exception = ReconciliationExceptionItem.builder()
            .id("ex-1")
            .reconciliationId("recon-1")
            .type(ReconciliationExceptionItem.ExceptionType.AMOUNT_MISMATCH)
            .systemTradeNo("SYS002")
            .channelTradeNo("ALI002")
            .systemAmount(new BigDecimal("120.00"))
            .channelAmount(new BigDecimal("118.00"))
            .amountDifference(new BigDecimal("2.00"))
            .systemStatus("SUCCESS")
            .channelStatus("TRADE_SUCCESS")
            .status(ReconciliationExceptionItem.ExceptionStatus.PENDING)
            .build();

        boolean handled = service.handleException(exception);

        assertTrue(handled);
        assertEquals(ReconciliationExceptionItem.ExceptionStatus.RESOLVED, exception.getStatus());
        assertNotNull(exception.getHandleRemark());
        assertTrue(exception.getHandleRemark().contains("金额差异"));
        assertNotNull(exception.getHandleTime());
    }

    @Test
    void generateExceptionReport_shouldReuseLatestDetectionResult() {
        List<ReconciliationExceptionCandidate> candidates = List.of(
            ReconciliationExceptionCandidate.builder()
                .type(ReconciliationExceptionItem.ExceptionType.TRADE_NOT_FOUND)
                .description("系统缺少渠道交易")
                .channelTradeNo("ALI003")
                .channelAmount(new BigDecimal("88.00"))
                .build()
        );
        service.detectExceptions("recon-report", candidates);

        String report = service.generateExceptionReport("recon-report");

        assertNotNull(report);
        assertTrue(report.contains("recon-report"));
        assertTrue(report.contains("异常总数: 1"));
    }
}
