package com.evcs.payment.service.channel;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeAppPayModel;
import com.alipay.api.domain.AlipayTradeCreateModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeCreateRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeCreateResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.evcs.common.exception.BusinessException;
import com.evcs.payment.config.AlipayConfig;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.enums.PaymentMethod;
import com.evcs.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * 支付宝支付渠道服务（集成真实SDK）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayChannelService implements IPaymentChannel {

    private static final String PRODUCT_CODE_APP = "QUICK_MSECURITY_PAY";
    private static final String TRADE_STATUS_WAIT = "WAIT_BUYER_PAY";
    private static final String TRADE_STATUS_SUCCESS = "TRADE_SUCCESS";
    private static final String TRADE_STATUS_FINISHED = "TRADE_FINISHED";
    private static final String TRADE_STATUS_CLOSED = "TRADE_CLOSED";

    private static final String REFUND_REQUEST_NO_PREFIX = "ALIRF";

    private final AlipayClientFactory alipayClientFactory;

    private final AlipayConfig alipayConfig;

    private final Environment environment;

    private boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of("prod", "production"));
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("创建支付宝支付订单: orderId={}, amount={}, method={}", request.getOrderId(), request.getAmount(),
                request.getPaymentMethod());

        PaymentResponse response = new PaymentResponse();
        String tradeNo = request.getTradeNo();
        if (tradeNo == null || tradeNo.isEmpty()) {
            tradeNo = generateTradeNo(request.getOrderId());
        }
        response.setTradeNo(tradeNo);
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);

        try {
            AlipayClient alipayClient = alipayClientFactory.getAlipayClient();

            switch (request.getPaymentMethod()) {
            case ALIPAY_APP:
                response = createAppPay(alipayClient, request, tradeNo);
                break;
            case ALIPAY_QR:
                response = createQrPay(alipayClient, request, tradeNo);
                break;
            default:
                throw new IllegalArgumentException("不支持的支付宝支付方式: " + request.getPaymentMethod());
            }

            log.info("支付宝支付订单创建成功: tradeNo={}, method={}", tradeNo, request.getPaymentMethod());

        } catch (AlipayApiException e) {
            log.error("支付宝API调用失败: {}", e.getMessage(), e);
            if (isProduction()) {
                throw new BusinessException("支付宝支付调用失败: " + e.getErrMsg());
            }
            return handleMockPayment(request, tradeNo);
        } catch (Exception e) {
            log.error("创建支付宝支付订单失败", e);
            throw new BusinessException("创建支付宝支付订单失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }

        log.info("支付宝支付订单创建成功: tradeNo={}", response.getTradeNo());
        return response;
    }

    @Override
    public PaymentResponse queryPayment(String tradeNo) {
        log.info("查询支付宝支付状态: tradeNo={}", tradeNo);

        try {
            AlipayClient alipayClient = alipayClientFactory.getAlipayClient();
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(tradeNo);
            request.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                PaymentResponse result = new PaymentResponse();
                result.setTradeNo(tradeNo);

                // 根据支付宝返回状态转换
                String tradeStatus = response.getTradeStatus();
                if (TRADE_STATUS_WAIT.equals(tradeStatus)) {
                    result.setStatus(PaymentStatus.PENDING);
                } else if (TRADE_STATUS_SUCCESS.equals(tradeStatus) || TRADE_STATUS_FINISHED.equals(tradeStatus)) {
                    result.setStatus(PaymentStatus.SUCCESS);
                    result.setAmount(new BigDecimal(response.getTotalAmount()));
                } else if (TRADE_STATUS_CLOSED.equals(tradeStatus)) {
                    result.setStatus(PaymentStatus.CLOSED);
                } else {
                    result.setStatus(PaymentStatus.FAILED);
                }

                log.info("支付宝支付状态查询成功: tradeNo={}, status={}", tradeNo, result.getStatus());
                return result;
            } else {
                log.warn("支付宝支付状态查询失败: tradeNo={}, error={}", tradeNo, response.getSubMsg());
                if (isProduction()) {
                    PaymentResponse errorResult = new PaymentResponse();
                    errorResult.setTradeNo(tradeNo);
                    errorResult.setStatus(PaymentStatus.FAILED);
                    return errorResult;
                }
                return handleMockQuery(tradeNo);
            }

        } catch (AlipayApiException e) {
            log.error("支付宝查询API调用失败: {}", e.getMessage(), e);
            return handleMockQuery(tradeNo);
        } catch (Exception e) {
            log.error("查询支付宝支付状态失败", e);
            throw new BusinessException("查询支付宝支付状态失败: " + e.getMessage());
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("发起支付宝退款: paymentId={}, amount={}", request.getPaymentId(), request.getRefundAmount());

        try {
            AlipayClient alipayClient = alipayClientFactory.getAlipayClient();
            AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();

            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            String outTradeNo = request.getTradeNo();
            if (outTradeNo == null || outTradeNo.isBlank()) {
                if (request.getPaymentId() != null) {
                    outTradeNo = request.getPaymentId().toString();
                    log.warn("支付宝退款缺少tradeNo，回退使用paymentId作为outTradeNo: paymentId={}", request.getPaymentId());
                } else {
                    throw new IllegalArgumentException("支付宝退款请求缺少tradeNo/paymentId");
                }
            }
            model.setOutTradeNo(outTradeNo);
            model.setRefundAmount(formatAmount(request.getRefundAmount()));
            model.setRefundReason(request.getRefundReason());
            String outRequestNo = request.getRefundRequestNo();
            if (outRequestNo == null || outRequestNo.isBlank()) {
                outRequestNo = UUID.randomUUID().toString().replace("-", "");
            }
            model.setOutRequestNo(outRequestNo);
            alipayRequest.setBizModel(model);

            AlipayTradeRefundResponse response = alipayClient.execute(alipayRequest);

            if (response.isSuccess()) {
                RefundResponse result = new RefundResponse();
                result.setRefundNo(model.getOutRequestNo());
                result.setRefundAmount(request.getRefundAmount());
                result.setRefundStatus("SUCCESS");

                log.info("支付宝退款成功: refundNo={}, amount={}", result.getRefundNo(), result.getRefundAmount());
                return result;
            } else {
                log.warn("支付宝退款失败: error={}", response.getSubMsg());
                if (isProduction()) {
                    throw new BusinessException("支付宝退款失败: " + response.getSubMsg());
                }
                return handleMockRefund(request);
            }

        } catch (AlipayApiException e) {
            log.error("支付宝退款API调用失败: {}", e.getMessage(), e);
            // 退款在异常/超时场景下可能进入未知状态：不应误判为成功。
            // 返回PROCESSING以便上层落库为REFUNDING，并由后台轮询(queryRefund)做最终态收敛。
            RefundResponse fallbackResponse = new RefundResponse();
            fallbackResponse.setRefundNo(request.getRefundRequestNo());
            fallbackResponse.setRefundAmount(request.getRefundAmount());
            fallbackResponse.setRefundStatus("PROCESSING");
            return fallbackResponse;
        } catch (Exception e) {
            log.error("发起支付宝退款失败", e);
            throw new BusinessException("发起支付宝退款失败: " + e.getMessage());
        }
    }

    @Override
    public RefundResponse queryRefund(String refundRequestNo, String tradeNo) {
        if (refundRequestNo == null || refundRequestNo.isBlank()) {
            throw new IllegalArgumentException("refundRequestNo不能为空");
        }

        String outTradeNo = tradeNo;
        if (outTradeNo == null || outTradeNo.isBlank()) {
            outTradeNo = resolveOutTradeNoFromRefundRequestNo(refundRequestNo);
        }
        log.info("查询支付宝退款状态: refundRequestNo={}, outTradeNo={}", refundRequestNo, outTradeNo);

        try {
            AlipayClient alipayClient = alipayClientFactory.getAlipayClient();
            AlipayTradeFastpayRefundQueryRequest alipayRequest = new AlipayTradeFastpayRefundQueryRequest();

            AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
            model.setOutTradeNo(outTradeNo);
            model.setOutRequestNo(refundRequestNo);
            alipayRequest.setBizModel(model);

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(alipayRequest);

            if (response != null && response.isSuccess()) {
                RefundResponse result = new RefundResponse();
                result.setRefundNo(refundRequestNo);
                result.setRefundStatus("SUCCESS");

                String refundAmount = response.getRefundAmount();
                if (refundAmount != null && !refundAmount.isBlank()) {
                    result.setRefundAmount(new BigDecimal(refundAmount));
                }

                log.info("支付宝退款状态查询成功: refundRequestNo={}, refundAmount={}",
                    refundRequestNo,
                    result.getRefundAmount());
                return result;
            }

            String subCode = response != null ? response.getSubCode() : null;
            String subMsg = response != null ? response.getSubMsg() : null;
            log.warn("支付宝退款状态查询失败: refundRequestNo={}, subCode={}, subMsg={}", refundRequestNo, subCode, subMsg);

            if (looksLikeRefundNotReady(subCode)) {
                RefundResponse processing = new RefundResponse();
                processing.setRefundNo(refundRequestNo);
                processing.setRefundStatus("PROCESSING");
                return processing;
            }

            if (isProduction()) {
                throw new BusinessException("支付宝退款状态查询失败: " + (subMsg != null ? subMsg : "未知错误"));
            }

            RefundResponse fallback = new RefundResponse();
            fallback.setRefundNo(refundRequestNo);
            fallback.setRefundStatus("PROCESSING");
            return fallback;

        } catch (AlipayApiException e) {
            log.error("支付宝退款查询API调用失败: {}", e.getMessage(), e);
            if (isProduction()) {
                throw new BusinessException("支付宝退款查询调用失败: " + e.getErrMsg());
            }

            RefundResponse fallback = new RefundResponse();
            fallback.setRefundNo(refundRequestNo);
            fallback.setRefundStatus("PROCESSING");
            return fallback;
        } catch (Exception e) {
            log.error("查询支付宝退款状态失败: refundRequestNo={}", refundRequestNo, e);
            throw new BusinessException("查询支付宝退款状态失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    @Override
    public boolean verifySignature(String data, String signature) {
        log.info("验证支付宝签名: data={}, signature={}", data, signature);
        try {
            // 使用支付宝公钥验证签名
            // data应该是待签名的内容（通常是排序后的参数字符串）
            return AlipaySignature.rsaCheckContent(data, signature, alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset());
        } catch (AlipayApiException e) {
            log.error("支付宝签名验证失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getChannelName() {
        return "alipay";
    }

    // ========== 私有辅助方法 ==========

    /**
     * 生成支付宝交易号
     */
    private String generateTradeNo(Long orderId) {
        // 业务前缀 + 订单ID + 随机后缀，降低碰撞概率
        return "ALI" + orderId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 创建APP支付
     */
    private PaymentResponse createAppPay(AlipayClient alipayClient, PaymentRequest request, String tradeNo)
            throws AlipayApiException {
        AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();
        alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

        AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
        model.setSubject(request.getDescription());
        model.setOutTradeNo(tradeNo);
        model.setTotalAmount(formatAmount(request.getAmount()));
        model.setProductCode(PRODUCT_CODE_APP);
        alipayRequest.setBizModel(model);

        AlipayTradeAppPayResponse response = alipayClient.sdkExecute(alipayRequest);

        PaymentResponse result = new PaymentResponse();
        result.setTradeNo(tradeNo);
        result.setAmount(request.getAmount());
        result.setStatus(PaymentStatus.PENDING);
        result.setPayParams(response.getBody());

        return result;
    }

    /**
     * 创建扫码支付
     */
    private PaymentResponse createQrPay(AlipayClient alipayClient, PaymentRequest request, String tradeNo)
            throws AlipayApiException {
        AlipayTradePrecreateRequest alipayRequest = new AlipayTradePrecreateRequest();
        alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setSubject(request.getDescription());
        model.setOutTradeNo(tradeNo);
        model.setTotalAmount(formatAmount(request.getAmount()));
        alipayRequest.setBizModel(model);

        AlipayTradePrecreateResponse response = alipayClient.execute(alipayRequest);

        if (response.isSuccess()) {
            PaymentResponse result = new PaymentResponse();
            result.setTradeNo(tradeNo);
            result.setAmount(request.getAmount());
            result.setStatus(PaymentStatus.PENDING);
            result.setPayUrl(response.getQrCode());

            return result;
        } else {
            throw new AlipayApiException("支付宝扫码支付失败: " + response.getSubMsg());
        }
    }

    /**
     * 格式化金额为支付宝要求的格式（两位小数）
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String resolveOutTradeNoFromRefundRequestNo(String refundRequestNo) {
        if (refundRequestNo == null || refundRequestNo.isBlank()) {
            throw new IllegalArgumentException("refundRequestNo不能为空");
        }

        if (!refundRequestNo.startsWith(REFUND_REQUEST_NO_PREFIX)) {
            throw new IllegalArgumentException("无法从refundRequestNo解析outTradeNo: " + refundRequestNo);
        }

        String remainder = refundRequestNo.substring(REFUND_REQUEST_NO_PREFIX.length());
        int idx = remainder.indexOf('_');
        if (idx <= 0) {
            throw new IllegalArgumentException("无法从refundRequestNo解析outTradeNo: " + refundRequestNo);
        }

        String paymentIdPart = remainder.substring(0, idx);
        if (!paymentIdPart.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("无法从refundRequestNo解析outTradeNo: " + refundRequestNo);
        }
        return paymentIdPart;
    }

    private boolean looksLikeRefundNotReady(String subCode) {
        if (subCode == null || subCode.isBlank()) {
            return false;
        }
        String normalized = subCode.toUpperCase();
        return normalized.contains("REFUND") && normalized.contains("NOT") && normalized.contains("EXIST");
    }

    // ========== Mock 降级逻辑 ==========

    private PaymentResponse handleMockPayment(PaymentRequest request, String tradeNo) {
        log.warn("非生产环境：回退到模拟实现");
        PaymentResponse response = new PaymentResponse();
        response.setTradeNo(tradeNo);
        response.setAmount(request.getAmount());
        response.setStatus(PaymentStatus.PENDING);

        switch (request.getPaymentMethod()) {
            case ALIPAY_APP:
                response.setPayParams("alipay_app_params_mock_" + tradeNo);
                break;
            case ALIPAY_QR:
                response.setPayUrl("https://qr.alipay.com/mock/" + tradeNo);
                break;
            default:
                throw new IllegalArgumentException("不支持的支付方式: " + request.getPaymentMethod());
        }
        return response;
    }

    private PaymentResponse handleMockQuery(String tradeNo) {
        log.warn("非生产环境：回退到模拟实现");
        PaymentResponse fallbackResponse = new PaymentResponse();
        fallbackResponse.setTradeNo(tradeNo);
        fallbackResponse.setStatus(PaymentStatus.SUCCESS);
        return fallbackResponse;
    }

    private RefundResponse handleMockRefund(RefundRequest request) {
        log.warn("非生产环境：回退到模拟实现");
        RefundResponse fallbackResponse = new RefundResponse();
        fallbackResponse.setRefundNo(UUID.randomUUID().toString());
        fallbackResponse.setRefundAmount(request.getRefundAmount());
        fallbackResponse.setRefundStatus("SUCCESS");
        return fallbackResponse;
    }
}
