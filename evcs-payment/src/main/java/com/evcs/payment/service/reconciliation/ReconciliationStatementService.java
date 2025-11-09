package com.evcs.payment.service.reconciliation;

import com.evcs.payment.dto.ReconciliationStatement;

import java.time.LocalDate;

/**
 * 对账单服务接口
 *
 * 负责从支付渠道下载和解析对账单
 */
public interface ReconciliationStatementService {

    /**
     * 下载对账单
     *
     * @param channel 支付渠道 (alipay/wechat)
     * @param date 对账日期
     * @return 对账单数据
     */
    ReconciliationStatement downloadStatement(String channel, LocalDate date);

    /**
     * 解析对账单
     *
     * @param channel 支付渠道
     * @param statementData 对账单原始数据
     * @return 解析后的对账单
     */
    ReconciliationStatement parseStatement(String channel, String statementData);

    /**
     * 验证对账单格式
     *
     * @param channel 支付渠道
     * @param statementData 对账单数据
     * @return 是否格式正确
     */
    boolean validateStatementFormat(String channel, String statementData);
}