package com.evcs.payment.service.reconciliation;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayDataDataserviceBillDownloadurlQueryModel;
import com.alipay.api.request.AlipayDataDataserviceBillDownloadurlQueryRequest;
import com.alipay.api.response.AlipayDataDataserviceBillDownloadurlQueryResponse;
import com.evcs.payment.config.AlipayConfig;
import com.evcs.payment.service.channel.AlipayClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付宝对账服务
 */
@Slf4j
@Service
public class AlipayReconciliationService {

    @Resource
    private AlipayClientFactory alipayClientFactory;

    @Resource
    private AlipayConfig alipayConfig;

    /**
     * 下载支付宝对账单URL
     */
    public String downloadBillUrl(LocalDate date) throws AlipayApiException {
        log.info("获取支付宝对账单下载URL: date={}", date);

        AlipayClient alipayClient = alipayClientFactory.getAlipayClient();
        AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();

        AlipayDataDataserviceBillDownloadurlQueryModel model = new AlipayDataDataserviceBillDownloadurlQueryModel();
        model.setBillType("trade"); // 账单类型：trade账单
        model.setBillDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        request.setBizModel(model);

        AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);

        if (response.isSuccess()) {
            String billDownloadUrl = response.getBillDownloadUrl();
            log.info("支付宝对账单下载URL获取成功: date={}, url={}", date, billDownloadUrl);
            return billDownloadUrl;
        } else {
            log.error("支付宝对账单下载URL获取失败: date={}, error={}", date, response.getSubMsg());
            throw new AlipayApiException("获取支付宝对账单下载URL失败: " + response.getSubMsg());
        }
    }

    /**
     * 下载并解析支付宝对账单
     */
    public List<AlipayBillRecord> downloadAndParseBill(LocalDate date) throws Exception {
        log.info("开始下载并解析支付宝对账单: date={}", date);

        try {
            // 1. 获取对账单下载URL
            String billDownloadUrl = downloadBillUrl(date);

            // 2. 下载对账单文件
            String billContent = downloadBillContent(billDownloadUrl);

            // 3. 解析对账单内容
            List<AlipayBillRecord> records = parseBillContent(billContent);

            log.info("支付宝对账单解析完成: date={}, recordCount={}", date, records.size());
            return records;

        } catch (Exception e) {
            log.error("下载并解析支付宝对账单失败: date={}", date, e);
            throw e;
        }
    }

    /**
     * 下载对账单文件内容
     */
    private String downloadBillContent(String billDownloadUrl) throws IOException {
        log.info("下载支付宝对账单文件: url={}", billDownloadUrl);

        // 使用URI替代废弃的URL构造器
        URI uri = URI.create(billDownloadUrl);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            return content.toString();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 解析支付宝对账单内容
     *
     * 支付宝对账单格式：
     * # 支付宝交易明细账单
     * # 账单时间：2024-11-02
     * # 商户订单号,支付宝交易号,业务类型,商品名称,创建时间,完成时间,门店编号,门店名称,操作员,终端号,对方账号,订单金额,商家红包,集分宝,支付宝红包,商家优惠,商家优惠金额,券核销金额,券名称,卡消费金额,卡品牌,分期付款金额,分期付款手续费,分期付款净额,退款金额,资金服务费,实收金额,备注,分润明细
     */
    private List<AlipayBillRecord> parseBillContent(String billContent) {
        List<AlipayBillRecord> records = new ArrayList<>();
        String[] lines = billContent.split("\n");

        boolean dataStart = false;
        for (String line : lines) {
            line = line.trim();

            // 跳过注释行和空行
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // 检查是否是数据开始行
            if (!dataStart) {
                if (line.contains("商户订单号")) {
                    dataStart = true;
                }
                continue;
            }

            try {
                AlipayBillRecord record = parseBillLine(line);
                if (record != null) {
                    records.add(record);
                }
            } catch (Exception e) {
                log.warn("解析对账单行失败: line={}", line, e);
            }
        }

        return records;
    }

    /**
     * 解析单行对账单数据
     */
    private AlipayBillRecord parseBillLine(String line) {
        String[] fields = line.split(",", -1); // -1保留空字段

        if (fields.length < 24) {
            log.warn("对账单字段数量不足: line={}", line);
            return null;
        }

        try {
            AlipayBillRecord record = new AlipayBillRecord();
            record.setOutTradeNo(fields[0]);           // 商户订单号
            record.setTradeNo(fields[1]);              // 支付宝交易号
            record.setBusinessType(fields[2]);         // 业务类型
            record.setGoodsName(fields[3]);            // 商品名称
            record.setCreateTime(fields[4]);           // 创建时间
            record.setFinishTime(fields[5]);           // 完成时间
            record.setStoreId(fields[6]);              // 门店编号
            record.setStoreName(fields[7]);            // 门店名称
            record.setOperator(fields[8]);             // 操作员
            record.setTerminalId(fields[9]);           // 终端号
            record.setOtherAccount(fields[10]);        // 对方账号
            record.setTotalAmount(parseAmount(fields[11])); // 订单金额
            record.setMerchantRedAmount(parseAmount(fields[12])); // 商家红包
            record.setJifenbaoAmount(parseAmount(fields[13]));  // 集分宝
            record.setAlipayRedAmount(parseAmount(fields[14])); // 支付宝红包
            record.setMerchantDiscountAmount(parseAmount(fields[15])); // 商家优惠
            record.setMerchantDiscount(parseAmount(fields[16]));   // 商家优惠金额
            record.setCouponAmount(parseAmount(fields[17]));      // 券核销金额
            record.setCouponName(fields[18]);          // 券名称
            record.setCardAmount(parseAmount(fields[19]));       // 卡消费金额
            record.setCardBrand(fields[20]);           // 卡品牌
            record.setInstallmentAmount(parseAmount(fields[21])); // 分期付款金额
            record.setInstallmentFee(parseAmount(fields[22]));   // 分期付款手续费
            record.setInstallmentNetAmount(parseAmount(fields[23])); // 分期付款净额
            record.setRefundAmount(parseAmount(fields[24]));      // 退款金额

            if (fields.length > 25) {
                record.setServiceFee(parseAmount(fields[25]));   // 资金服务费
            }
            if (fields.length > 26) {
                record.setActualAmount(parseAmount(fields[26]));  // 实收金额
            }
            if (fields.length > 27) {
                record.setRemark(fields[27]);             // 备注
            }

            return record;
        } catch (Exception e) {
            log.error("解析对账单行失败: line={}", line, e);
            return null;
        }
    }

    /**
     * 解析金额字段
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(amountStr.trim());
        } catch (NumberFormatException e) {
            log.warn("金额格式错误: {}", amountStr);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 支付宝账单记录
     */
    public static class AlipayBillRecord {
        private String outTradeNo;            // 商户订单号
        private String tradeNo;               // 支付宝交易号
        private String businessType;          // 业务类型
        private String goodsName;             // 商品名称
        private String createTime;            // 创建时间
        private String finishTime;            // 完成时间
        private String storeId;               // 门店编号
        private String storeName;             // 门店名称
        private String operator;              // 操作员
        private String terminalId;            // 终端号
        private String otherAccount;          // 对方账号
        private BigDecimal totalAmount;       // 订单金额
        private BigDecimal merchantRedAmount; // 商家红包
        private BigDecimal jifenbaoAmount;    // 集分宝
        private BigDecimal alipayRedAmount;   // 支付宝红包
        private BigDecimal merchantDiscountAmount; // 商家优惠
        private BigDecimal merchantDiscount;  // 商家优惠金额
        private BigDecimal couponAmount;      // 券核销金额
        private String couponName;            // 券名称
        private BigDecimal cardAmount;        // 卡消费金额
        private String cardBrand;             // 卡品牌
        private BigDecimal installmentAmount; // 分期付款金额
        private BigDecimal installmentFee;    // 分期付款手续费
        private BigDecimal installmentNetAmount; // 分期付款净额
        private BigDecimal refundAmount;      // 退款金额
        private BigDecimal serviceFee;        // 资金服务费
        private BigDecimal actualAmount;      // 实收金额
        private String remark;                // 备注

        // Getters and Setters
        public String getOutTradeNo() { return outTradeNo; }
        public void setOutTradeNo(String outTradeNo) { this.outTradeNo = outTradeNo; }

        public String getTradeNo() { return tradeNo; }
        public void setTradeNo(String tradeNo) { this.tradeNo = tradeNo; }

        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }

        public String getGoodsName() { return goodsName; }
        public void setGoodsName(String goodsName) { this.goodsName = goodsName; }

        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }

        public String getFinishTime() { return finishTime; }
        public void setFinishTime(String finishTime) { this.finishTime = finishTime; }

        public String getStoreId() { return storeId; }
        public void setStoreId(String storeId) { this.storeId = storeId; }

        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }

        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }

        public String getTerminalId() { return terminalId; }
        public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

        public String getOtherAccount() { return otherAccount; }
        public void setOtherAccount(String otherAccount) { this.otherAccount = otherAccount; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public BigDecimal getMerchantRedAmount() { return merchantRedAmount; }
        public void setMerchantRedAmount(BigDecimal merchantRedAmount) { this.merchantRedAmount = merchantRedAmount; }

        public BigDecimal getJifenbaoAmount() { return jifenbaoAmount; }
        public void setJifenbaoAmount(BigDecimal jifenbaoAmount) { this.jifenbaoAmount = jifenbaoAmount; }

        public BigDecimal getAlipayRedAmount() { return alipayRedAmount; }
        public void setAlipayRedAmount(BigDecimal alipayRedAmount) { this.alipayRedAmount = alipayRedAmount; }

        public BigDecimal getMerchantDiscountAmount() { return merchantDiscountAmount; }
        public void setMerchantDiscountAmount(BigDecimal merchantDiscountAmount) { this.merchantDiscountAmount = merchantDiscountAmount; }

        public BigDecimal getMerchantDiscount() { return merchantDiscount; }
        public void setMerchantDiscount(BigDecimal merchantDiscount) { this.merchantDiscount = merchantDiscount; }

        public BigDecimal getCouponAmount() { return couponAmount; }
        public void setCouponAmount(BigDecimal couponAmount) { this.couponAmount = couponAmount; }

        public String getCouponName() { return couponName; }
        public void setCouponName(String couponName) { this.couponName = couponName; }

        public BigDecimal getCardAmount() { return cardAmount; }
        public void setCardAmount(BigDecimal cardAmount) { this.cardAmount = cardAmount; }

        public String getCardBrand() { return cardBrand; }
        public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }

        public BigDecimal getInstallmentAmount() { return installmentAmount; }
        public void setInstallmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; }

        public BigDecimal getInstallmentFee() { return installmentFee; }
        public void setInstallmentFee(BigDecimal installmentFee) { this.installmentFee = installmentFee; }

        public BigDecimal getInstallmentNetAmount() { return installmentNetAmount; }
        public void setInstallmentNetAmount(BigDecimal installmentNetAmount) { this.installmentNetAmount = installmentNetAmount; }

        public BigDecimal getRefundAmount() { return refundAmount; }
        public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

        public BigDecimal getServiceFee() { return serviceFee; }
        public void setServiceFee(BigDecimal serviceFee) { this.serviceFee = serviceFee; }

        public BigDecimal getActualAmount() { return actualAmount; }
        public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}