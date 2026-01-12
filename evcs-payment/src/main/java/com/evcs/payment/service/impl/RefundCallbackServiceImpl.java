package com.evcs.payment.service.impl;

import com.evcs.payment.config.AlipayConfig;
import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.dto.RefundCallbackRequest;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.service.IRefundCallbackService;
import com.evcs.payment.service.channel.IPaymentChannel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;

/**
 * 退款回调服务实现
 */
@Slf4j
@Service
public class RefundCallbackServiceImpl implements IRefundCallbackService {

    @Resource
    private AlipayConfig alipayConfig;

    @Resource
    private PaymentOrderMapper paymentOrderMapper;

    @Resource
    private Map<String, IPaymentChannel> paymentChannelMap;

    @Resource
    private PaymentConfig paymentConfig;

    @Override
    @Transactional
    public boolean handleRefundCallback(RefundCallbackRequest callbackRequest) {
        log.info("处理退款回调: channel={}, outTradeNo={}, outRequestNo={}, refundStatus={}",
            callbackRequest.getChannel(), callbackRequest.getOutTradeNo(),
            callbackRequest.getOutRequestNo(), callbackRequest.getRefundStatus());

        try {
            // 1. 验证签名
            if (!verifyRefundCallbackSignature(callbackRequest)) {
                log.error("退款回调签名验证失败: channel={}, outTradeNo={}",
                    callbackRequest.getChannel(), callbackRequest.getOutTradeNo());
                return false;
            }

            // 2. 查找支付订单
            QueryWrapper<PaymentOrder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("trade_no", callbackRequest.getOutTradeNo())
                       .eq("deleted", 0);
            PaymentOrder order = paymentOrderMapper.selectOne(queryWrapper);

            if (order == null) {
                log.warn("退款回调对应的订单不存在: outTradeNo={}", callbackRequest.getOutTradeNo());
                return false;
            }

            // 3. 检查订单状态
            PaymentStatus currentStatus = order.getStatusEnum();
            if (!(PaymentStatus.SUCCESS.equals(currentStatus)
                || PaymentStatus.PARTIALLY_REFUNDED.equals(currentStatus)
                || PaymentStatus.REFUNDING.equals(currentStatus)
                || PaymentStatus.REFUNDED.equals(currentStatus))) {
                log.warn("退款回调对应订单状态不正确: orderId={}, status={}",
                    order.getId(), order.getStatus());
                return false;
            }

            // 4. 处理退款状态
            boolean refundSuccess = processRefundStatus(callbackRequest, order);

            log.info("退款回调处理完成: channel={}, outTradeNo={}, success={}",
                callbackRequest.getChannel(), callbackRequest.getOutTradeNo(), refundSuccess);

            return refundSuccess;

        } catch (Exception e) {
            log.error("处理退款回调失败: channel={}, outTradeNo={}",
                callbackRequest.getChannel(), callbackRequest.getOutTradeNo(), e);
            return false;
        }
    }

    @Override
    public boolean verifyRefundCallbackSignature(RefundCallbackRequest callbackRequest) {
        try {
            switch (callbackRequest.getChannel()) {
                case "alipay":
                    return verifyAlipayRefundSignature(callbackRequest);
                case "wechat":
                    return verifyWechatRefundSignature(callbackRequest);
                default:
                    log.warn("不支持的渠道: {}", callbackRequest.getChannel());
                    return false;
            }
        } catch (Exception e) {
            log.error("验证退款回调签名失败: channel={}", callbackRequest.getChannel(), e);
            return false;
        }
    }

    @Override
    public RefundCallbackRequest parseAlipayRefundCallback(Map<String, String> params) {
        RefundCallbackRequest request = new RefundCallbackRequest();
        request.setChannel("alipay");
        request.setOutTradeNo(params.get("out_trade_no"));
        request.setOutRequestNo(params.get("out_request_no"));
        request.setTradeNo(params.get("trade_no"));
        request.setRefundFee(new BigDecimal(params.get("refund_fee")));
        request.setRefundStatus(params.get("refund_status"));
        request.setReason(params.get("refund_reason"));
        request.setGmtRefundPay(params.get("gmt_refund_pay"));
        request.setRawParams(params);
        request.setSign(params.get("sign"));
        request.setSignType(params.get("sign_type"));

        return request;
    }

    @Override
    public RefundCallbackRequest parseWechatRefundCallback(String xmlData) {
        if (!StringUtils.hasText(xmlData)) {
            log.warn("微信退款回调XML为空");
            return null;
        }

        Map<String, String> rootParams = parseXmlToMap(xmlData);
        if (rootParams.isEmpty()) {
            log.warn("微信退款回调解析后参数为空");
            return null;
        }

        RefundCallbackRequest request = new RefundCallbackRequest();
        request.setChannel("wechat");
        request.setRawParams(rootParams);
        request.setSign(rootParams.get("sign"));
        request.setSignType(rootParams.get("sign_type"));

        String returnCode = rootParams.get("return_code");
        if (!"SUCCESS".equalsIgnoreCase(returnCode)) {
            log.warn("微信退款回调返回码非SUCCESS: {}", returnCode);
            request.setRefundStatus("REFUND_FAILED");
            return request;
        }

        Map<String, String> decryptedParams = Collections.emptyMap();
        String reqInfo = rootParams.get("req_info");
        if (StringUtils.hasText(reqInfo)) {
            decryptedParams = decryptWechatReqInfo(reqInfo);
        } else {
            decryptedParams = rootParams;
        }

        if (decryptedParams.isEmpty()) {
            log.warn("微信退款回调解密信息为空");
            return null;
        }

        request.setOutTradeNo(decryptedParams.getOrDefault("out_trade_no", rootParams.get("out_trade_no")));
        request.setOutRequestNo(decryptedParams.getOrDefault("out_refund_no", rootParams.get("out_refund_no")));
        request.setTradeNo(decryptedParams.getOrDefault("transaction_id", rootParams.get("transaction_id")));
        request.setRefundStatus(normalizeWechatRefundStatus(decryptedParams.get("refund_status")));
        request.setReason(decryptedParams.get("refund_reason"));
        request.setGmtRefundPay(decryptedParams.getOrDefault("success_time",
            decryptedParams.getOrDefault("refund_success_time", rootParams.get("success_time"))));

        BigDecimal refundFee = extractWechatRefundAmount(decryptedParams);
        if (refundFee != null) {
            request.setRefundFee(refundFee);
        }

        return request;
    }

    /**
     * 处理退款状态
     */
    private boolean processRefundStatus(RefundCallbackRequest callbackRequest, PaymentOrder order) {
        String refundStatus = callbackRequest.getRefundStatus();
        BigDecimal refundAmount = callbackRequest.getRefundFee();

        switch (refundStatus) {
            case "REFUND_SUCCESS":
                return handleRefundSuccess(order, refundAmount, callbackRequest);
            case "REFUND_FAILED":
                return handleRefundFailed(order, refundAmount, callbackRequest);
            default:
                log.warn("未知的退款状态: {}", refundStatus);
                return false;
        }
    }

    /**
     * 处理退款成功
     */
    private boolean handleRefundSuccess(PaymentOrder order, BigDecimal refundAmount, RefundCallbackRequest callbackRequest) {
        try {
            if (PaymentStatus.REFUNDED.equals(order.getStatusEnum())) {
                return true;
            }

            BigDecimal totalAmount = order.getAmount();
            BigDecimal alreadyRefunded = order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO;
            BigDecimal delta = refundAmount != null ? refundAmount : BigDecimal.ZERO;
            BigDecimal newRefundTotal = alreadyRefunded.add(delta);

            if (totalAmount != null && newRefundTotal.compareTo(totalAmount) > 0) {
                newRefundTotal = totalAmount;
            }

            PaymentStatus newStatus = computeRefundedPaymentStatus(totalAmount, newRefundTotal);

            order.setStatus(newStatus.getCode());
            order.setRefundAmount(newRefundTotal);
            order.setRefundTime(java.time.LocalDateTime.now());

            int updateCount = paymentOrderMapper.updateById(order);
            if (updateCount > 0) {
                log.info("订单退款状态更新成功: orderId={}, refundAmount={}, status={}",
                    order.getId(), newRefundTotal, newStatus);
                return true;
            } else {
                log.error("订单退款状态更新失败: orderId={}", order.getId());
                return false;
            }
        } catch (Exception e) {
            log.error("处理退款成功状态失败: orderId={}", order.getId(), e);
            return false;
        }
    }

    private PaymentStatus computeRefundedPaymentStatus(BigDecimal totalAmount, BigDecimal refundedAmount) {
        if (totalAmount == null || refundedAmount == null) {
            return PaymentStatus.REFUNDED;
        }
        if (refundedAmount.compareTo(totalAmount) >= 0) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.PARTIALLY_REFUNDED;
    }

    /**
     * 处理退款失败
     */
    private boolean handleRefundFailed(PaymentOrder order, BigDecimal refundAmount, RefundCallbackRequest callbackRequest) {
        log.warn("退款失败: orderId={}, refundAmount={}, reason={}",
            order.getId(), refundAmount, callbackRequest.getReason());
        // 这里可以记录退款失败信息，或者发送通知
        return true; // 回调处理成功，虽然退款失败
    }

    /**
     * 验证支付宝退款回调签名
     */
    private boolean verifyAlipayRefundSignature(RefundCallbackRequest callbackRequest) {
        try {
            // 获取对应的支付渠道服务
            IPaymentChannel channel = paymentChannelMap.get("alipay");
            if (channel == null) {
                log.error("找不到支付宝支付渠道服务");
                return false;
            }

            // 使用支付渠道服务验证签名
            return channel.verifySignature(
                callbackRequest.getRawParams().toString(),
                callbackRequest.getSign()
            );
        } catch (Exception e) {
            log.error("验证支付宝退款回调签名失败", e);
            return false;
        }
    }

    /**
     * 验证微信退款回调签名
     */
    private boolean verifyWechatRefundSignature(RefundCallbackRequest callbackRequest) {
        Map<String, String> params = callbackRequest.getRawParams();
        if (params == null || params.isEmpty()) {
            log.warn("微信退款回调原始参数为空，无法验证签名");
            return false;
        }

        String signature = params.get("sign");
        if (!StringUtils.hasText(signature)) {
            log.warn("微信退款回调缺少签名字段");
            return false;
        }

        String signContent = buildWechatSignContent(params);
        if (!StringUtils.hasText(signContent)) {
            log.warn("微信退款回调签名内容为空");
            return false;
        }

        String signType = params.getOrDefault("sign_type", "MD5");
        String calculatedSignature = calculateWechatSignature(signContent, signType);
        if (!StringUtils.hasText(calculatedSignature)) {
            log.warn("微信退款回调签名计算失败，尝试使用渠道实现进行校验");
            IPaymentChannel wechatChannel = paymentChannelMap.get("wechat");
            if (wechatChannel != null) {
                boolean fallback = wechatChannel.verifySignature(signContent, signature);
                if (!fallback) {
                    log.warn("微信退款回调渠道签名校验失败");
                }
                return fallback;
            }
            return false;
        }

        boolean match = signature.equalsIgnoreCase(calculatedSignature);
        if (!match) {
            log.warn("微信退款回调签名验证失败: expected={}, actual={}", calculatedSignature, signature);
        }
        return match;
    }

    private Map<String, String> parseXmlToMap(String xmlData) {
        Map<String, String> result = new HashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlData)));
            NodeList nodeList = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.put(node.getNodeName(), node.getTextContent());
                }
            }
        } catch (Exception e) {
            log.error("解析微信退款回调XML失败", e);
        }
        return result;
    }

    private Map<String, String> decryptWechatReqInfo(String reqInfo) {
        if (!StringUtils.hasText(reqInfo)) {
            return Collections.emptyMap();
        }

        try {
            PaymentConfig.WechatConfig wechatConfig = paymentConfig != null ? paymentConfig.getWechat() : null;
            String apiV2Key = wechatConfig != null ? wechatConfig.getApiV2Key() : null;
            if (apiV2Key == null || apiV2Key.isBlank()) {
                log.warn("微信API v2密钥未配置，无法解密退款信息");
                return Collections.emptyMap();
            }

            byte[] decodedBytes = Base64.getDecoder().decode(reqInfo);
            String md5Key = md5HexLower(apiV2Key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec keySpec = new SecretKeySpec(md5Key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            String decryptedXml = new String(decrypted, StandardCharsets.UTF_8);
            return parseXmlToMap(decryptedXml);
        } catch (Exception e) {
            log.error("解密微信退款回调信息失败", e);
            return Collections.emptyMap();
        }
    }

    private String md5HexLower(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算MD5失败", e);
            return "";
        }
    }

    private BigDecimal extractWechatRefundAmount(Map<String, String> params) {
        String refundFee = params.get("refund_fee");
        if (!StringUtils.hasText(refundFee)) {
            refundFee = params.get("settlement_refund_fee");
        }
        if (!StringUtils.hasText(refundFee)) {
            return null;
        }
        try {
            return new BigDecimal(refundFee).movePointLeft(2);
        } catch (NumberFormatException ex) {
            log.warn("解析微信退款金额失败: {}", refundFee, ex);
            return null;
        }
    }

    private String normalizeWechatRefundStatus(String refundStatus) {
        if (!StringUtils.hasText(refundStatus)) {
            return "REFUND_FAILED";
        }
        switch (refundStatus.toUpperCase(Locale.ROOT)) {
            case "SUCCESS":
                return "REFUND_SUCCESS";
            case "CHANGE":
            case "REFUNDCLOSE":
                return "REFUND_FAILED";
            default:
                return refundStatus;
        }
    }

    private String buildWechatSignContent(Map<String, String> params) {
        return params.entrySet().stream()
            .filter(entry -> StringUtils.hasText(entry.getValue()))
            .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));
    }

    private String calculateWechatSignature(String signContent, String signType) {
        PaymentConfig.WechatConfig wechatConfig = paymentConfig != null ? paymentConfig.getWechat() : null;
        if (wechatConfig == null || !StringUtils.hasText(wechatConfig.getApiV2Key())) {
            log.warn("微信API v2密钥未配置，无法计算签名");
            return null;
        }

        String contentWithKey = signContent + "&key=" + wechatConfig.getApiV2Key();
        try {
            if ("HMAC-SHA256".equalsIgnoreCase(signType)) {
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(wechatConfig.getApiV2Key().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(secretKeySpec);
                byte[] bytes = mac.doFinal(contentWithKey.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(bytes).toUpperCase(Locale.ROOT);
            }

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(contentWithKey.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            log.error("计算微信退款签名失败", e);
            return null;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }
}
