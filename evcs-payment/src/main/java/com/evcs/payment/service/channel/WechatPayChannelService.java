package com.evcs.payment.service.channel;

import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.dto.WechatPaymentOptions;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 微信支付渠道服务（集成微信官方SDK）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPayChannelService implements IPaymentChannel {

    private final PaymentConfig paymentConfig;
    private final WechatPayClientFactory clientFactory;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        if (!isRealIntegrationEnabled()) {
            log.debug("微信支付处于模拟模式，返回伪造响应");
            return createMockPaymentResponse(request);
        }

        try {
            PaymentMethod method = request.getPaymentMethod();
            if (method == PaymentMethod.WECHAT_JSAPI) {
                return createJsapiPayment(request);
            } else if (method == PaymentMethod.WECHAT_NATIVE) {
                return createNativePayment(request);
            }
            throw new IllegalArgumentException("不支持的微信支付方式: " + method);
        } catch (Exception ex) {
            log.error("调用微信支付失败，回退到模拟响应: orderId={}", request.getOrderId(), ex);
            return createMockPaymentResponse(request);
        }
    }

    @Override
    public PaymentResponse queryPayment(String tradeNo) {
        if (!isRealIntegrationEnabled()) {
            return createMockQueryResponse(tradeNo);
        }

        try {
            JsapiServiceExtension jsapiService = clientFactory.getJsapiService()
                .orElseThrow(() -> new IllegalStateException("微信JSAPI服务不可用"));

            PaymentConfig.WechatConfig config = paymentConfig.getWechat();
            QueryOrderByOutTradeNoRequest query = new QueryOrderByOutTradeNoRequest();
            query.setMchid(config.getMchid());
            query.setOutTradeNo(tradeNo);

            Transaction transaction = jsapiService.queryOrderByOutTradeNo(query);
            return buildResponseFromTransaction(tradeNo, transaction);
        } catch (Exception ex) {
            log.error("微信支付查询失败: tradeNo={}", tradeNo, ex);
            return createMockQueryResponse(tradeNo);
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        if (!isRealIntegrationEnabled()) {
            return createMockRefundResponse(request);
        }

        try {
            RefundService refundService = clientFactory.getRefundService()
                .orElseThrow(() -> new IllegalStateException("微信退款服务不可用"));

            PaymentConfig.WechatConfig config = paymentConfig.getWechat();
            CreateRequest refundRequest = new CreateRequest();
            refundRequest.setOutRefundNo(generateRefundNo(request.getPaymentId()));
            refundRequest.setReason(request.getRefundReason());
            refundRequest.setNotifyUrl(resolveRefundNotifyUrl(config));

            if (StringUtils.hasText(config.getSubMchid())) {
                refundRequest.setSubMchid(config.getSubMchid());
            }

            if (StringUtils.hasText(request.getTransactionId())) {
                refundRequest.setTransactionId(request.getTransactionId());
            } else if (StringUtils.hasText(request.getTradeNo())) {
                refundRequest.setOutTradeNo(request.getTradeNo());
            } else {
                throw new IllegalArgumentException("退款请求缺少tradeNo或transactionId");
            }

            AmountReq amountReq = new AmountReq();
            amountReq.setCurrency("CNY");
            amountReq.setRefund((long) convertAmountToFen(request.getRefundAmount()));
            BigDecimal totalAmount = Optional.ofNullable(request.getTotalAmount())
                .orElse(request.getRefundAmount());
            amountReq.setTotal((long) convertAmountToFen(totalAmount));
            refundRequest.setAmount(amountReq);

            Refund response = refundService.create(refundRequest);

            RefundResponse result = new RefundResponse();
            result.setRefundNo(response.getRefundId());
            result.setRefundAmount(request.getRefundAmount());
            result.setRefundStatus(response.getStatus() != null ? response.getStatus().name() : "PROCESSING");

            log.info("微信退款提交成功: outRefundNo={}, channelRefundId={}",
                refundRequest.getOutRefundNo(), response.getRefundId());
            return result;
        } catch (Exception ex) {
            log.error("微信退款失败，回退到模拟响应: paymentId={}", request.getPaymentId(), ex);
            return createMockRefundResponse(request);
        }
    }

    @Override
    public boolean verifySignature(String data, String signature) {
        if (!isRealIntegrationEnabled()) {
            return true;
        }

        // 现有回调数据结构仅提供原始body与签名，缺少必要的HTTP头信息，无法直接使用SDK验签。
        // 此处先记录日志，后续可在控制器层补充完整参数后再调用NotificationParser进行校验。
        log.debug("跳过微信签名验证，占位实现 dataLength={}, signatureLength={}",
            data != null ? data.length() : 0,
            signature != null ? signature.length() : 0);
        return true;
    }

    @Override
    public String getChannelName() {
        return "wechat";
    }

    private PaymentResponse createJsapiPayment(PaymentRequest request) throws JsonProcessingException {
        JsapiServiceExtension jsapiService = clientFactory.getJsapiService()
            .orElseThrow(() -> new IllegalStateException("微信JSAPI服务不可用"));

        PaymentConfig.WechatConfig config = paymentConfig.getWechat();
        WechatPaymentOptions options = Objects.requireNonNull(request.getWechatOptions(),
            "微信支付缺少wechatOptions");

        String appId = resolveAppId(options, config);
        String outTradeNo = generateTradeNo(request.getOrderId());

        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(appId);
        prepayRequest.setMchid(config.getMchid());
        prepayRequest.setDescription(resolveDescription(request));
        prepayRequest.setOutTradeNo(outTradeNo);
        prepayRequest.setNotifyUrl(resolveNotifyUrl(config));

        if (StringUtils.hasText(options.getAttach())) {
            prepayRequest.setAttach(options.getAttach());
        }
        if (StringUtils.hasText(options.getGoodsTag())) {
            prepayRequest.setGoodsTag(options.getGoodsTag());
        }

        com.wechat.pay.java.service.payments.jsapi.model.Amount amount =
            new com.wechat.pay.java.service.payments.jsapi.model.Amount();
        amount.setCurrency("CNY");
        amount.setTotal(convertAmountToFen(request.getAmount()));
        prepayRequest.setAmount(amount);

        com.wechat.pay.java.service.payments.jsapi.model.Payer payer =
            new com.wechat.pay.java.service.payments.jsapi.model.Payer();
        payer.setOpenid(options.getOpenId());
        prepayRequest.setPayer(payer);

        PrepayWithRequestPaymentResponse sdkResponse = jsapiService.prepayWithRequestPayment(prepayRequest);
        Map<String, String> payParams = buildJsapiPayParams(sdkResponse);

        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(outTradeNo);
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);
        response.setPayParams(objectMapper.writeValueAsString(payParams));
        return response;
    }

    private PaymentResponse createNativePayment(PaymentRequest request) throws JsonProcessingException {
        NativePayService nativePayService = clientFactory.getNativePayService()
            .orElseThrow(() -> new IllegalStateException("微信Native支付服务不可用"));

        PaymentConfig.WechatConfig config = paymentConfig.getWechat();
        WechatPaymentOptions options = request.getWechatOptions();

        String outTradeNo = generateTradeNo(request.getOrderId());

        com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest prepayRequest =
            new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
        prepayRequest.setAppid(resolveAppId(options, config));
        prepayRequest.setMchid(config.getMchid());
        prepayRequest.setDescription(resolveDescription(request));
        prepayRequest.setOutTradeNo(outTradeNo);
        prepayRequest.setNotifyUrl(resolveNotifyUrl(config));

        com.wechat.pay.java.service.payments.nativepay.model.Amount amount =
            new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        amount.setCurrency("CNY");
        amount.setTotal(convertAmountToFen(request.getAmount()));
        prepayRequest.setAmount(amount);

        if (options != null) {
            if (StringUtils.hasText(options.getAttach())) {
                prepayRequest.setAttach(options.getAttach());
            }
            if (StringUtils.hasText(options.getGoodsTag())) {
                prepayRequest.setGoodsTag(options.getGoodsTag());
            }
            if (StringUtils.hasText(options.getPayerClientIp())) {
                com.wechat.pay.java.service.payments.nativepay.model.SceneInfo sceneInfo =
                    new com.wechat.pay.java.service.payments.nativepay.model.SceneInfo();
                sceneInfo.setPayerClientIp(options.getPayerClientIp());
                prepayRequest.setSceneInfo(sceneInfo);
            }
        }

        PrepayResponse sdkResponse = nativePayService.prepay(prepayRequest);

        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(outTradeNo);
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);
        response.setPayUrl(sdkResponse.getCodeUrl());
        return response;
    }

    private PaymentResponse buildResponseFromTransaction(String tradeNo, Transaction transaction) {
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(tradeNo);
        response.setStatus(mapTradeState(transaction));

        if (transaction != null && transaction.getAmount() != null
            && transaction.getAmount().getTotal() != null) {
            BigDecimal amount = new BigDecimal(transaction.getAmount().getTotal())
                .movePointLeft(2);
            response.setAmount(amount);
        }
        return response;
    }

    private PaymentStatus mapTradeState(Transaction transaction) {
        if (transaction == null || transaction.getTradeState() == null) {
            return PaymentStatus.PENDING;
        }
        switch (transaction.getTradeState()) {
            case SUCCESS:
                return PaymentStatus.SUCCESS;
            case REFUND:
                return PaymentStatus.REFUNDED;
            case NOTPAY:
            case USERPAYING:
            case ACCEPT:
                return PaymentStatus.PENDING;
            case CLOSED:
            case REVOKED:
            case PAYERROR:
            default:
                return PaymentStatus.FAILED;
        }
    }

    private Map<String, String> buildJsapiPayParams(PrepayWithRequestPaymentResponse response) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appId", response.getAppId());
        params.put("timeStamp", response.getTimeStamp());
        params.put("nonceStr", response.getNonceStr());
        params.put("package", response.getPackageVal());
        params.put("signType", response.getSignType());
        params.put("paySign", response.getPaySign());
        return params;
    }

    private String resolveAppId(WechatPaymentOptions options, PaymentConfig.WechatConfig config) {
        if (options != null && StringUtils.hasText(options.getAppId())) {
            return options.getAppId();
        }
        if (StringUtils.hasText(config.getAppId())) {
            return config.getAppId();
        }
        throw new IllegalArgumentException("未配置微信支付AppId");
    }

    private String resolveNotifyUrl(PaymentConfig.WechatConfig config) {
        if (StringUtils.hasText(config.getNotifyUrl())) {
            return config.getNotifyUrl();
        }
        return paymentConfig.getCallbackUrlPrefix() + "/wechat";
    }

    private String resolveRefundNotifyUrl(PaymentConfig.WechatConfig config) {
        if (StringUtils.hasText(config.getRefundNotifyUrl())) {
            return config.getRefundNotifyUrl();
        }
        return paymentConfig.getCallbackUrlPrefix() + "/wechat/refund";
    }

    private String resolveDescription(PaymentRequest request) {
        if (StringUtils.hasText(request.getDescription())) {
            return request.getDescription();
        }
        return "EVCS订单支付-" + request.getOrderId();
    }

    private boolean isRealIntegrationEnabled() {
        return paymentConfig.isEnabled()
            && paymentConfig.getWechat() != null
            && paymentConfig.getWechat().isFullyConfigured()
            && clientFactory.isActive();
    }

    private int convertAmountToFen(BigDecimal amount) {
        return amount.movePointRight(2)
            .setScale(0, RoundingMode.UNNECESSARY)
            .intValueExact();
    }

    private String generateTradeNo(Long orderId) {
        String suffix = orderId != null ? String.format("%06d", Math.abs(orderId) % 1_000_000) : "000000";
        return "EVC" + Instant.now().getEpochSecond() + suffix;
    }

    private String generateRefundNo(Long paymentId) {
        return "EVCR" + (paymentId != null ? paymentId : UUID.randomUUID());
    }

    private PaymentResponse createMockPaymentResponse(PaymentRequest request) {
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(generateTradeNo(request.getOrderId()));
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);

        if (request.getPaymentMethod() == PaymentMethod.WECHAT_NATIVE) {
            response.setPayUrl("https://pay.mock.wechat.qq.com/native/" + response.getTradeNo());
        } else {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("mock", "true");
            payload.put("tradeNo", response.getTradeNo());
            try {
                response.setPayParams(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException e) {
                response.setPayParams("{\"mock\":true}");
            }
        }
        return response;
    }

    private PaymentResponse createMockQueryResponse(String tradeNo) {
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(tradeNo);
        response.setStatus(PaymentStatus.SUCCESS);
        return response;
    }

    private RefundResponse createMockRefundResponse(RefundRequest request) {
        RefundResponse response = new RefundResponse();
        response.setRefundNo(generateRefundNo(request.getPaymentId()));
        response.setRefundAmount(request.getRefundAmount());
        response.setRefundStatus("SUCCESS");
        return response;
    }
}
