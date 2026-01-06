package com.evcs.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evcs.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付订单Mapper
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

	@Select("""
		SELECT DISTINCT tenant_id
		FROM payment_order
		WHERE status = #{status}
		  AND trade_no IS NOT NULL
		  AND trade_no <> ''
		  AND create_time <= #{cutoff}
		ORDER BY tenant_id
		LIMIT #{limit}
		""")
	List<Long> selectTenantIdsWithStatusBefore(
		@Param("status") Integer status,
		@Param("cutoff") LocalDateTime cutoff,
		@Param("limit") int limit
	);
}
