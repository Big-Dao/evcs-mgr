-- 支付订单表
CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL COMMENT '业务订单ID',
    trade_no VARCHAR(64) NOT NULL COMMENT '交易流水号',
    payment_method VARCHAR(32) NOT NULL COMMENT '支付方式',
    amount DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    status INTEGER NOT NULL DEFAULT 0 COMMENT '支付状态：0-待支付，1-支付中，2-支付成功，3-支付失败，4-退款中，5-已退款',
    out_trade_no VARCHAR(64) COMMENT '第三方支付流水号',
    paid_time TIMESTAMP COMMENT '支付完成时间',
    idempotent_key VARCHAR(64) COMMENT '幂等键',
    description VARCHAR(255) COMMENT '支付描述',
    pay_params TEXT COMMENT '支付参数（JSON）',
    pay_url VARCHAR(512) COMMENT '支付URL',
    refund_amount DECIMAL(10, 2) COMMENT '退款金额',
    refund_time TIMESTAMP COMMENT '退款时间',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0
);

-- 创建索引
CREATE INDEX idx_payment_order_tenant_id ON payment_order(tenant_id);
CREATE INDEX idx_payment_order_order_id ON payment_order(order_id);
CREATE UNIQUE INDEX uk_payment_order_trade_no ON payment_order(trade_no);
CREATE INDEX idx_payment_order_idempotent_key ON payment_order(idempotent_key) WHERE idempotent_key IS NOT NULL;
CREATE INDEX idx_payment_order_status ON payment_order(tenant_id, status);

-- 添加注释
COMMENT ON TABLE payment_order IS '支付订单表';
COMMENT ON COLUMN payment_order.id IS '主键ID';
COMMENT ON COLUMN payment_order.tenant_id IS '租户ID';
COMMENT ON COLUMN payment_order.order_id IS '业务订单ID（充电订单ID）';
COMMENT ON COLUMN payment_order.trade_no IS '交易流水号';
COMMENT ON COLUMN payment_order.payment_method IS '支付方式';
COMMENT ON COLUMN payment_order.amount IS '支付金额';
COMMENT ON COLUMN payment_order.status IS '支付状态：0-待支付，1-支付中，2-支付成功，3-支付失败，4-退款中，5-已退款';
COMMENT ON COLUMN payment_order.out_trade_no IS '第三方支付流水号';
COMMENT ON COLUMN payment_order.paid_time IS '支付完成时间';
COMMENT ON COLUMN payment_order.idempotent_key IS '幂等键';
COMMENT ON COLUMN payment_order.description IS '支付描述';
COMMENT ON COLUMN payment_order.pay_params IS '支付参数（JSON）';
COMMENT ON COLUMN payment_order.pay_url IS '支付URL';
COMMENT ON COLUMN payment_order.refund_amount IS '退款金额';
COMMENT ON COLUMN payment_order.refund_time IS '退款时间';

-- 更新时间触发器
CREATE OR REPLACE FUNCTION update_payment_order_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_payment_order_updated_at
    BEFORE UPDATE ON payment_order
    FOR EACH ROW
    EXECUTE FUNCTION update_payment_order_updated_at();
