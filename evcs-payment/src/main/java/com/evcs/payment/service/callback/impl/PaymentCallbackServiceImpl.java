package com.evcs.payment.service.callback.impl;

import com.evcs.common.tenant.TenantContext;
import com.evcs.payment.dto.CallbackRequest;
import com.evcs.payment.dto.CallbackResponse;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.metrics.PaymentMetrics;
import com.evcs.payment.service.IPaymentService;
import com.evcs.payment.service.OrderSyncService;
import com.evcs.payment.service.callback.PaymentCallbackService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.evcs.payment.service.channel.WechatPayClientFactory;
import com.evcs.payment.service.message.PaymentMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.wechat.pay.java.core.notification.Notification;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 支付回调服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackServiceImpl implements PaymentCallbackService {

    private static final long WECHAT_CALLBACK_MAX_SKEW_SECONDS = 300L;

    private final IPaymentService paymentService;
    private final PaymentMetrics paymentMetrics;
    private final PaymentMessageService paymentMessageService;
    private final OrderSyncService orderSyncService;
    private final Optional<WechatPayClientFactory> wechatPayClientFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallbackResponse handleCallback(String channel, CallbackRequest request) {
        log.info("处理支付回调: channel={}, tradeNo={}, tradeStatus={}",
                channel, request.getTradeNo(), request.getTradeStatus());

        try {
            // 1. 验证签名
            if (!verifySignature(channel, request)) {
                log.warn("支付回调签名验证失败: channel={}, tradeNo={}", channel, request.getTradeNo());
                paymentMetrics.recordCallbackFailure();
                return CallbackResponse.failure("签名验证失败");
            }

            // 2. 记录回调接收
            paymentMetrics.recordCallbackReceived();

            // 3. 查询支付订单
            PaymentOrder paymentOrder = paymentService.getByTradeNo(request.getTradeNo());
            if (paymentOrder == null) {
                log.warn("支付订单不存在: tradeNo={}", request.getTradeNo());
                paymentMetrics.recordCallbackFailure();
                return CallbackResponse.failure("支付订单不存在");
            }

            // 4. 幂等性检查
            if (PaymentStatus.SUCCESS.equals(paymentOrder.getStatusEnum())) {
                log.info("支付订单已经是成功状态，跳过处理: tradeNo={}", request.getTradeNo());
                paymentMetrics.recordCallbackSuccess();
                return createSuccessResponse(channel);
            }

            // 5. 解析支付状态
            boolean isPaymentSuccess = parsePaymentStatus(channel, request.getTradeStatus());

            // 6. 更新支付订单状态
            boolean updated = updatePaymentOrder(paymentOrder, request, isPaymentSuccess);

            if (updated) {
                paymentMetrics.recordCallbackSuccess();

                if (isPaymentSuccess) {
                    // 7. 同步订单状态到订单服务（新增）
                    boolean orderSynced = syncOrderStatus(paymentOrder, true);

                    // 8. 发送业务消息通知订单服务（降级处理）
                    sendPaymentSuccessNotification(paymentOrder);

                    if (!orderSynced) {
                        log.warn("订单状态同步失败，但消息队列通知已发送: orderId={}, tradeNo={}",
                                paymentOrder.getOrderId(), paymentOrder.getTradeNo());
                    }
                } else {
                    // 支付失败也需要同步订单状态
                    boolean orderSynced = syncOrderStatus(paymentOrder, false);
                    if (!orderSynced) {
                        log.warn("支付失败订单状态同步失败: orderId={}, tradeNo={}",
                                paymentOrder.getOrderId(), paymentOrder.getTradeNo());
                    }
                }

                log.info("支付回调处理成功: channel={}, tradeNo={}, success={}",
                        channel, request.getTradeNo(), isPaymentSuccess);

                return createSuccessResponse(channel);
            } else {
                paymentMetrics.recordCallbackFailure();
                return CallbackResponse.failure("更新支付订单失败");
            }

        } catch (Exception e) {
            log.error("处理支付回调异常: channel={}, tradeNo={}", channel, request.getTradeNo(), e);
            paymentMetrics.recordCallbackFailure();
            return CallbackResponse.failure("内部处理异常");
        }
    }

    @Override
    public boolean verifySignature(String channel, CallbackRequest request) {
        if ("wechat".equalsIgnoreCase(channel)) {
            Boolean verified = verifyWechatSignature(request);
            if (verified != null) {
                return verified;
            }
        }

        PaymentMethod method = resolvePaymentMethod(channel);
        if (method == null) {
            log.warn("未找到支付渠道: {}", channel);
            return false;
        }

        try {
            IPaymentChannel paymentChannel = paymentService.selectChannel(method);
            if (paymentChannel == null) {
                log.warn("支付渠道未注册: {}", channel);
                return false;
            }
            return paymentChannel.verifySignature(request.getRawData(), request.getSign());
        } catch (Exception e) {
            log.error("验证签名失败: channel={}", channel, e);
            return false;
        }
    }

    private Boolean verifyWechatSignature(CallbackRequest request) {
        Map<String, String> headers = request.getHeaders();
        if (headers == null || headers.isEmpty()) {
            log.warn("微信回调缺少请求头信息，无法验证签名");
            return null;
        }

        if (wechatPayClientFactory.isEmpty()) {
            log.warn("微信支付客户端工厂不可用，无法验证签名");
            return false;
        }

        if (!wechatPayClientFactory.get().isActive()) {
            // 非真实接入场景（未完整配置）不应收到生产回调，允许跳过验签以便本地/测试场景运行。
            log.info("微信支付未启用真实接入（配置不完整），跳过签名验证");
            return true;
        }

        Optional<NotificationParser> parserOptional = wechatPayClientFactory
            .flatMap(WechatPayClientFactory::getNotificationParser);
        if (parserOptional.isEmpty()) {
            log.warn("微信支付通知解析器不可用，无法验证签名");
            return false;
        }

        String serial = getHeader(headers, "Wechatpay-Serial");
        String signature = getHeader(headers, "Wechatpay-Signature");
        String timestamp = getHeader(headers, "Wechatpay-Timestamp");
        String nonce = getHeader(headers, "Wechatpay-Nonce");
        String signType = getHeader(headers, "Wechatpay-Signature-Type");

        if (!StringUtils.hasText(serial) || !StringUtils.hasText(signature)
            || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
            log.warn("微信回调缺少必要签名参数 serial={}, signature={}, timestamp={}, nonce={}",
                serial, signature, timestamp, nonce);
            return false;
        }

        if (!isWechatTimestampAcceptable(timestamp)) {
            log.warn("微信回调timestamp超出允许偏差范围，拒绝处理: timestamp={}", timestamp);
            return false;
        }

        if (!StringUtils.hasText(request.getRawData())) {
            log.warn("微信回调原始数据为空，无法验证签名");
            return false;
        }

        try {
            RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serial)
                .signature(signature)
                .timestamp(timestamp)
                .nonce(nonce)
                .signType(signType)
                .body(request.getRawData())
                .build();
            NotificationParser parser = parserOptional.get();
            parser.parse(requestParam, Notification.class);
            return true;
        } catch (Exception ex) {
            log.warn("微信签名验证失败", ex);
            return false;
        }
    }

    private boolean isWechatTimestampAcceptable(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return false;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            return false;
        }

        long nowSeconds = System.currentTimeMillis() / 1000L;
        long diff = Math.abs(nowSeconds - ts);
        return diff <= WECHAT_CALLBACK_MAX_SKEW_SECONDS;
    }

    private String getHeader(Map<String, String> headers, String target) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (target.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 解析支付状态
     */
    private boolean parsePaymentStatus(String channel, String tradeStatus) {
        // 支付宝状态码
        if ("alipay".equals(channel)) {
            return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
        }

        // 微信支付状态码
        if ("wechat".equals(channel)) {
            return "SUCCESS".equals(tradeStatus);
        }

        // 默认处理
        return "SUCCESS".equals(tradeStatus);
    }

    /**
     * 更新支付订单状态
     */
    private boolean updatePaymentOrder(PaymentOrder paymentOrder, CallbackRequest request, boolean isPaymentSuccess) {
        try {
            paymentOrder.setStatusEnum(isPaymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

            if (isPaymentSuccess) {
                paymentOrder.setPaidTime(LocalDateTime.now());
                paymentOrder.setOutTradeNo(request.getOutTradeNo());
            }

            paymentOrder.setUpdateBy(TenantContext.getCurrentUserId());

            return paymentService.updatePaymentOrder(paymentOrder);
        } catch (Exception e) {
            log.error("更新支付订单状态失败: tradeNo={}", request.getTradeNo(), e);
            return false;
        }
    }

    /**
     * 同步订单状态到订单服务
     */
    private boolean syncOrderStatus(PaymentOrder paymentOrder, boolean isSuccess) {
        try {
            if (isSuccess) {
                return orderSyncService.syncPaymentSuccess(paymentOrder);
            } else {
                return orderSyncService.syncPaymentFailure(paymentOrder, "支付失败");
            }
        } catch (Exception e) {
            log.error("同步订单状态异常: paymentOrderId={}, success={}",
                    paymentOrder.getId(), isSuccess, e);
            return false;
        }
    }

    /**
     * 发送支付成功通知
     */
    private void sendPaymentSuccessNotification(PaymentOrder paymentOrder) {
        try {
            // 使用消息服务发送支付成功通知
            paymentMessageService.sendPaymentSuccessMessage(paymentOrder);

            log.info("支付成功通知已发送: orderId={}, tradeNo={}, amount={}",
                    paymentOrder.getOrderId(), paymentOrder.getTradeNo(), paymentOrder.getAmount());

        } catch (Exception e) {
            log.error("发送支付成功通知失败: orderId={}", paymentOrder.getOrderId(), e);
            // 不影响回调处理结果，只记录日志
        }
    }

    /**
     * 创建成功响应
     */
    private CallbackResponse createSuccessResponse(String channel) {
        if ("alipay".equals(channel)) {
            // 支付宝要求返回 "success"
            return CallbackResponse.success("success");
        } else if ("wechat".equals(channel)) {
            // 微信支付要求返回XML格式的成功响应
            String xmlResponse = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
            return CallbackResponse.success(xmlResponse);
        }

        return CallbackResponse.success("OK");
    }

    private PaymentMethod resolvePaymentMethod(String channel) {
        if (!org.springframework.util.StringUtils.hasText(channel)) {
            return null;
        }

        if ("alipay".equalsIgnoreCase(channel)) {
            return PaymentMethod.ALIPAY_APP;
        }
        if ("wechat".equalsIgnoreCase(channel)) {
            return PaymentMethod.WECHAT_JSAPI;
        }

        try {
            return PaymentMethod.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("无法根据渠道解析支付方式: {}", channel, ex);
            return null;
        }
    }
}
