package com.evcs.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.payment.entity.PaymentSyncRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付同步记录Mapper
 */
@Mapper
public interface PaymentSyncRecordMapper extends BaseMapper<PaymentSyncRecord> {
}
