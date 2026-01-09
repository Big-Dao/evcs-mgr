package com.evcs.payment.service.channel;

import com.evcs.common.exception.BusinessException;
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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 微信支付渠道服务（集成微信官方SDK）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPayChannelService implements IPaymentChannel {

    private static final String CURRENCY_CNY = "CNY";
    private static final String REFUND_STATUS_PROCESSING = "PROCESSING";
    private static final String REFUND_STATUS_SUCCESS = "SUCCESS";
    private static final String REFUND_STATUS_FAILED = "FAILED";

    private final PaymentConfig paymentConfig;
    private final WechatPayClientFactory clientFactory;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Qualifier("wechatPayRetry")
    private final Retry wechatPayRetry;

    @Qualifier("wechatPayCircuitBreaker")
    private final CircuitBreaker wechatPayCircuitBreaker;

    private boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of("prod", "production"));
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("创建微信支付订单: orderId={}, amount={}, method={}", request.getOrderId(), request.getAmount(),
                request.getPaymentMethod());

        if (!isRealIntegrationEnabled()) {
            log.warn("微信支付未完全配置或非生产环境，返回模拟响应");
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
            log.error("调用微信支付失败: orderId={}", request.getOrderId(), ex);
            if (isProduction()) {
                throw new BusinessException("创建微信支付订单失败: " + ex.getMessage());
            }
            return createMockPaymentResponse(request);
        }
    }

    @Override
    public PaymentResponse queryPayment(String tradeNo) {
        log.info("查询微信支付状态: tradeNo={}", tradeNo);

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

            // Query is safe to retry on transient failures.
            Transaction transaction = executeWithWechatResilience(
                "queryPayment",
                () -> jsapiService.queryOrderByOutTradeNo(query)
            );
            return buildResponseFromTransaction(tradeNo, transaction);
        } catch (Exception ex) {
            log.error("微信支付查询失败: tradeNo={}", tradeNo, ex);
            if (isProduction()) {
                throw new BusinessException("查询微信支付状态失败: " + ex.getMessage());
            }
            return createMockQueryResponse(tradeNo);
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("发起微信退款: paymentId={}, amount={}", request.getPaymentId(), request.getRefundAmount());

        if (!isRealIntegrationEnabled()) {
            return createMockRefundResponse(request);
        }

        try {
            RefundService refundService = clientFactory.getRefundService()
                .orElseThrow(() -> new IllegalStateException("微信退款服务不可用"));

            PaymentConfig.WechatConfig config = paymentConfig.getWechat();
            CreateRequest refundRequest = new CreateRequest();
            String outRefundNo = StringUtils.hasText(request.getRefundRequestNo())
                ? request.getRefundRequestNo()
                : generateRefundNo(request.getPaymentId());
            refundRequest.setOutRefundNo(outRefundNo);
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
            amountReq.setCurrency(CURRENCY_CNY);
            amountReq.setRefund((long) convertAmountToFen(request.getRefundAmount()));
            BigDecimal totalAmount = Optional.ofNullable(request.getTotalAmount())
                .orElse(request.getRefundAmount());
            amountReq.setTotal((long) convertAmountToFen(totalAmount));
            refundRequest.setAmount(amountReq);

            // Refund is idempotent with outRefundNo; safe to retry on transient failures.
            Refund response = executeWithWechatResilience(
                "refund",
                () -> refundService.create(refundRequest)
            );

            RefundResponse result = new RefundResponse();
            result.setRefundNo(response.getRefundId());
            result.setRefundAmount(request.getRefundAmount());
            result.setRefundStatus(response.getStatus() != null ? response.getStatus().name() : REFUND_STATUS_PROCESSING);

            log.info("微信退款提交成功: outRefundNo={}, channelRefundId={}",
                refundRequest.getOutRefundNo(), response.getRefundId());
            return result;
        } catch (Exception ex) {
            log.error("微信退款失败: paymentId={}", request.getPaymentId(), ex);
            if (isProduction()) {
                throw new BusinessException("微信退款失败: " + ex.getMessage());
            }
            return createMockRefundResponse(request);
        }
    }

    @Override
    public RefundResponse queryRefund(String refundRequestNo) {
        if (!StringUtils.hasText(refundRequestNo)) {
            throw new IllegalArgumentException("refundRequestNo不能为空");
        }

        if (!isRealIntegrationEnabled()) {
            RefundResponse resp = new RefundResponse();
            resp.setRefundNo(refundRequestNo);
            resp.setRefundStatus(REFUND_STATUS_SUCCESS);
            return resp;
        }

        try {
            RefundService refundService = clientFactory.getRefundService()
                .orElseThrow(() -> new IllegalStateException("微信退款服务不可用"));

            PaymentConfig.WechatConfig config = paymentConfig.getWechat();
            com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest query =
                new com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest();
            query.setOutRefundNo(refundRequestNo);
            if (StringUtils.hasText(config.getSubMchid())) {
                query.setSubMchid(config.getSubMchid());
            }

            Refund refund = executeWithWechatResilience(
                "queryRefund",
                () -> refundService.queryByOutRefundNo(query)
            );

            RefundResponse resp = new RefundResponse();
            resp.setRefundNo(refund != null ? refund.getRefundId() : refundRequestNo);
            resp.setRefundStatus(refund != null && refund.getStatus() != null
                ? refund.getStatus().name()
                : REFUND_STATUS_FAILED);

            if (refund != null && refund.getAmount() != null && refund.getAmount().getRefund() != null) {
                BigDecimal amount = new BigDecimal(refund.getAmount().getRefund()).movePointLeft(2);
                resp.setRefundAmount(amount);
            }
            return resp;
        } catch (Exception ex) {
            log.error("查询微信退款状态失败: refundRequestNo={}", refundRequestNo, ex);
            if (isProduction()) {
                throw new BusinessException("查询微信退款状态失败: " + ex.getMessage());
            }
            RefundResponse fallback = new RefundResponse();
            fallback.setRefundNo(refundRequestNo);
            fallback.setRefundStatus(REFUND_STATUS_PROCESSING);
            return fallback;
        }
    }

    private <T> T executeWithWechatResilience(String operation, Supplier<T> supplier) {
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(wechatPayCircuitBreaker, supplier);
        decorated = Retry.decorateSupplier(wechatPayRetry, decorated);

        try {
            return decorated.get();
        } catch (RuntimeException ex) {
            log.warn("微信支付调用失败（已应用重试/熔断）: op={}", operation, ex);
            throw ex;
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
        String outTradeNo = StringUtils.hasText(request.getTradeNo())
            ? request.getTradeNo()
            : generateTradeNo(request.getOrderId());

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
        amount.setCurrency(CURRENCY_CNY);
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

        String outTradeNo = StringUtils.hasText(request.getTradeNo())
            ? request.getTradeNo()
            : generateTradeNo(request.getOrderId());

        com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest prepayRequest =
            new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
        prepayRequest.setAppid(resolveAppId(options, config));
        prepayRequest.setMchid(config.getMchid());
        prepayRequest.setDescription(resolveDescription(request));
        prepayRequest.setOutTradeNo(outTradeNo);
        prepayRequest.setNotifyUrl(resolveNotifyUrl(config));

        com.wechat.pay.java.service.payments.nativepay.model.Amount amount =
            new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        amount.setCurrency(CURRENCY_CNY);
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
                return PaymentStatus.CLOSED;
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
            .setScale(0, RoundingMode.HALF_UP)
            .intValueExact();
    }

    private String generateTradeNo(Long orderId) {
        // 业务前缀 + 订单ID + 随机后缀，降低碰撞概率
        return "WXP" + orderId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateRefundNo(Long paymentId) {
        return "WXPR" + (paymentId != null ? paymentId : UUID.randomUUID().toString().substring(0, 8));
    }

    private PaymentResponse createMockPaymentResponse(PaymentRequest request) {
        log.warn("非生产环境：回退到模拟实现");
        PaymentResponse response = new PaymentResponse();
        String tradeNo = request.getTradeNo();
        if (tradeNo == null || tradeNo.isEmpty()) {
            tradeNo = generateTradeNo(request.getOrderId());
        }
        response.setTradeNo(tradeNo);
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
        log.warn("非生产环境：回退到模拟实现");
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(tradeNo);
        response.setStatus(PaymentStatus.SUCCESS);
        return response;
    }

    private RefundResponse createMockRefundResponse(RefundRequest request) {
        log.warn("非生产环境：回退到模拟实现");
        RefundResponse response = new RefundResponse();
        response.setRefundNo(generateRefundNo(request.getPaymentId()));
        response.setRefundAmount(request.getRefundAmount());
        response.setRefundStatus(REFUND_STATUS_SUCCESS);
        return response;
    }
}
