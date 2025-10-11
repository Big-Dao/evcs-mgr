package com.evcs.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付订单Mapper
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
