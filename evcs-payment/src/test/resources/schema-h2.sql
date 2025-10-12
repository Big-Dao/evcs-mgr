-- Payment Order schema for H2 test database

CREATE TABLE IF NOT EXISTS payment_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    trade_no VARCHAR(100) NOT NULL,
    payment_method VARCHAR(50),
    amount DECIMAL(10,2) NOT NULL,
    status INTEGER DEFAULT 0,
    out_trade_no VARCHAR(100),
    paid_time TIMESTAMP,
    idempotent_key VARCHAR(100),
    description VARCHAR(500),
    pay_params TEXT,
    pay_url VARCHAR(500),
    refund_amount DECIMAL(10,2),
    refund_time TIMESTAMP,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_trade_no ON payment_order(trade_no);
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payment_order(order_id);
