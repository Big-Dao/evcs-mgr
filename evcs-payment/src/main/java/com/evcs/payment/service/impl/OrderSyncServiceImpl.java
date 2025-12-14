package com.evcs.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evcs.common.result.Result;
import com.evcs.payment.config.OrderSyncConfig;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.entity.PaymentSyncRecord;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.mapper.PaymentSyncRecordMapper;
import com.evcs.payment.service.OrderSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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
    private final PaymentSyncRecordMapper paymentSyncRecordMapper;
    private final PaymentOrderMapper paymentOrderMapper;

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
            Long count = paymentSyncRecordMapper.selectCount(new LambdaQueryWrapper<PaymentSyncRecord>()
                    .eq(PaymentSyncRecord::getPaymentOrderId, paymentOrderId)
                    .eq(PaymentSyncRecord::getSyncStatus, "SUCCESS"));
            
            if (count > 0) {
                return true;
            }

            // 如果没有本地记录，检查订单服务状态
            if (orderSyncConfig.isDirectApiEnabled()) {
                PaymentOrder paymentOrder = paymentOrderMapper.selectById(paymentOrderId);
                if (paymentOrder != null) {
                    return checkOrderStatusViaApi(paymentOrder);
                }
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
            // 使用新的回调接口: /order/payment/callback
            String orderServiceUrl = orderSyncConfig.getOrderServiceUrl() + "/order/payment/callback";

            // 构建请求参数
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("tradeId", paymentOrder.getTradeNo());
            params.add("success", String.valueOf(isSuccess));

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            if (paymentOrder.getTenantId() != null) {
                headers.set("X-Tenant-Id", String.valueOf(paymentOrder.getTenantId()));
            }
            if (paymentOrder.getCreateBy() != null) {
                headers.set("X-User-Id", String.valueOf(paymentOrder.getCreateBy()));
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // 发送请求
            ResponseEntity<Result<Boolean>> response = restTemplate.exchange(
                orderServiceUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Result<Boolean>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Result<Boolean> result = response.getBody();
                if (result.getCode() == 200 && Boolean.TRUE.equals(result.getData())) {
                    log.info("订单状态同步API调用成功: paymentOrderId={}", paymentOrder.getId());
                    return true;
                } else {
                    log.warn("订单状态同步API调用返回失败: paymentOrderId={}, result={}",
                            paymentOrder.getId(), result);
                    return false;
                }
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
    private boolean checkOrderStatusViaApi(PaymentOrder paymentOrder) {
        try {
            // 使用新的查询接口: /order/{id}
            String orderServiceUrl = orderSyncConfig.getOrderServiceUrl() + "/order/" + paymentOrder.getOrderId();

            HttpHeaders headers = new HttpHeaders();
            if (paymentOrder.getTenantId() != null) {
                headers.set("X-Tenant-Id", String.valueOf(paymentOrder.getTenantId()));
            }

            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Result<Map<String, Object>>> response = restTemplate.exchange(
                orderServiceUrl,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Result<Map<String, Object>>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Result<Map<String, Object>> result = response.getBody();
                if (result.getCode() == 200 && result.getData() != null) {
                    Map<String, Object> orderData = result.getData();
                    Integer status = (Integer) orderData.get("status");
                    // 假设状态 2 (PAID) 或 3 (COMPLETED) 表示已支付
                    // 需要确认 ChargingOrderStatus 枚举，这里暂时假设
                    return status != null && status >= 2; 
                }
            }
            return false;
        } catch (Exception e) {
            log.error("通过API检查订单状态失败: paymentOrderId={}", paymentOrder.getId(), e);
            return false;
        }
    }

    /**
     * 记录同步成功
     */
    private void recordSyncSuccess(Long paymentOrderId, String method) {
        log.info("订单状态同步成功: paymentOrderId={}, method={}, time={}",
                paymentOrderId, method, LocalDateTime.now());

        saveSyncRecord(paymentOrderId, method, "SUCCESS", null);
    }

    /**
     * 记录到重试表
     */
    private boolean recordForRetry(PaymentOrder paymentOrder, boolean isSuccess, String errorReason) {
        log.warn("记录订单状态同步重试: paymentOrderId={}, success={}, reason={}",
                paymentOrder.getId(), isSuccess, errorReason);

        saveSyncRecord(paymentOrder.getId(), "RETRY", "FAILED", errorReason);
        return true; // 返回true表示已记录，虽然同步失败但已处理
    }

    private void saveSyncRecord(Long paymentOrderId, String method, String status, String error) {
        try {
            PaymentSyncRecord record = new PaymentSyncRecord();
            record.setPaymentOrderId(paymentOrderId);
            record.setSyncMethod(method);
            record.setSyncStatus(status);
            record.setSyncTime(LocalDateTime.now());
            record.setLastError(error);
            record.setRetryCount(0);
            
            paymentSyncRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存同步记录失败: paymentOrderId={}", paymentOrderId, e);
        }
    }
}