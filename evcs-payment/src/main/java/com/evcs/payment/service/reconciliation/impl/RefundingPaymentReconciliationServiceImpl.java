package com.evcs.payment.service.reconciliation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evcs.common.tenant.CustomTenantLineHandler;
import com.evcs.common.tenant.TenantContext;
import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.evcs.payment.service.reconciliation.RefundingPaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundingPaymentReconciliationServiceImpl implements RefundingPaymentReconciliationService {

    private static final long SYSTEM_USER_ID = 0L;
    private static final int SYSTEM_TENANT_TYPE = 1;

    private final PaymentConfig paymentConfig;
    private final PaymentOrderMapper paymentOrderMapper;
    private final IPaymentService paymentService;
    private final PaymentMetrics paymentMetrics;

    @Override
    public RefundingPaymentReconciliationResult reconcileOnce() {
        PaymentConfig.RefundingReconcileConfig cfg = paymentConfig.getRefundingReconcile();

        long startMs = System.currentTimeMillis();
        int tenantCount = 0;
        int scannedOrderCount = 0;
        int finalizedOrderCount = 0;
        int errorCount = 0;

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(cfg.getMinAgeMinutes());

        List<Long> tenantIds;
        CustomTenantLineHandler.disableTenantFilter();
        try {
            tenantIds = paymentOrderMapper.selectTenantIdsWithRefundingStatusBefore(
                PaymentStatus.REFUNDING.getCode(),
                cutoff,
                cfg.getMaxTenantsPerRun()
            );
        } finally {
            CustomTenantLineHandler.enableTenantFilter();
        }

        if (tenantIds == null || tenantIds.isEmpty()) {
            paymentMetrics.recordCustomMetric(
                "payment.refunding_reconcile.noop",
                1.0,
                Map.of("result", "no_tenants")
            );
            return new RefundingPaymentReconciliationResult(0, 0, 0, 0);
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
                        .eq(PaymentOrder::getStatus, PaymentStatus.REFUNDING.getCode())
                        .isNotNull(PaymentOrder::getRefundRequestNo)
                        .ne(PaymentOrder::getRefundRequestNo, "")
                        .and(w -> w
                            .le(PaymentOrder::getRefundRequestTime, cutoff)
                            .or()
                            .isNull(PaymentOrder::getRefundRequestTime)
                            .le(PaymentOrder::getCreateTime, cutoff)
                        )
                        .orderByAsc(PaymentOrder::getCreateTime)
                        .last("LIMIT " + cfg.getBatchSizePerTenant())
                );

                if (orders == null || orders.isEmpty()) {
                    continue;
                }

                scannedOrderCount += orders.size();

                for (PaymentOrder order : orders) {
                    if (order == null || !StringUtils.hasText(order.getRefundRequestNo())) {
                        continue;
                    }

                    try {
                        PaymentMethod method = resolvePaymentMethod(order.getPaymentMethod());
                        if (method == null) {
                            continue;
                        }

                        IPaymentChannel channel = paymentService.selectChannel(method);
                        RefundResponse response = channel.queryRefund(order.getRefundRequestNo(), order.getTradeNo());
                        String refundStatus = response != null ? response.getRefundStatus() : null;

                        if (refundStatus == null || refundStatus.trim().isEmpty()) {
                            continue;
                        }

                        String normalized = refundStatus.toUpperCase(Locale.ROOT);
                        if ("PROCESSING".equals(normalized)) {
                            continue;
                        }

                        boolean converged = paymentService.handleRefundFinalStatus(
                            order.getId(),
                            order.getRefundRequestNo(),
                            refundStatus,
                            response != null ? response.getRefundAmount() : null
                        );

                        if (converged) {
                            finalizedOrderCount++;
                        }

                    } catch (UnsupportedOperationException ex) {
                        log.info(
                            "渠道不支持退款查询，跳过REFUNDING轮询: tenantId={}, paymentId={}, paymentMethod={}",
                            tenantId,
                            order.getId(),
                            order.getPaymentMethod()
                        );
                    } catch (Exception ex) {
                        errorCount++;
                        log.warn(
                            "REFUNDING订单轮询补偿失败: tenantId={}, paymentId={}, refundRequestNo={}",
                            tenantId,
                            order.getId(),
                            order.getRefundRequestNo(),
                            ex
                        );
                    }
                }

            } catch (Exception ex) {
                errorCount++;
                log.warn("REFUNDING订单轮询补偿租户处理失败: tenantId={}", tenantId, ex);
            } finally {
                TenantContext.clear();
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        paymentMetrics.recordCustomMetric(
            "payment.refunding_reconcile.run",
            1.0,
            Map.of("result", errorCount == 0 ? "success" : "partial")
        );
        paymentMetrics.recordTimer(
            "payment.refunding_reconcile.duration",
            durationMs,
            Map.of("result", errorCount == 0 ? "success" : "partial")
        );

        log.info(
            "REFUNDING订单轮询补偿完成: tenants={}, scannedOrders={}, finalizedOrders={}, errors={}, durationMs={}",
            tenantCount,
            scannedOrderCount,
            finalizedOrderCount,
            errorCount,
            durationMs
        );

        return new RefundingPaymentReconciliationResult(
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
            return PaymentMethod.valueOf(paymentMethodCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("无法识别支付方式，跳过轮询: paymentMethod={}", paymentMethodCode);
            return null;
        }
    }
}
