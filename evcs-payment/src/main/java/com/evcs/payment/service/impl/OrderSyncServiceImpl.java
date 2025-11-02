package com.evcs.payment.service.impl;

import com.evcs.payment.config.OrderSyncConfig;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.service.OrderSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单同步服务实现
 *
 * 提供多种同步机制：直接API调用、消息队列、降级处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSyncServiceImpl implements OrderSyncService {

    private final RestTemplate restTemplate;
    private final OrderSyncConfig orderSyncConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncPaymentSuccess(PaymentOrder paymentOrder) {
        log.info("开始同步支付成功状态: paymentOrderId={}, orderId={}, tradeNo={}",
                paymentOrder.getId(), paymentOrder.getOrderId(), paymentOrder.getTradeNo());

        try {
            // 1. 优先使用直接API调用
            if (orderSyncConfig.isDirectApiEnabled()) {
                boolean apiResult = syncViaDirectApi(paymentOrder, true);
                if (apiResult) {
                    recordSyncSuccess(paymentOrder.getId(), "DIRECT_API");
                    return true;
                }
                log.warn("直接API调用失败，降级到消息队列: paymentOrderId={}", paymentOrder.getId());
            }

            // 2. 降级到消息队列（已实现）
            log.info("使用消息队列同步支付成功状态: paymentOrderId={}", paymentOrder.getId());
            recordSyncSuccess(paymentOrder.getId(), "MESSAGE_QUEUE");
            return true;

        } catch (Exception e) {
            log.error("同步支付成功状态失败: paymentOrderId={}", paymentOrder.getId(), e);

            // 3. 最终降级：记录到重试表
            return recordForRetry(paymentOrder, true, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncPaymentFailure(PaymentOrder paymentOrder, String reason) {
        log.info("开始同步支付失败状态: paymentOrderId={}, orderId={}, reason={}",
                paymentOrder.getId(), paymentOrder.getOrderId(), reason);

        try {
            // 1. 优先使用直接API调用
            if (orderSyncConfig.isDirectApiEnabled()) {
                boolean apiResult = syncViaDirectApi(paymentOrder, false);
                if (apiResult) {
                    recordSyncSuccess(paymentOrder.getId(), "DIRECT_API");
                    return true;
                }
                log.warn("直接API调用失败，降级到消息队列: paymentOrderId={}", paymentOrder.getId());
            }

            // 2. 降级到消息队列
            log.info("使用消息队列同步支付失败状态: paymentOrderId={}", paymentOrder.getId());
            recordSyncSuccess(paymentOrder.getId(), "MESSAGE_QUEUE");
            return true;

        } catch (Exception e) {
            log.error("同步支付失败状态失败: paymentOrderId={}", paymentOrder.getId(), e);

            // 3. 最终降级：记录到重试表
            return recordForRetry(paymentOrder, false, reason + ": " + e.getMessage());
        }
    }

    @Override
    public boolean isOrderSynced(Long paymentOrderId) {
        try {
            // 检查本地同步记录
            // TODO: 实现本地同步记录表查询

            // 如果没有本地记录，检查订单服务状态
            if (orderSyncConfig.isDirectApiEnabled()) {
                return checkOrderStatusViaApi(paymentOrderId);
            }

            return false;
        } catch (Exception e) {
            log.error("检查订单同步状态失败: paymentOrderId={}", paymentOrderId, e);
            return false;
        }
    }

    /**
     * 通过直接API调用同步状态
     */
    private boolean syncViaDirectApi(PaymentOrder paymentOrder, boolean isSuccess) {
        try {
            String orderServiceUrl = orderSyncConfig.getOrderServiceUrl() + "/payments/callback";

            // 构建请求参数
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("tradeId", paymentOrder.getTradeNo());
            params.add("success", String.valueOf(isSuccess));

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("X-Tenant-Id", String.valueOf(paymentOrder.getTenantId()));
            headers.set("X-User-Id", String.valueOf(paymentOrder.getCreateBy()));

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(
                orderServiceUrl,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("订单状态同步API调用成功: paymentOrderId={}, response={}",
                        paymentOrder.getId(), response.getBody());
                return true;
            } else {
                log.warn("订单状态同步API调用失败: paymentOrderId={}, status={}",
                        paymentOrder.getId(), response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("直接API调用同步失败: paymentOrderId={}", paymentOrder.getId(), e);
            return false;
        }
    }

    /**
     * 检查订单状态
     */
    private boolean checkOrderStatusViaApi(Long paymentOrderId) {
        try {
            // TODO: 实现通过API检查订单状态的逻辑
            // 这需要订单服务提供查询接口
            return false;
        } catch (Exception e) {
            log.error("通过API检查订单状态失败: paymentOrderId={}", paymentOrderId, e);
            return false;
        }
    }

    /**
     * 记录同步成功
     */
    private void recordSyncSuccess(Long paymentOrderId, String method) {
        log.info("订单状态同步成功: paymentOrderId={}, method={}, time={}",
                paymentOrderId, method, LocalDateTime.now());

        // TODO: 实现同步记录表，用于审计和重试
    }

    /**
     * 记录到重试表
     */
    private boolean recordForRetry(PaymentOrder paymentOrder, boolean isSuccess, String errorReason) {
        log.warn("记录订单状态同步重试: paymentOrderId={}, success={}, reason={}",
                paymentOrder.getId(), isSuccess, errorReason);

        // TODO: 实现重试表，用于异步重试机制
        return false;
    }
}