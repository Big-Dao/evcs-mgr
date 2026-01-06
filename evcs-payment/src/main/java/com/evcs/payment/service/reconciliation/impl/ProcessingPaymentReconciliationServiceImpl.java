package com.evcs.payment.service.reconciliation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.common.tenant.TenantContext;
import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.evcs.payment.service.reconciliation.ProcessingPaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingPaymentReconciliationServiceImpl implements ProcessingPaymentReconciliationService {

    private static final long SYSTEM_USER_ID = 0L;
    private static final int SYSTEM_TENANT_TYPE = 1;

    private final PaymentConfig paymentConfig;
    private final PaymentOrderMapper paymentOrderMapper;
    private final IPaymentService paymentService;
    private final PaymentMetrics paymentMetrics;

    @Override
    public ProcessingPaymentReconciliationResult reconcileOnce() {
        PaymentConfig.ProcessingReconcileConfig cfg = paymentConfig.getProcessingReconcile();

        long startMs = System.currentTimeMillis();
        int tenantCount = 0;
        int scannedOrderCount = 0;
        int finalizedOrderCount = 0;
        int errorCount = 0;

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(cfg.getMinAgeMinutes());

        List<Long> tenantIds;
        CustomTenantLineHandler.disableTenantFilter();
        try {
            tenantIds = paymentOrderMapper.selectTenantIdsWithStatusBefore(
                PaymentStatus.PROCESSING.getCode(),
                cutoff,
                cfg.getMaxTenantsPerRun()
            );
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }

        if (tenantIds == null || tenantIds.isEmpty()) {
            paymentMetrics.recordCustomMetric(
                "payment.processing_reconcile.noop",
                1.0,
                Map.of("result", "no_tenants")
            );
            return new ProcessingPaymentReconciliationResult(0, 0, 0, 0);
        }

        tenantCount = tenantIds.size();

        for (Long tenantId : tenantIds) {
            if (tenantId == null) {
                continue;
            }

            try {
                TenantContext.clear();
                TenantContext.setTenantId(tenantId);
                TenantContext.setUserId(SYSTEM_USER_ID);
                TenantContext.setTenantType(SYSTEM_TENANT_TYPE);

                List<PaymentOrder> orders = paymentOrderMapper.selectList(
                    new LambdaQueryWrapper<PaymentOrder>()
                        .eq(PaymentOrder::getStatus, PaymentStatus.PROCESSING.getCode())
                        .le(PaymentOrder::getCreateTime, cutoff)
                        .isNotNull(PaymentOrder::getTradeNo)
                        .orderByAsc(PaymentOrder::getCreateTime)
                        .last("LIMIT " + cfg.getBatchSizePerTenant())
                );

                if (orders == null || orders.isEmpty()) {
                    continue;
                }

                scannedOrderCount += orders.size();

                for (PaymentOrder order : orders) {
                    if (order == null || !StringUtils.hasText(order.getTradeNo())) {
                        continue;
                    }

                    try {
                        PaymentMethod method = resolvePaymentMethod(order.getPaymentMethod());
                        if (method == null) {
                            continue;
                        }

                        IPaymentChannel channel = paymentService.selectChannel(method);
                        PaymentResponse response = channel.queryPayment(order.getTradeNo());
                        PaymentStatus status = response != null ? response.getStatus() : null;

                        if (PaymentStatus.SUCCESS.equals(status)) {
                            boolean handled = paymentService.handlePaymentCallback(order.getTradeNo(), true);
                            if (handled) {
                                finalizedOrderCount++;
                            }
                        } else if (PaymentStatus.FAILED.equals(status)) {
                            boolean handled = paymentService.handlePaymentCallback(order.getTradeNo(), false);
                            if (handled) {
                                finalizedOrderCount++;
                            }
                        }

                    } catch (Exception ex) {
                        errorCount++;
                        log.warn(
                            "PROCESSING订单轮询补偿失败: tenantId={}, paymentId={}, tradeNo={}",
                            tenantId,
                            order.getId(),
                            order.getTradeNo(),
                            ex
                        );
                    }
                }

            } catch (Exception ex) {
                errorCount++;
                log.warn("PROCESSING订单轮询补偿租户处理失败: tenantId={}", tenantId, ex);
            } finally {
                TenantContext.clear();
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        paymentMetrics.recordCustomMetric(
            "payment.processing_reconcile.run",
            1.0,
            Map.of("result", errorCount == 0 ? "success" : "partial")
        );
        paymentMetrics.recordTimer(
            "payment.processing_reconcile.duration",
            durationMs,
            Map.of("result", errorCount == 0 ? "success" : "partial")
        );

        log.info(
            "PROCESSING订单轮询补偿完成: tenants={}, scannedOrders={}, finalizedOrders={}, errors={}, durationMs={}",
            tenantCount,
            scannedOrderCount,
            finalizedOrderCount,
            errorCount,
            durationMs
        );

        return new ProcessingPaymentReconciliationResult(
            tenantCount,
            scannedOrderCount,
            finalizedOrderCount,
            errorCount
        );
    }

    private PaymentMethod resolvePaymentMethod(String paymentMethodCode) {
        if (!StringUtils.hasText(paymentMethodCode)) {
            return null;
        }
        try {
            return PaymentMethod.valueOf(paymentMethodCode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("无法识别支付方式，跳过轮询: paymentMethod={}", paymentMethodCode);
            return null;
        }
    }
}
