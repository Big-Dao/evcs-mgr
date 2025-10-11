package com.evcs.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evcs.common.annotation.DataScope;
import com.evcs.payment.dto.ReconciliationRequest;
import com.evcs.payment.dto.ReconciliationResult;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.service.IReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 对账服务实现
 * 
 * TODO: 完整实现需要：
 * 1. 集成支付宝对账单下载API
 * 2. 集成微信支付对账单下载API
 * 3. 解析对账单CSV文件
 * 4. 比对系统订单与对账单数据
 * 5. 生成对账报表并导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements IReconciliationService {

    private final PaymentOrderMapper paymentOrderMapper;

    @Override
    @DataScope
    public ReconciliationResult reconcile(ReconciliationRequest request) {
        log.info("开始对账: date={}, channel={}", request.getReconciliationDate(), request.getChannel());

        LocalDate date = request.getReconciliationDate();
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

        // 查询当天的支付订单
        List<PaymentOrder> orders = paymentOrderMapper.selectList(
            new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, PaymentStatus.SUCCESS.getCode())
                .like(PaymentOrder::getPaymentMethod, request.getChannel())
                .between(PaymentOrder::getCreateTime, startTime, endTime)
        );

        // 计算系统总金额
        BigDecimal systemTotalAmount = orders.stream()
            .map(PaymentOrder::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // TODO: 从支付渠道下载对账单并比对
        // 现阶段使用模拟数据
        BigDecimal channelTotalAmount = systemTotalAmount; // 假设渠道金额一致
        int totalCount = orders.size();
        int matchedCount = orders.size(); // 假设全部匹配
        int mismatchCount = 0;

        // 计算对账成功率
        double successRate = totalCount > 0 
            ? (double) matchedCount / totalCount * 100 
            : 100.0;

        // 计算金额差异
        BigDecimal amountDifference = systemTotalAmount.subtract(channelTotalAmount).abs();

        // 确定对账状态
        String status;
        if (mismatchCount == 0 && amountDifference.compareTo(BigDecimal.ZERO) == 0) {
            status = "SUCCESS";
        } else if (mismatchCount == totalCount) {
            status = "FAILED";
        } else {
            status = "PARTIAL";
        }

        ReconciliationResult result = ReconciliationResult.builder()
            .reconciliationDate(date)
            .channel(request.getChannel())
            .totalCount(totalCount)
            .matchedCount(matchedCount)
            .mismatchCount(mismatchCount)
            .systemTotalAmount(systemTotalAmount)
            .channelTotalAmount(channelTotalAmount)
            .amountDifference(amountDifference)
            .successRate(BigDecimal.valueOf(successRate).setScale(2, RoundingMode.HALF_UP).doubleValue())
            .status(status)
            .build();

        log.info("对账完成: date={}, channel={}, totalCount={}, matchedCount={}, successRate={}%", 
            date, request.getChannel(), totalCount, matchedCount, result.getSuccessRate());

        return result;
    }

    @Override
    public ReconciliationResult dailyReconciliation(String channel) {
        // 对昨天的订单进行对账
        LocalDate yesterday = LocalDate.now().minusDays(1);
        ReconciliationRequest request = new ReconciliationRequest();
        request.setReconciliationDate(yesterday);
        request.setChannel(channel);
        return reconcile(request);
    }
}
