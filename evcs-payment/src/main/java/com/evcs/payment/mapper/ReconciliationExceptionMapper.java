package com.evcs.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.payment.entity.ReconciliationExceptionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReconciliationExceptionMapper extends BaseMapper<ReconciliationExceptionRecord> {
}
