# evcs-user 模块规划 RFC

> **版本**: v1.6  
> **最后更新**: 2026-01-13  
> **维护者**: 架构团队  
> **状态**: 草稿

## 概述

本 RFC 规划为 EVCS 充电站管理系统引入 **evcs-user** 模块，用于管理最终充电用户（C 端用户），支撑积分、优惠券、客诉、会员等业务场景。

### 背景

当前系统用户模型（`sys_user`）主要面向 B 端运营人员，缺少对 C 端充电用户的专门管理能力。随着业务发展，需要：
- 独立的充电用户账户体系
- 用户积分与会员等级
- 优惠券发放与核销
- 客诉工单管理
- 用户行为分析

### 目标

1. 建立独立的 C 端用户模型，与 B 端用户解耦
2. 支持多租户隔离，运营商可管理各自用户
3. 提供积分、优惠券、客诉等核心能力
4. 预留用户画像与营销扩展点
5. 支持账户安全、钱包充值、消息通知等完整用户生命周期管理

---

## 模块架构

### 服务定位

```
evcs-user (8088)  - 用户管理服务，C端用户、积分、优惠券、客诉
```

### 与现有模块关系

```
┌─────────────────────────────────────────────────────────────────┐
│                        evcs-gateway                              │
└────────────────┬────────────────────────────────────────────────┘
                 │
    ┌────────────┼────────────┬────────────┬────────────┐
    │            │            │            │            │
    ▼            ▼            ▼            ▼            ▼
evcs-auth   evcs-user    evcs-order  evcs-station  evcs-payment
(B端认证)   (C端用户)    (订单)       (站点)        (支付)
    │            │            │            │            │
    │            │◄───────────┤            │            │
    │            │ 用户ID关联  │◄───────────┤            │
    │            │◄────────────────────────────────────┤
    │            │        优惠券核销/积分抵扣           │
    └────────────┴────────────┴────────────┴────────────┘
                              │
                        evcs-common
                      (多租户/公共组件)
```

### 核心领域模型

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Aggregate                            │
├─────────────────────────────────────────────────────────────────┤
│  ChargingUser (聚合根)                                          │
│    ├── UserProfile (用户画像)                                   │
│    ├── UserWallet  (钱包/余额)                                  │
│    ├── PointsAccount (积分账户)                                 │
│    ├── MemberLevel (会员等级)                                   │
│    ├── UserDevice (登录设备)                                    │
│    └── UserVehicle (绑定车辆)                                   │
├─────────────────────────────────────────────────────────────────┤
│  UserGroup Aggregate                                             │
│    ├── UserGroup (用户群组)                                     │
│    ├── UserGroupMember (群组成员)                               │
│    └── GroupBenefit (群组权益)                                  │
├─────────────────────────────────────────────────────────────────┤
│  Profile & Tag Aggregate                                         │
│    ├── UserProfileData (画像数据)                               │
│    ├── UserTag (标签定义)                                       │
│    ├── UserTagRelation (用户标签关联)                           │
│    └── UserBehaviorEvent (行为事件)                             │
├─────────────────────────────────────────────────────────────────┤
│  Marketing Aggregate                                             │
│    ├── MarketingCampaign (营销活动)                             │
│    ├── CampaignParticipation (活动参与记录)                     │
│    ├── BenefitPackage (权益包/月卡)                             │
│    └── UserPackageSubscription (用户订阅)                       │
├─────────────────────────────────────────────────────────────────┤
│  Task Aggregate                                                  │
│    ├── UserTask (任务定义)                                      │
│    └── UserTaskProgress (任务进度)                              │
├─────────────────────────────────────────────────────────────────┤
│  Coupon Aggregate                                                │
│    ├── CouponTemplate (优惠券模板)                              │
│    └── UserCoupon (用户持有的优惠券)                            │
├─────────────────────────────────────────────────────────────────┤
│  Complaint Aggregate                                             │
│    ├── Complaint (客诉工单)                                     │
│    └── ComplaintRecord (处理记录)                               │
├─────────────────────────────────────────────────────────────────┤
│  Message Aggregate                                               │
│    ├── UserMessage (站内消息)                                   │
│    └── MessageTemplate (消息模板)                               │
├─────────────────────────────────────────────────────────────────┤
│  Invitation Aggregate                                            │
│    └── UserInvitation (邀请记录)                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 数据库设计

### 1. 充电用户表 (charging_user)

```sql
CREATE TABLE charging_user (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    -- 账户信息
    phone VARCHAR(20) NOT NULL,                  -- 手机号（登录标识）
    password VARCHAR(100),                       -- 密码（可选，支持验证码登录）
    nickname VARCHAR(50),                        -- 昵称
    avatar VARCHAR(500),                         -- 头像URL
    gender INTEGER DEFAULT 0,                    -- 0-未知 1-男 2-女
    birthday DATE,                               -- 生日
    
    -- 实名信息（可选）
    real_name VARCHAR(50),                       -- 真实姓名
    id_card VARCHAR(30),                         -- 身份证号（脱敏存储）
    is_verified INTEGER DEFAULT 0,              -- 0-未实名 1-已实名
    
    -- 会员信息
    member_level INTEGER DEFAULT 1,             -- 会员等级 1-普通 2-银卡 3-金卡 4-钻石
    total_points BIGINT DEFAULT 0,              -- 累计积分
    available_points BIGINT DEFAULT 0,          -- 可用积分
    
    -- 钱包
    balance DECIMAL(12,2) DEFAULT 0,            -- 账户余额
    
    -- 状态
    status INTEGER DEFAULT 1,                    -- 0-禁用 1-正常 2-冻结 3-待完善(扫码注册)
    register_source VARCHAR(20),                -- 注册来源: APP/MINI_PROGRAM/WEB/SCAN_QR/ADMIN
    register_channel VARCHAR(50),               -- 注册渠道
    register_connector_id BIGINT,               -- 注册时扫码的充电枪ID（扫码注册场景）
    
    -- 统计
    total_charge_count INTEGER DEFAULT 0,       -- 累计充电次数
    total_charge_energy DECIMAL(12,4) DEFAULT 0,-- 累计充电度数
    total_charge_amount DECIMAL(12,2) DEFAULT 0,-- 累计消费金额
    last_charge_time TIMESTAMP,                 -- 最后充电时间
    
    -- 登录信息
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    last_active_time TIMESTAMP,                 -- 最后活跃时间（用于风险检测）
    phone_bindtime TIMESTAMP,                   -- 手机号绑定时间
    risk_level INTEGER DEFAULT 0,               -- 风险等级 0正常 1低 2中 3高
    
    -- 标准字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

-- 索引
CREATE UNIQUE INDEX uk_charging_user_phone ON charging_user(phone) WHERE deleted = 0;
CREATE INDEX idx_charging_user_tenant ON charging_user(tenant_id, status, deleted);
CREATE INDEX idx_charging_user_member ON charging_user(tenant_id, member_level);
CREATE INDEX idx_charging_user_phone_tenant ON charging_user(tenant_id, phone) WHERE deleted = 0;

COMMENT ON TABLE charging_user IS '充电用户表（C端用户）';
```

### 2. 用户第三方账号绑定表 (user_oauth)

```sql
CREATE TABLE user_oauth (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,                     -- 关联 charging_user.id
    
    oauth_type VARCHAR(20) NOT NULL,            -- WECHAT/ALIPAY/APPLE
    oauth_id VARCHAR(100) NOT NULL,             -- 第三方唯一标识
    union_id VARCHAR(100),                      -- 微信UnionID等
    oauth_nickname VARCHAR(100),
    oauth_avatar VARCHAR(500),
    access_token TEXT,
    refresh_token TEXT,
    token_expire_time TIMESTAMP,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_user_oauth ON user_oauth(oauth_type, oauth_id) WHERE deleted = 0;
CREATE INDEX idx_user_oauth_user ON user_oauth(user_id);

COMMENT ON TABLE user_oauth IS '用户第三方账号绑定表';
```

### 3. 积分流水表 (points_transaction)

```sql
CREATE TABLE points_transaction (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    transaction_no VARCHAR(64) NOT NULL,        -- 流水号
    type INTEGER NOT NULL,                       -- 1-获取 2-消费 3-过期 4-调整
    points BIGINT NOT NULL,                      -- 积分数量（正/负）
    balance_after BIGINT NOT NULL,              -- 交易后余额
    
    source VARCHAR(30),                          -- 来源: CHARGE/SIGN_IN/ACTIVITY/REFUND/EXPIRE
    source_id VARCHAR(64),                       -- 关联业务ID（订单号等）
    description VARCHAR(200),                    -- 描述
    expire_time TIMESTAMP,                       -- 过期时间（仅获取类型）
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT
);

CREATE INDEX idx_points_trans_user ON points_transaction(tenant_id, user_id, create_time);
CREATE INDEX idx_points_trans_no ON points_transaction(transaction_no);
CREATE INDEX idx_points_trans_source ON points_transaction(source, source_id);

COMMENT ON TABLE points_transaction IS '积分流水表';
```

### 4. 优惠券模板表 (coupon_template)

```sql
CREATE TABLE coupon_template (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    name VARCHAR(100) NOT NULL,                 -- 优惠券名称
    code VARCHAR(50),                           -- 优惠券编码
    type INTEGER NOT NULL,                       -- 1-满减券 2-折扣券 3-免费充电券
    
    -- 面值/折扣
    discount_amount DECIMAL(12,2),              -- 满减金额/固定金额
    discount_rate DECIMAL(5,2),                 -- 折扣率（0.8=8折）
    max_discount DECIMAL(12,2),                 -- 最大折扣金额（折扣券使用）
    
    -- 使用条件
    min_amount DECIMAL(12,2) DEFAULT 0,         -- 最低消费金额
    min_energy DECIMAL(12,4),                   -- 最低充电度数
    
    -- 适用范围
    apply_scope INTEGER DEFAULT 1,              -- 1-全部 2-指定站点 3-指定类型
    apply_station_ids TEXT,                     -- 适用站点ID列表（JSON数组）
    apply_charger_types TEXT,                   -- 适用充电桩类型（JSON数组）
    
    -- 发放信息
    total_count INTEGER,                        -- 发放总量（NULL=不限）
    issued_count INTEGER DEFAULT 0,             -- 已发放数量
    per_user_limit INTEGER DEFAULT 1,           -- 每人限领数量
    
    -- 有效期
    validity_type INTEGER DEFAULT 1,            -- 1-固定日期 2-领取后N天
    valid_start_time TIMESTAMP,                 -- 固定开始时间
    valid_end_time TIMESTAMP,                   -- 固定结束时间
    validity_days INTEGER,                      -- 领取后有效天数
    
    status INTEGER DEFAULT 1,                   -- 0-下架 1-上架
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE INDEX idx_coupon_tpl_tenant ON coupon_template(tenant_id, status, deleted);
CREATE INDEX idx_coupon_tpl_code ON coupon_template(tenant_id, code) WHERE code IS NOT NULL;

COMMENT ON TABLE coupon_template IS '优惠券模板表';
```

### 5. 用户优惠券表 (user_coupon)

```sql
CREATE TABLE user_coupon (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    
    coupon_code VARCHAR(32) NOT NULL,           -- 券码（唯一）
    status INTEGER DEFAULT 1,                    -- 1-未使用 2-已使用 3-已过期 4-已作废
    
    -- 冗余优惠券信息（快照）
    coupon_name VARCHAR(100),
    coupon_type INTEGER,
    discount_amount DECIMAL(12,2),
    discount_rate DECIMAL(5,2),
    max_discount DECIMAL(12,2),
    min_amount DECIMAL(12,2),
    
    -- 有效期
    valid_start_time TIMESTAMP,
    valid_end_time TIMESTAMP,
    
    -- 使用信息
    use_time TIMESTAMP,
    use_order_id BIGINT,                        -- 使用的订单ID
    use_amount DECIMAL(12,2),                   -- 实际抵扣金额
    
    -- 来源
    source VARCHAR(30),                         -- ACTIVITY/EXCHANGE/GIFT/SYSTEM
    source_id VARCHAR(64),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_user_coupon_code ON user_coupon(coupon_code) WHERE deleted = 0;
CREATE INDEX idx_user_coupon_user ON user_coupon(tenant_id, user_id, status);
CREATE INDEX idx_user_coupon_expire ON user_coupon(valid_end_time) WHERE status = 1;

COMMENT ON TABLE user_coupon IS '用户优惠券表';
```

### 6. 客诉工单表 (complaint)

```sql
CREATE TABLE complaint (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    complaint_no VARCHAR(32) NOT NULL,          -- 工单编号
    type INTEGER NOT NULL,                       -- 1-充电问题 2-支付问题 3-设备故障 4-服务投诉 5-其他
    priority INTEGER DEFAULT 2,                  -- 1-紧急 2-普通 3-低
    
    -- 关联信息
    order_id BIGINT,                            -- 关联订单
    station_id BIGINT,                          -- 关联站点
    charger_id BIGINT,                          -- 关联充电桩
    
    -- 投诉内容
    title VARCHAR(200) NOT NULL,                -- 标题
    content TEXT NOT NULL,                      -- 描述
    images TEXT,                                -- 图片URL列表（JSON数组）
    contact_phone VARCHAR(20),                  -- 联系电话
    
    -- 处理信息
    status INTEGER DEFAULT 1,                   -- 1-待处理 2-处理中 3-待用户确认 4-已完成 5-已关闭
    handler_id BIGINT,                          -- 处理人ID（sys_user）
    handler_name VARCHAR(50),                   -- 处理人姓名
    handle_time TIMESTAMP,                      -- 开始处理时间
    close_time TIMESTAMP,                       -- 关闭时间
    
    -- 评价
    satisfaction INTEGER,                       -- 满意度 1-5
    evaluation TEXT,                            -- 用户评价
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_complaint_no ON complaint(complaint_no) WHERE deleted = 0;
CREATE INDEX idx_complaint_tenant ON complaint(tenant_id, status, deleted);
CREATE INDEX idx_complaint_user ON complaint(user_id);
CREATE INDEX idx_complaint_handler ON complaint(handler_id) WHERE handler_id IS NOT NULL;

COMMENT ON TABLE complaint IS '客诉工单表';
```

### 7. 客诉处理记录表 (complaint_record)

```sql
CREATE TABLE complaint_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    complaint_id BIGINT NOT NULL,
    
    type INTEGER NOT NULL,                       -- 1-系统 2-客服回复 3-用户回复
    content TEXT NOT NULL,                       -- 内容
    images TEXT,                                 -- 图片URL列表
    
    operator_id BIGINT,                         -- 操作人ID
    operator_name VARCHAR(50),                  -- 操作人姓名
    operator_type INTEGER,                      -- 1-系统 2-客服 3-用户
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_complaint_record ON complaint_record(complaint_id, create_time);

COMMENT ON TABLE complaint_record IS '客诉处理记录表';
```

### 8. 登录日志表 (user_login_log)

```sql
CREATE TABLE user_login_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    login_type VARCHAR(20),              -- PASSWORD/SMS/WECHAT/ALIPAY
    login_result INTEGER,                -- 1-成功 0-失败
    fail_reason VARCHAR(100),
    
    device_id VARCHAR(64),
    device_type VARCHAR(20),             -- IOS/ANDROID/WEB/MINI_PROGRAM
    device_model VARCHAR(50),
    app_version VARCHAR(20),
    ip VARCHAR(50),
    location VARCHAR(100),
    
    -- 风险检测
    risk_flag INTEGER DEFAULT 0,         -- 0正常 1可疑 2高危
    risk_reason VARCHAR(200),            -- 风险原因
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_log_user ON user_login_log(user_id, create_time);
CREATE INDEX idx_login_log_time ON user_login_log(create_time);

COMMENT ON TABLE user_login_log IS '用户登录日志表';
```

### 9. 用户设备表 (user_device)

```sql
CREATE TABLE user_device (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    device_id VARCHAR(64) NOT NULL,
    device_type VARCHAR(20),             -- IOS/ANDROID/WEB/MINI_PROGRAM
    device_name VARCHAR(100),
    device_model VARCHAR(50),
    os_version VARCHAR(30),
    app_version VARCHAR(20),
    push_token VARCHAR(200),             -- 推送token
    
    is_current INTEGER DEFAULT 0,        -- 当前登录设备
    is_trusted INTEGER DEFAULT 0,        -- 是否信任设备
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    last_active_time TIMESTAMP,          -- 最后活跃时间
    status INTEGER DEFAULT 1,            -- 1正常 0已踢出 2已过期
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_user_device_user ON user_device(user_id) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_user_device ON user_device(user_id, device_id) WHERE deleted = 0;

COMMENT ON TABLE user_device IS '用户设备表';
```

### 10. 余额流水表 (balance_transaction)

```sql
CREATE TABLE balance_transaction (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    transaction_no VARCHAR(64) NOT NULL,
    type INTEGER NOT NULL,                -- 1-充值 2-消费 3-退款 4-提现 5-调整 6-充值赠送
    amount DECIMAL(12,2) NOT NULL,        -- 金额（正/负）
    balance_after DECIMAL(12,2) NOT NULL, -- 交易后余额
    
    source VARCHAR(30),                   -- RECHARGE/ORDER/REFUND/WITHDRAW/ADMIN
    source_id VARCHAR(64),                -- 关联业务ID
    payment_channel VARCHAR(20),          -- WECHAT/ALIPAY（充值时）
    payment_trade_no VARCHAR(64),         -- 第三方交易号
    description VARCHAR(200),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT
);

CREATE INDEX idx_balance_trans_user ON balance_transaction(tenant_id, user_id, create_time);
CREATE INDEX idx_balance_trans_no ON balance_transaction(transaction_no);

COMMENT ON TABLE balance_transaction IS '余额流水表';
```

### 11. 用户车辆表 (user_vehicle)

```sql
CREATE TABLE user_vehicle (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    plate_number VARCHAR(20) NOT NULL,    -- 车牌号
    plate_color INTEGER,                  -- 1-蓝 2-绿 3-黄 4-白 5-黑
    vehicle_brand VARCHAR(50),            -- 品牌
    vehicle_model VARCHAR(50),            -- 车型
    vehicle_type INTEGER,                 -- 1-纯电动 2-插电混动 3-增程式
    vin VARCHAR(30),                      -- 车架号
    battery_capacity DECIMAL(8,2),        -- 电池容量(kWh)
    
    is_default INTEGER DEFAULT 0,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_user_vehicle_user ON user_vehicle(user_id) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_user_vehicle_plate ON user_vehicle(plate_number) WHERE deleted = 0;

COMMENT ON TABLE user_vehicle IS '用户车辆表';
```

### 12. 收藏站点表 (user_favorite_station)

```sql
CREATE TABLE user_favorite_station (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_favorite ON user_favorite_station(user_id, station_id);
CREATE INDEX idx_favorite_user ON user_favorite_station(user_id);

COMMENT ON TABLE user_favorite_station IS '用户收藏站点表';
```

### 13. 站内消息表 (user_message)

```sql
CREATE TABLE user_message (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    title VARCHAR(100),
    content TEXT,
    type INTEGER,                         -- 1-系统公告 2-订单通知 3-活动推送 4-客诉回复 5-积分变动
    
    is_read INTEGER DEFAULT 0,
    read_time TIMESTAMP,
    
    link_type VARCHAR(30),                -- ORDER/STATION/COUPON/COMPLAINT/URL
    link_id VARCHAR(64),
    link_url VARCHAR(500),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP                 -- 消息过期时间
);

CREATE INDEX idx_user_message ON user_message(user_id, is_read, create_time);
CREATE INDEX idx_user_message_type ON user_message(user_id, type);

COMMENT ON TABLE user_message IS '站内消息表';
```

### 14. 邀请记录表 (user_invitation)

```sql
CREATE TABLE user_invitation (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    inviter_id BIGINT NOT NULL,           -- 邀请人ID
    invitee_id BIGINT NOT NULL,           -- 被邀请人ID
    invite_code VARCHAR(20),              -- 邀请码
    
    -- 奖励信息
    inviter_reward_type INTEGER,          -- 1-积分 2-优惠券 3-余额
    inviter_reward_value DECIMAL(12,2),
    inviter_reward_status INTEGER DEFAULT 0, -- 0-待发放 1-已发放
    inviter_reward_time TIMESTAMP,
    
    invitee_reward_type INTEGER,
    invitee_reward_value DECIMAL(12,2),
    invitee_reward_status INTEGER DEFAULT 0,
    invitee_reward_time TIMESTAMP,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invitation_inviter ON user_invitation(inviter_id);
CREATE INDEX idx_invitation_invitee ON user_invitation(invitee_id);
CREATE UNIQUE INDEX uk_invitation ON user_invitation(invitee_id);

COMMENT ON TABLE user_invitation IS '用户邀请记录表';
```

### 15. 开票信息表 (user_invoice_info)

```sql
CREATE TABLE user_invoice_info (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    type INTEGER,                         -- 1-个人 2-企业
    title VARCHAR(100) NOT NULL,          -- 发票抬头
    tax_number VARCHAR(30),               -- 税号（企业必填）
    bank_name VARCHAR(100),               -- 开户行
    bank_account VARCHAR(50),             -- 银行账号
    address VARCHAR(200),                 -- 企业地址
    phone VARCHAR(20),                    -- 企业电话
    email VARCHAR(100),                   -- 接收邮箱
    
    is_default INTEGER DEFAULT 0,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_invoice_info_user ON user_invoice_info(user_id) WHERE deleted = 0;

COMMENT ON TABLE user_invoice_info IS '用户开票信息表';
```

### 16. 用户协议签署记录表 (user_agreement)

```sql
CREATE TABLE user_agreement (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    agreement_type VARCHAR(30) NOT NULL,  -- USER_AGREEMENT/PRIVACY_POLICY/RECHARGE_AGREEMENT
    agreement_version VARCHAR(20) NOT NULL,
    signed_time TIMESTAMP NOT NULL,
    ip VARCHAR(50),
    device_id VARCHAR(64),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_agreement ON user_agreement(user_id, agreement_type);

COMMENT ON TABLE user_agreement IS '用户协议签署记录表';
```

### 17. 签到记录表 (user_sign_in)

```sql
CREATE TABLE user_sign_in (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    sign_date DATE NOT NULL,              -- 签到日期
    continuous_days INTEGER DEFAULT 1,    -- 连续签到天数
    reward_points BIGINT,                 -- 奖励积分
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_sign_in ON user_sign_in(user_id, sign_date);
CREATE INDEX idx_sign_in_user ON user_sign_in(user_id, sign_date);

COMMENT ON TABLE user_sign_in IS '用户签到记录表';
```

### 18. 手机号换绑记录表 (phone_bindchange_log)

```sql
CREATE TABLE phone_bindchange_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    old_phone VARCHAR(20),                -- 原手机号（脱敏存储）
    new_phone VARCHAR(20) NOT NULL,       -- 新手机号（脱敏存储）
    verify_type VARCHAR(30) NOT NULL,     -- 验证方式: OLD_PHONE/WECHAT/ALIPAY/FACE
    
    ip VARCHAR(50),
    device_id VARCHAR(64),
    location VARCHAR(100),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_phone_change_user ON phone_bindchange_log(user_id, create_time);

COMMENT ON TABLE phone_bindchange_log IS '手机号换绑记录表';
```

### 18. 用户群组表 (user_group)

```sql
CREATE TABLE user_group (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    group_code VARCHAR(50) NOT NULL,          -- 群组编码
    group_name VARCHAR(100) NOT NULL,         -- 群组名称
    group_type INTEGER NOT NULL,              -- 1-企业客户 2-合作伙伴 3-特约用户 4-内部员工 5-自定义
    description VARCHAR(500),                 -- 描述
    
    -- 群组规则（自动加入条件，JSON格式）
    auto_join_rules TEXT,                     -- {"member_level_gte": 3, "total_charge_gte": 1000}
    
    -- 优先级（多群组时取最高优先级的权益）
    priority INTEGER DEFAULT 0,
    
    -- 有效期
    valid_start_time TIMESTAMP,
    valid_end_time TIMESTAMP,
    
    status INTEGER DEFAULT 1,                 -- 0-禁用 1-启用
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_user_group_code ON user_group(tenant_id, group_code) WHERE deleted = 0;
CREATE INDEX idx_user_group_tenant ON user_group(tenant_id, status, deleted);

COMMENT ON TABLE user_group IS '用户群组表';
```

### 19. 用户群组成员表 (user_group_member)

```sql
CREATE TABLE user_group_member (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    join_type INTEGER DEFAULT 1,              -- 1-手动添加 2-自动加入 3-用户申请
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP,                    -- 成员资格过期时间（NULL=永久）
    
    status INTEGER DEFAULT 1,                 -- 0-已移除 1-有效 2-已过期
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT
);

CREATE UNIQUE INDEX uk_group_member ON user_group_member(group_id, user_id) WHERE status = 1;
CREATE INDEX idx_group_member_user ON user_group_member(user_id, status);
CREATE INDEX idx_group_member_group ON user_group_member(group_id, status);

COMMENT ON TABLE user_group_member IS '用户群组成员表';
```

### 20. 群组权益表 (group_benefit)

```sql
CREATE TABLE group_benefit (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    
    benefit_type INTEGER NOT NULL,            -- 1-专属折扣 2-专属计费方案 3-专属优惠券 4-积分倍率 5-免服务费
    benefit_name VARCHAR(100),                -- 权益名称
    
    -- 折扣类权益
    discount_rate DECIMAL(5,2),               -- 折扣率（0.9=9折）
    max_discount DECIMAL(12,2),               -- 最大折扣金额
    
    -- 计费方案类权益
    billing_plan_id BIGINT,                   -- 专属计费方案ID
    
    -- 优惠券类权益
    coupon_template_id BIGINT,                -- 自动发放的优惠券模板
    coupon_issue_frequency VARCHAR(20),       -- 发放频率: ONCE/DAILY/WEEKLY/MONTHLY
    
    -- 积分倍率权益
    points_multiplier DECIMAL(3,1),           -- 积分倍率（1.5=1.5倍积分）
    
    -- 适用范围
    apply_scope INTEGER DEFAULT 1,            -- 1-全部站点 2-指定站点
    apply_station_ids TEXT,                   -- 适用站点ID列表
    
    -- 有效期
    valid_start_time TIMESTAMP,
    valid_end_time TIMESTAMP,
    
    status INTEGER DEFAULT 1,                 -- 0-禁用 1-启用
    priority INTEGER DEFAULT 0,               -- 同类型权益优先级
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_group_benefit ON group_benefit(group_id, status, deleted);
CREATE INDEX idx_group_benefit_type ON group_benefit(benefit_type, status);

COMMENT ON TABLE group_benefit IS '群组权益表';
```

### 21. 用户画像主表 (user_profile_data)

```sql
CREATE TABLE user_profile_data (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    
    -- 基础画像
    age_range VARCHAR(20),                    -- 18-25/26-35/36-45/46-55/55+
    city_code VARCHAR(20),                    -- 常驻城市
    
    -- 车辆画像
    primary_vehicle_brand VARCHAR(50),        -- 主要车辆品牌
    primary_vehicle_type INTEGER,             -- 1-纯电动 2-插混 3-增程
    primary_battery_capacity DECIMAL(8,2),    -- 主要车辆电池容量
    
    -- 充电行为画像
    charge_frequency INTEGER,                 -- 月均充电次数
    avg_charge_duration INTEGER,              -- 平均充电时长(分钟)
    prefer_charge_type INTEGER,               -- 1-快充为主 2-慢充为主 3-混合
    prefer_time_slot VARCHAR(20),             -- 偏好时段: MORNING/AFTERNOON/EVENING/NIGHT
    prefer_weekday INTEGER,                   -- 1-工作日为主 2-周末为主 3-均衡
    
    -- 消费画像
    total_amount DECIMAL(12,2) DEFAULT 0,     -- 累计消费
    avg_order_amount DECIMAL(10,2),           -- 客单价
    monthly_amount DECIMAL(10,2),             -- 月均消费
    prefer_payment VARCHAR(20),               -- 偏好支付: WECHAT/ALIPAY/BALANCE
    
    -- RFM 指标
    rfm_recency INTEGER,                      -- R: 最近一次充电距今天数
    rfm_frequency INTEGER,                    -- F: 近90天充电次数
    rfm_monetary DECIMAL(10,2),               -- M: 近90天消费金额
    rfm_score INTEGER,                        -- RFM 综合得分 (1-5)
    
    -- 价值分层
    value_level INTEGER DEFAULT 1,            -- 1-普通 2-潜力 3-高价值 4-超级VIP
    lifecycle_stage VARCHAR(20),              -- NEW/ACTIVE/SILENT/LOST/RECALLED
    
    -- 活跃度
    login_days_30 INTEGER DEFAULT 0,          -- 近30天登录天数
    charge_days_30 INTEGER DEFAULT 0,         -- 近30天充电天数
    last_active_date DATE,                    -- 最后活跃日期
    
    -- 风险指标
    complaint_rate DECIMAL(5,4),              -- 投诉率
    refund_rate DECIMAL(5,4),                 -- 退款率
    risk_level INTEGER DEFAULT 0,             -- 0-正常 1-关注 2-高风险
    
    -- 更新时间
    profile_updated_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_profile_user ON user_profile_data(user_id);
CREATE INDEX idx_user_profile_value ON user_profile_data(tenant_id, value_level);
CREATE INDEX idx_user_profile_lifecycle ON user_profile_data(tenant_id, lifecycle_stage);
CREATE INDEX idx_user_profile_rfm ON user_profile_data(tenant_id, rfm_score);

COMMENT ON TABLE user_profile_data IS '用户画像主表';
```

### 22. 用户标签定义表 (user_tag)

```sql
CREATE TABLE user_tag (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    tag_code VARCHAR(50) NOT NULL,            -- 标签编码
    tag_name VARCHAR(100) NOT NULL,           -- 标签名称
    tag_category VARCHAR(30) NOT NULL,        -- 类别: BEHAVIOR/PREFERENCE/VALUE/RISK/LIFECYCLE/CUSTOM
    tag_type INTEGER DEFAULT 1,               -- 1-系统标签 2-自定义标签
    
    -- 规则定义（系统标签使用）
    rule_expression TEXT,                     -- 规则表达式 JSON
    -- 例如: {"field": "monthly_amount", "operator": "gte", "value": 500}
    
    description VARCHAR(500),
    color VARCHAR(20),                        -- 标签颜色（展示用）
    priority INTEGER DEFAULT 0,
    
    status INTEGER DEFAULT 1,                 -- 0-禁用 1-启用
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_user_tag_code ON user_tag(tenant_id, tag_code) WHERE deleted = 0;
CREATE INDEX idx_user_tag_category ON user_tag(tenant_id, tag_category, status);

COMMENT ON TABLE user_tag IS '用户标签定义表';
```

### 23. 用户标签关联表 (user_tag_relation)

```sql
CREATE TABLE user_tag_relation (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    
    source INTEGER DEFAULT 1,                 -- 1-系统计算 2-手动打标
    expire_time TIMESTAMP,                    -- 标签过期时间（NULL=永久）
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT
);

CREATE UNIQUE INDEX uk_user_tag_rel ON user_tag_relation(user_id, tag_id);
CREATE INDEX idx_user_tag_rel_user ON user_tag_relation(user_id);
CREATE INDEX idx_user_tag_rel_tag ON user_tag_relation(tag_id);

COMMENT ON TABLE user_tag_relation IS '用户标签关联表';
```

### 24. 用户行为事件表 (user_behavior_event)

```sql
CREATE TABLE user_behavior_event (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    event_type VARCHAR(30) NOT NULL,          -- LOGIN/CHARGE_START/CHARGE_END/RECHARGE/COUPON_USE/COMPLAINT/...
    event_time TIMESTAMP NOT NULL,
    
    -- 事件属性（JSON）
    event_data JSONB,
    -- 例如: {"station_id": 123, "charger_type": "DC", "energy": 35.5, "amount": 45.00}
    
    device_type VARCHAR(20),
    ip VARCHAR(50),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 建议按月分区，此处为示例
CREATE INDEX idx_behavior_event_user ON user_behavior_event(user_id, event_time);
CREATE INDEX idx_behavior_event_type ON user_behavior_event(event_type, event_time);
CREATE INDEX idx_behavior_event_time ON user_behavior_event(event_time);

COMMENT ON TABLE user_behavior_event IS '用户行为事件表';
```

### 25. 用户画像快照表 (user_profile_snapshot)

```sql
CREATE TABLE user_profile_snapshot (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    snapshot_date DATE NOT NULL,
    profile_data JSONB NOT NULL,              -- 完整画像快照
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_profile_snapshot ON user_profile_snapshot(user_id, snapshot_date);
CREATE INDEX idx_profile_snapshot_date ON user_profile_snapshot(snapshot_date);

COMMENT ON TABLE user_profile_snapshot IS '用户画像历史快照表';
```

### 26. 营销活动表 (marketing_campaign)

```sql
CREATE TABLE marketing_campaign (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    campaign_code VARCHAR(50) NOT NULL,
    campaign_name VARCHAR(100) NOT NULL,
    campaign_type INTEGER NOT NULL,           -- 1-满减 2-折扣 3-免单 4-抽奖 5-秒杀 6-新人专享
    
    -- 时间
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    
    -- 预算控制
    total_budget DECIMAL(12,2),               -- 总预算（NULL=不限）
    used_budget DECIMAL(12,2) DEFAULT 0,      -- 已使用预算
    total_quota INTEGER,                      -- 总名额（NULL=不限）
    used_quota INTEGER DEFAULT 0,             -- 已使用名额
    per_user_limit INTEGER DEFAULT 1,         -- 每人限参与次数
    
    -- 规则配置（JSON）
    rules JSONB,
    -- 例如: {"min_amount": 50, "min_energy": 10, "user_tags": ["NEW_USER"], "time_slots": ["10:00-12:00"]}
    
    -- 奖励配置（JSON）
    rewards JSONB,
    -- 例如: {"type": "discount", "value": 0.8, "max_discount": 20} 或 {"type": "coupon", "template_id": 123}
    
    -- 适用范围
    apply_scope INTEGER DEFAULT 1,            -- 1-全部 2-指定站点 3-指定用户标签
    apply_station_ids TEXT,                   -- 适用站点ID列表
    apply_user_tags TEXT,                     -- 适用用户标签
    
    -- 状态
    status INTEGER DEFAULT 0,                 -- 0-草稿 1-待开始 2-进行中 3-已结束 4-已终止
    
    -- 效果统计（定期更新）
    participate_count INTEGER DEFAULT 0,      -- 参与人数
    order_count INTEGER DEFAULT 0,            -- 关联订单数
    total_discount DECIMAL(12,2) DEFAULT 0,   -- 总优惠金额
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted INTEGER DEFAULT 0,
    version INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_campaign_code ON marketing_campaign(tenant_id, campaign_code) WHERE deleted = 0;
CREATE INDEX idx_campaign_tenant ON marketing_campaign(tenant_id, status, deleted);
CREATE INDEX idx_campaign_time ON marketing_campaign(start_time, end_time) WHERE status IN (1, 2);

COMMENT ON TABLE marketing_campaign IS '营销活动表';
```

### 27. 活动参与记录表 (campaign_participation)

```sql
CREATE TABLE campaign_participation (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    
    participate_time TIMESTAMP NOT NULL,
    
    -- 奖励信息
    reward_type VARCHAR(20),                  -- DISCOUNT/COUPON/POINTS/FREE
    reward_value DECIMAL(12,2),               -- 奖励数值
    coupon_id BIGINT,                         -- 发放的优惠券ID
    
    -- 关联订单
    order_id BIGINT,
    order_amount DECIMAL(12,2),
    discount_amount DECIMAL(12,2),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_participation_campaign ON campaign_participation(campaign_id, participate_time);
CREATE INDEX idx_participation_user ON campaign_participation(user_id, participate_time);
CREATE INDEX idx_participation_order ON campaign_participation(order_id) WHERE order_id IS NOT NULL;

COMMENT ON TABLE campaign_participation IS '活动参与记录表';
```

### 28. 权益包表 (benefit_package)

```sql
CREATE TABLE benefit_package (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    package_code VARCHAR(50) NOT NULL,
    package_name VARCHAR(100) NOT NULL,
    package_type INTEGER NOT NULL,            -- 1-月卡 2-季卡 3-年卡 4-次卡
    
    -- 价格
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    
    -- 权益内容（JSON）
    benefits JSONB NOT NULL,
    -- 例如: {
    --   "free_service_fee": true,
    --   "discount_rate": 0.9,
    --   "monthly_coupons": [{"template_id": 1, "count": 2}],
    --   "points_multiplier": 1.5,
    --   "priority_charging": true
    -- }
    
    -- 有效期
    validity_days INTEGER,                    -- 有效天数
    validity_type INTEGER DEFAULT 1,          -- 1-购买后N天 2-自然月/季/年
    
    -- 限制
    purchase_limit INTEGER,                   -- 每人限购次数（NULL=不限）
    total_stock INTEGER,                      -- 总库存（NULL=不限）
    sold_count INTEGER DEFAULT 0,             -- 已售数量
    
    -- 适用范围
    apply_scope INTEGER DEFAULT 1,
    apply_station_ids TEXT,
    
    status INTEGER DEFAULT 1,                 -- 0-下架 1-上架
    sort_order INTEGER DEFAULT 0,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_package_code ON benefit_package(tenant_id, package_code) WHERE deleted = 0;
CREATE INDEX idx_package_tenant ON benefit_package(tenant_id, status, deleted);

COMMENT ON TABLE benefit_package IS '权益包/月卡表';
```

### 29. 用户权益包订阅表 (user_package_subscription)

```sql
CREATE TABLE user_package_subscription (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    
    -- 订单信息
    order_no VARCHAR(64),
    purchase_price DECIMAL(10,2),
    purchase_time TIMESTAMP NOT NULL,
    
    -- 有效期
    start_time TIMESTAMP NOT NULL,
    expire_time TIMESTAMP NOT NULL,
    
    -- 权益快照
    benefits_snapshot JSONB,
    
    -- 使用统计
    used_discount_amount DECIMAL(12,2) DEFAULT 0,
    used_coupon_count INTEGER DEFAULT 0,
    
    status INTEGER DEFAULT 1,                 -- 1-有效 2-已过期 3-已退款
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscription_user ON user_package_subscription(user_id, status);
CREATE INDEX idx_subscription_expire ON user_package_subscription(expire_time) WHERE status = 1;

COMMENT ON TABLE user_package_subscription IS '用户权益包订阅表';
```

### 30. 任务定义表 (user_task)

```sql
CREATE TABLE user_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    
    task_code VARCHAR(50) NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    task_desc VARCHAR(500),
    task_type INTEGER NOT NULL,               -- 1-每日任务 2-每周任务 3-成就任务 4-新手任务
    
    -- 完成条件
    condition_type VARCHAR(30) NOT NULL,      -- CHARGE_COUNT/CHARGE_AMOUNT/CHARGE_ENERGY/SIGN_IN/INVITE/BINDCAR/BINDOAUTH/...
    condition_value INTEGER NOT NULL,         -- 目标值
    condition_extra JSONB,                    -- 额外条件 {"charger_type": "DC", "station_ids": [...]}
    
    -- 奖励
    reward_type INTEGER NOT NULL,             -- 1-积分 2-优惠券 3-成长值 4-余额
    reward_value DECIMAL(10,2),
    reward_coupon_template_id BIGINT,
    
    -- 任务周期（每日/每周任务用）
    reset_cycle VARCHAR(10),                  -- DAILY/WEEKLY/NEVER
    
    -- 前置任务（新手任务用）
    prerequisite_task_id BIGINT,
    
    -- 展示
    icon_url VARCHAR(500),
    sort_order INTEGER DEFAULT 0,
    
    status INTEGER DEFAULT 1,                 -- 0-禁用 1-启用
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    deleted INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX uk_task_code ON user_task(tenant_id, task_code) WHERE deleted = 0;
CREATE INDEX idx_task_tenant ON user_task(tenant_id, task_type, status);

COMMENT ON TABLE user_task IS '任务定义表';
```

### 31. 用户任务进度表 (user_task_progress)

```sql
CREATE TABLE user_task_progress (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    
    -- 周期标识（每日/每周任务用）
    period_key VARCHAR(20),                   -- 如 2026-01-13 或 2026-W02
    
    -- 进度
    current_value INTEGER DEFAULT 0,
    target_value INTEGER NOT NULL,
    
    -- 状态
    status INTEGER DEFAULT 0,                 -- 0-进行中 1-已完成待领取 2-已领取
    complete_time TIMESTAMP,
    claim_time TIMESTAMP,
    
    -- 奖励
    reward_type INTEGER,
    reward_value DECIMAL(10,2),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_task_progress ON user_task_progress(user_id, task_id, period_key);
CREATE INDEX idx_task_progress_user ON user_task_progress(user_id, status);
CREATE INDEX idx_task_progress_task ON user_task_progress(task_id, period_key);

COMMENT ON TABLE user_task_progress IS '用户任务进度表';
```

### 32. 站点评价表 (station_review)

```sql
CREATE TABLE station_review (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    order_id BIGINT,
    
    -- 评分
    overall_score INTEGER NOT NULL,           -- 总体评分 1-5
    environment_score INTEGER,                -- 环境评分
    service_score INTEGER,                    -- 服务评分
    speed_score INTEGER,                      -- 充电速度评分
    
    -- 评论
    content TEXT,
    images TEXT,                              -- 图片URL列表
    
    -- 标签
    tags TEXT,                                -- 用户选择的标签 ["快充很快", "停车方便", ...]
    
    -- 状态
    status INTEGER DEFAULT 1,                 -- 0-隐藏 1-显示 2-置顶
    is_anonymous INTEGER DEFAULT 0,           -- 是否匿名
    
    -- 回复
    reply_content TEXT,
    reply_time TIMESTAMP,
    reply_by BIGINT,
    
    -- 统计
    like_count INTEGER DEFAULT 0,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0
);

CREATE INDEX idx_review_station ON station_review(station_id, status, create_time);
CREATE INDEX idx_review_user ON station_review(user_id);
CREATE UNIQUE INDEX uk_review_order ON station_review(order_id) WHERE order_id IS NOT NULL AND deleted = 0;

COMMENT ON TABLE station_review IS '站点评价表';
```

### 33. 修改 charging_order 表

```sql
-- 为 charging_order 添加用户相关字段（如果不存在）
DO $$
BEGIN
    -- 用户ID已存在，确保有索引
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_order_user') THEN
        CREATE INDEX idx_order_user ON charging_order(user_id) WHERE user_id IS NOT NULL;
    END IF;
END $$;
```

---

## API 设计

### 1. 用户相关接口

#### 1.1 用户创建方式

用户创建支持多种方式：

| 创建方式 | 触发场景 | 说明 |
|---------|---------|------|
| **手机号注册** | 用户主动在 APP/小程序注册 | 标准注册流程，手机号+验证码 |
| **扫码自动注册** | 用户扫描充电枪二维码时系统检测为新用户 | 静默创建临时账户 → 引导完善信息 |
| **第三方授权注册** | 微信/支付宝授权登录 | OAuth 授权后自动创建账户 |
| **管理员创建** | B 端运营人员手工创建 | 批量导入企业员工、VIP 客户等 |

#### 1.2 扫码注册流程

```
用户扫描充电枪二维码
        │
        ▼
  ┌─────────────────┐
  │ 解析二维码获取   │
  │ connectorId     │
  └────────┬────────┘
           │
           ▼
  ┌─────────────────┐     已登录
  │ 检查用户登录态   │─────────────────┐
  └────────┬────────┘                  │
           │ 未登录                     │
           ▼                           │
  ┌─────────────────┐                  │
  │ 获取手机号       │                  │
  │ (微信/支付宝授权)│                  │
  └────────┬────────┘                  │
           │                           │
           ▼                           │
  ┌─────────────────┐     存在         │
  │ 查询用户是否存在 │─────────────────┼───┐
  └────────┬────────┘                  │   │
           │ 不存在                     │   │
           ▼                           │   │
  ┌─────────────────┐                  │   │
  │ 创建临时用户     │                  │   │
  │ status=PENDING  │                  │   │
  └────────┬────────┘                  │   │
           │                           │   │
           ▼                           ▼   ▼
  ┌─────────────────┐         ┌─────────────────┐
  │ 返回授权页面     │         │ 进入充电流程     │
  │ 引导完善信息     │         │                 │
  └────────┬────────┘         └─────────────────┘
           │
           ▼
  ┌─────────────────┐
  │ 用户确认协议     │
  │ 完善必要信息     │
  └────────┬────────┘
           │
           ▼
  ┌─────────────────┐
  │ 激活账户        │
  │ status=ACTIVE   │
  └────────┬────────┘
           │
           ▼
      进入充电流程
```

#### 1.3 用户接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/users/register` | 用户注册（手机号+验证码） |
| POST | `/api/v1/users/register/scan` | 扫码触发注册（静默创建+引导完善） |
| POST | `/api/v1/users/login` | 用户登录 |
| POST | `/api/v1/users/login/oauth` | 第三方登录（微信/支付宝） |
| POST | `/api/v1/users/logout` | 退出登录 |
| GET | `/api/v1/users/profile` | 获取用户信息 |
| PUT | `/api/v1/users/profile` | 更新用户信息 |
| POST | `/api/v1/users/profile/complete` | 完善用户信息（扫码注册后） |
| POST | `/api/v1/users/phone/change` | 换绑手机号 |
| POST | `/api/v1/users/phone/change/verify` | 换绑手机号验证（原手机/OAuth/人脸） |
| POST | `/api/v1/users/bind-oauth` | 绑定第三方账号 |
| DELETE | `/api/v1/users/unbind-oauth/{type}` | 解绑第三方账号 |
| POST | `/api/v1/users/verify` | 实名认证 |
| POST | `/api/v1/users/destroy` | 账号注销申请 |
| GET | `/api/v1/users/devices` | 登录设备列表 |
| DELETE | `/api/v1/users/devices/{deviceId}` | 移除登录设备 |
| PUT | `/api/v1/users/devices/{deviceId}/trust` | 标记为信任设备 |

### 2. 钱包/余额接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/wallet/balance` | 查询余额 |
| POST | `/api/v1/wallet/recharge` | 余额充值 |
| GET | `/api/v1/wallet/transactions` | 余额流水列表 |
| POST | `/api/v1/wallet/withdraw` | 余额提现申请 |

### 3. 积分相关接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/points/balance` | 查询积分余额 |
| GET | `/api/v1/points/transactions` | 积分流水列表 |
| POST | `/api/v1/points/exchange` | 积分兑换 |
| POST | `/api/v1/points/sign-in` | 每日签到 |
| GET | `/api/v1/points/sign-in/status` | 签到状态（连续天数等） |

### 4. 优惠券相关接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/coupons/available` | 可领取优惠券列表 |
| POST | `/api/v1/coupons/{templateId}/claim` | 领取优惠券 |
| GET | `/api/v1/coupons/my` | 我的优惠券列表 |
| GET | `/api/v1/coupons/usable` | 可用于订单的优惠券 |

### 5. 客诉相关接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/complaints` | 提交客诉 |
| GET | `/api/v1/complaints` | 我的客诉列表 |
| GET | `/api/v1/complaints/{id}` | 客诉详情 |
| POST | `/api/v1/complaints/{id}/reply` | 用户回复 |
| POST | `/api/v1/complaints/{id}/evaluate` | 评价客诉处理 |

### 6. 车辆管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/vehicles` | 我的车辆列表 |
| POST | `/api/v1/vehicles` | 添加车辆 |
| PUT | `/api/v1/vehicles/{id}` | 更新车辆信息 |
| DELETE | `/api/v1/vehicles/{id}` | 删除车辆 |
| PUT | `/api/v1/vehicles/{id}/default` | 设为默认车辆 |

### 7. 收藏站点接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/favorites/stations` | 收藏站点列表 |
| POST | `/api/v1/favorites/stations/{stationId}` | 收藏站点 |
| DELETE | `/api/v1/favorites/stations/{stationId}` | 取消收藏 |

### 8. 消息通知接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/messages` | 消息列表 |
| GET | `/api/v1/messages/unread-count` | 未读消息数 |
| PUT | `/api/v1/messages/{id}/read` | 标记已读 |
| PUT | `/api/v1/messages/read-all` | 全部标记已读 |

### 9. 开票相关接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/invoice-info` | 开票信息列表 |
| POST | `/api/v1/invoice-info` | 添加开票信息 |
| PUT | `/api/v1/invoice-info/{id}` | 更新开票信息 |
| DELETE | `/api/v1/invoice-info/{id}` | 删除开票信息 |

### 10. 邀请相关接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/invitation/code` | 获取我的邀请码 |
| GET | `/api/v1/invitation/records` | 邀请记录列表 |
| GET | `/api/v1/invitation/stats` | 邀请统计（总人数、奖励等） |

### 11. 群组权益接口（C端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/groups/my` | 我所属的群组列表 |
| GET | `/api/v1/groups/my/benefits` | 我享有的群组权益 |
| GET | `/api/v1/groups/{groupId}/benefits` | 指定群组的权益详情 |

### 12. 用户画像接口（C端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/profile/summary` | 我的画像摘要（充电习惯、偏好等） |

### 13. 营销活动接口（C端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/campaigns/available` | 可参与的活动列表 |
| GET | `/api/v1/campaigns/{id}` | 活动详情 |
| POST | `/api/v1/campaigns/{id}/participate` | 参与活动 |
| GET | `/api/v1/campaigns/my` | 我参与的活动记录 |

### 14. 权益包接口（C端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/packages` | 权益包列表（月卡/季卡等） |
| GET | `/api/v1/packages/{id}` | 权益包详情 |
| POST | `/api/v1/packages/{id}/purchase` | 购买权益包 |
| GET | `/api/v1/packages/my` | 我的权益包订阅 |
| GET | `/api/v1/packages/my/benefits` | 当前生效的权益 |

### 15. 任务中心接口（C端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/tasks` | 任务列表（每日/每周/成就） |
| GET | `/api/v1/tasks/progress` | 我的任务进度 |
| POST | `/api/v1/tasks/{taskId}/claim` | 领取任务奖励 |

### 16. 站点评价接口（C端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/stations/{stationId}/reviews` | 站点评价列表 |
| POST | `/api/v1/stations/{stationId}/reviews` | 提交评价 |
| GET | `/api/v1/reviews/my` | 我的评价列表 |
| POST | `/api/v1/reviews/{id}/like` | 点赞评价 |

### 17. 管理端接口（B端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/users` | 用户列表（分页） |
| GET | `/api/v1/admin/users/{id}` | 用户详情 |
| POST | `/api/v1/admin/users` | 管理员创建用户 |
| POST | `/api/v1/admin/users/batch-import` | 批量导入用户（Excel） |
| PUT | `/api/v1/admin/users/{id}/status` | 修改用户状态 |
| POST | `/api/v1/admin/users/{id}/points/adjust` | 调整用户积分 |
| POST | `/api/v1/admin/users/{id}/balance/adjust` | 调整用户余额 |
| POST | `/api/v1/admin/coupon-templates` | 创建优惠券模板 |
| POST | `/api/v1/admin/coupons/batch-issue` | 批量发放优惠券 |
| GET | `/api/v1/admin/complaints` | 客诉工单列表 |
| PUT | `/api/v1/admin/complaints/{id}/assign` | 分配客诉处理人 |
| PUT | `/api/v1/admin/complaints/{id}/handle` | 处理客诉 |
| GET | `/api/v1/admin/invitations` | 邀请记录列表 |

### 13. 群组管理接口（B端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/groups` | 群组列表 |
| POST | `/api/v1/admin/groups` | 创建群组 |
| PUT | `/api/v1/admin/groups/{id}` | 更新群组 |
| DELETE | `/api/v1/admin/groups/{id}` | 删除群组 |
| GET | `/api/v1/admin/groups/{id}/members` | 群组成员列表 |
| POST | `/api/v1/admin/groups/{id}/members` | 批量添加成员 |
| DELETE | `/api/v1/admin/groups/{id}/members/{userId}` | 移除成员 |
| POST | `/api/v1/admin/groups/{id}/members/import` | 导入成员（Excel） |
| GET | `/api/v1/admin/groups/{id}/benefits` | 群组权益列表 |
| POST | `/api/v1/admin/groups/{id}/benefits` | 添加群组权益 |
| PUT | `/api/v1/admin/groups/{id}/benefits/{benefitId}` | 更新权益 |
| DELETE | `/api/v1/admin/groups/{id}/benefits/{benefitId}` | 删除权益 |

### 19. 用户画像管理接口（B端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/users/{id}/profile` | 用户完整画像 |
| GET | `/api/v1/admin/users/{id}/tags` | 用户标签列表 |
| POST | `/api/v1/admin/users/{id}/tags` | 手动打标签 |
| DELETE | `/api/v1/admin/users/{id}/tags/{tagId}` | 移除标签 |
| GET | `/api/v1/admin/users/{id}/behavior-events` | 用户行为事件列表 |
| GET | `/api/v1/admin/tags` | 标签列表 |
| POST | `/api/v1/admin/tags` | 创建自定义标签 |
| PUT | `/api/v1/admin/tags/{id}` | 更新标签 |
| DELETE | `/api/v1/admin/tags/{id}` | 删除标签 |
| GET | `/api/v1/admin/users/segment` | 用户分群查询（按标签/画像条件） |
| POST | `/api/v1/admin/users/segment/export` | 导出分群用户 |
| GET | `/api/v1/admin/profile/stats` | 画像统计（各维度分布） |
| GET | `/api/v1/admin/profile/value-distribution` | 用户价值分布 |
| GET | `/api/v1/admin/profile/lifecycle-distribution` | 生命周期分布 |

### 20. 营销活动管理接口（B端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/campaigns` | 活动列表 |
| POST | `/api/v1/admin/campaigns` | 创建活动 |
| GET | `/api/v1/admin/campaigns/{id}` | 活动详情 |
| PUT | `/api/v1/admin/campaigns/{id}` | 更新活动 |
| PUT | `/api/v1/admin/campaigns/{id}/status` | 更改活动状态（上线/终止） |
| GET | `/api/v1/admin/campaigns/{id}/participants` | 活动参与记录 |
| GET | `/api/v1/admin/campaigns/{id}/stats` | 活动效果统计 |
| POST | `/api/v1/admin/campaigns/{id}/copy` | 复制活动 |

### 21. 权益包管理接口（B端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/packages` | 权益包列表 |
| POST | `/api/v1/admin/packages` | 创建权益包 |
| PUT | `/api/v1/admin/packages/{id}` | 更新权益包 |
| PUT | `/api/v1/admin/packages/{id}/status` | 上架/下架 |
| GET | `/api/v1/admin/packages/{id}/subscriptions` | 订阅记录 |
| GET | `/api/v1/admin/packages/stats` | 权益包销售统计 |

### 22. 任务管理接口（B端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/tasks` | 任务列表 |
| POST | `/api/v1/admin/tasks` | 创建任务 |
| PUT | `/api/v1/admin/tasks/{id}` | 更新任务 |
| PUT | `/api/v1/admin/tasks/{id}/status` | 启用/禁用任务 |
| GET | `/api/v1/admin/tasks/{id}/stats` | 任务完成统计 |

### 23. 站点评价管理接口（B端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/reviews` | 评价列表 |
| PUT | `/api/v1/admin/reviews/{id}/status` | 显示/隐藏评价 |
| POST | `/api/v1/admin/reviews/{id}/reply` | 回复评价 |
| GET | `/api/v1/admin/reviews/stats` | 评价统计（站点评分排行等） |

---

## 模块结构

```
evcs-user/
├── build.gradle
├── Dockerfile
└── src/main/java/com/evcs/user/
    ├── UserApplication.java
    ├── controller/
    │   ├── UserController.java              # C端用户接口
    │   ├── WalletController.java            # 钱包/余额接口
    │   ├── PointsController.java            # 积分接口
    │   ├── CouponController.java            # 优惠券接口
    │   ├── ComplaintController.java         # 客诉接口
    │   ├── VehicleController.java           # 车辆管理接口
    │   ├── FavoriteController.java          # 收藏接口
    │   ├── MessageController.java           # 消息接口
    │   ├── InvitationController.java        # 邀请接口
    │   ├── InvoiceController.java           # 开票接口
    │   ├── CampaignController.java          # 营销活动接口
    │   ├── PackageController.java           # 权益包接口
    │   ├── TaskController.java              # 任务中心接口
    │   ├── ReviewController.java            # 站点评价接口
    │   └── admin/
    │       ├── AdminUserController.java     # 用户管理
    │       ├── AdminCouponController.java   # 优惠券管理
    │       ├── AdminComplaintController.java# 客诉管理
    │       ├── AdminInvitationController.java# 邀请管理
    │       ├── AdminCampaignController.java # 营销活动管理
    │       ├── AdminPackageController.java  # 权益包管理
    │       ├── AdminTaskController.java     # 任务管理
    │       └── AdminReviewController.java   # 评价管理
    ├── service/
    │   ├── UserService.java
    │   ├── WalletService.java
    │   ├── PointsService.java
    │   ├── CouponService.java
    │   ├── ComplaintService.java
    │   ├── VehicleService.java
    │   ├── FavoriteService.java
    │   ├── MessageService.java
    │   ├── InvitationService.java
    │   ├── SignInService.java
    │   ├── UserGroupService.java
    │   ├── GroupBenefitService.java
    │   ├── UserProfileService.java
    │   ├── UserTagService.java
    │   ├── BehaviorEventService.java
    │   ├── CampaignService.java
    │   ├── PackageService.java
    │   ├── TaskService.java
    │   ├── ReviewService.java
    │   └── impl/
    ├── domain/
    │   ├── entity/
    │   │   ├── ChargingUser.java
    │   │   ├── UserOauth.java
    │   │   ├── UserLoginLog.java
    │   │   ├── UserDevice.java
    │   │   ├── BalanceTransaction.java
    │   │   ├── PointsTransaction.java
    │   │   ├── CouponTemplate.java
    │   │   ├── UserCoupon.java
    │   │   ├── Complaint.java
    │   │   ├── ComplaintRecord.java
    │   │   ├── UserVehicle.java
    │   │   ├── UserFavoriteStation.java
    │   │   ├── UserMessage.java
    │   │   ├── UserInvitation.java
    │   │   ├── UserInvoiceInfo.java
    │   │   ├── UserAgreement.java
    │   │   ├── UserSignIn.java
    │   │   ├── UserGroup.java
    │   │   ├── UserGroupMember.java
    │   │   ├── GroupBenefit.java
    │   │   ├── UserProfileData.java
    │   │   ├── UserTag.java
    │   │   ├── UserTagRelation.java
    │   │   ├── UserBehaviorEvent.java
    │   │   ├── UserProfileSnapshot.java
    │   │   ├── MarketingCampaign.java
    │   │   ├── CampaignParticipation.java
    │   │   ├── BenefitPackage.java
    │   │   ├── UserPackageSubscription.java
    │   │   ├── UserTask.java
    │   │   ├── UserTaskProgress.java
    │   │   └── StationReview.java
    │   ├── dto/
    │   ├── vo/
    │   └── enums/
    ├── mapper/
    ├── config/
    ├── job/
    │   ├── CouponExpireJob.java             # 优惠券过期定时任务
    │   ├── PointsExpireJob.java             # 积分过期定时任务
    │   ├── GroupMemberExpireJob.java        # 群组成员过期定时任务
    │   ├── ProfileAggregationJob.java       # 画像聚合定时任务
    │   ├── RfmCalculationJob.java           # RFM计算定时任务
    │   ├── TagCalculationJob.java           # 系统标签计算任务
    │   ├── LifecycleUpdateJob.java          # 生命周期更新任务
    │   ├── ProfileSnapshotJob.java          # 画像快照任务
    │   ├── CampaignStatusJob.java           # 活动状态更新任务
    │   ├── PackageExpireJob.java            # 权益包过期任务
    │   └── TaskResetJob.java                # 每日/每周任务重置
    └── feign/
        └── OrderFeignClient.java            # 调用订单服务
```

---

## 跨服务集成

### 1. 订单服务集成

订单创建/完成时与用户模块交互：

```java
// evcs-order 调用 evcs-user
@FeignClient("evcs-user")
public interface UserFeignClient {
    
    // 验证并锁定优惠券
    @PostMapping("/api/v1/internal/coupons/{couponId}/lock")
    Result<CouponLockDTO> lockCoupon(@PathVariable Long couponId, 
                                      @RequestBody CouponLockRequest request);
    
    // 核销优惠券
    @PostMapping("/api/v1/internal/coupons/{couponId}/consume")
    Result<Void> consumeCoupon(@PathVariable Long couponId, 
                               @RequestBody CouponConsumeRequest request);
    
    // 释放优惠券（订单取消）
    @PostMapping("/api/v1/internal/coupons/{couponId}/release")
    Result<Void> releaseCoupon(@PathVariable Long couponId);
    
    // 增加积分
    @PostMapping("/api/v1/internal/points/add")
    Result<Void> addPoints(@RequestBody PointsAddRequest request);
    
    // 更新用户充电统计
    @PostMapping("/api/v1/internal/users/{userId}/charge-stats")
    Result<Void> updateChargeStats(@PathVariable Long userId, 
                                    @RequestBody ChargeStatsRequest request);
}
```

### 2. 事件驱动

使用 RabbitMQ 进行异步通知：

```
交换机: evcs.user.events
队列:
  - evcs.user.points.add      # 积分增加事件
  - evcs.user.coupon.expire   # 优惠券过期事件
  - evcs.user.level.upgrade   # 会员升级事件
  - evcs.user.group.join      # 用户加入群组事件
  - evcs.user.group.leave     # 用户离开群组事件
```

### 3. 订单服务获取用户群组权益

订单计费时需查询用户群组权益：

```java
// evcs-order 调用 evcs-user 获取用户权益
@FeignClient("evcs-user")
public interface UserFeignClient {
    
    // ... 其他方法 ...
    
    // 获取用户在指定站点的有效群组权益
    @GetMapping("/api/v1/internal/users/{userId}/group-benefits")
    Result<List<GroupBenefitDTO>> getUserGroupBenefits(
        @PathVariable Long userId,
        @RequestParam(required = false) Long stationId);
    
    // 获取用户专属计费方案（群组权益中的计费方案）
    @GetMapping("/api/v1/internal/users/{userId}/billing-plan")
    Result<BillingPlanDTO> getUserBillingPlan(
        @PathVariable Long userId,
        @RequestParam Long stationId);
}
```

---

## 实施计划

### Phase 1: 基础用户管理（2周）

- [ ] 创建 evcs-user 模块脚手架
- [ ] 实现 charging_user 表及 CRUD
- [ ] 用户注册/登录（手机号+验证码）
- [ ] 用户信息管理
- [ ] 登录日志记录
- [ ] 设备管理（登录设备列表、踢出设备）
- [ ] 用户协议签署记录
- [ ] 管理端用户列表

### Phase 2: 钱包与积分系统（2周）

- [ ] 余额充值（微信/支付宝）
- [ ] 余额流水记录
- [ ] 积分账户模型
- [ ] 积分流水记录
- [ ] 每日签到送积分
- [ ] 充电订单完成后积分发放
- [ ] 积分/余额查询接口

### Phase 3: 优惠券系统（2周）

- [ ] 优惠券模板管理
- [ ] 优惠券发放/领取
- [ ] 订单创建时优惠券选择
- [ ] 优惠券核销
- [ ] 过期优惠券自动处理

### Phase 4: 客诉系统（1周）

- [ ] 客诉工单创建
- [ ] 工单处理流程
- [ ] 客诉沟通记录
- [ ] 满意度评价

### Phase 5: 会员体系（1周）

- [ ] 会员等级规则
- [ ] 等级自动升降
- [ ] 等级权益配置

### Phase 6: 用户便捷功能（1周）

- [ ] 车辆管理（添加/编辑/删除）
- [ ] 收藏站点
- [ ] 站内消息通知
- [ ] 开票信息管理

### Phase 7: 运营增长功能（1周）

- [ ] 邀请码生成
- [ ] 邀请记录与奖励发放
- [ ] 用户标签体系（预留）
- [ ] 账号注销流程

### Phase 8: 用户群组与权益（2周）

- [ ] 用户群组管理（创建/编辑/删除）
- [ ] 群组成员管理（添加/移除/批量导入）
- [ ] 群组权益配置（折扣/专属计费/优惠券/积分倍率）
- [ ] 订单计费时权益应用
- [ ] 群组成员过期自动处理
- [ ] 自动加入规则（基于会员等级/消费金额）

### Phase 9: 用户画像与标签（4周）

#### 9.1 基础画像（1周）
- [ ] 用户画像主表设计与实现
- [ ] 手动打标签功能
- [ ] 用户画像查看接口
- [ ] 标签定义管理（系统标签+自定义标签）

#### 9.2 行为采集与聚合（1周）
- [ ] 行为事件采集（登录/充电/支付/投诉等）
- [ ] 基础指标聚合定时任务
- [ ] 消费/充电行为画像计算

#### 9.3 智能标签与分层（1周）
- [ ] RFM 模型计算
- [ ] 用户价值分层（普通/潜力/高价值/VIP）
- [ ] 生命周期阶段更新（新用户/活跃/沉默/流失/召回）
- [ ] 系统标签自动计算

#### 9.4 分群与应用（1周）
- [ ] 用户分群查询（按标签/画像条件组合）
- [ ] 分群用户导出
- [ ] 画像统计报表（各维度分布）
- [ ] 画像历史快照

### Phase 10: 营销活动中心（2周）

- [ ] 活动模板管理（满减/折扣/免单/新人专享）
- [ ] 活动规则配置（时间/预算/名额/用户条件）
- [ ] 活动上线/终止流程
- [ ] 活动参与记录
- [ ] 活动效果统计（参与率/转化率/ROI）
- [ ] 订单计费时活动应用

### Phase 11: 权益包/月卡（2周）

- [ ] 权益包管理（月卡/季卡/年卡/次卡）
- [ ] 权益内容配置（折扣/免服务费/优惠券/积分倍率）
- [ ] 权益包购买流程
- [ ] 订阅有效期管理
- [ ] 权益自动应用于订单
- [ ] 销售统计报表

### Phase 12: 任务中心（1周）

- [ ] 任务定义管理（每日/每周/成就/新手）
- [ ] 任务进度追踪
- [ ] 任务奖励发放
- [ ] 任务完成统计

### Phase 13: 站点评价（1周）

- [ ] 用户提交评价（评分+评论+图片）
- [ ] 评价展示与排序
- [ ] 评价回复功能
- [ ] 评价统计（站点评分排行）

---

## 技术考量

### 多租户隔离

- 所有表包含 `tenant_id` 字段
- 使用 MyBatis-Plus 租户插件自动过滤
- C 端用户可能被多个租户共享（取决于业务模式）

### 数据安全

- 手机号、身份证等敏感信息脱敏存储
- 日志中禁止打印敏感信息
- 接口返回时进行脱敏处理

### 账户安全与风险控制

#### 用户识别机制

采用 **OAuth 优先 + 手机号备用** 的多因子识别策略：

```
扫码进入
    │
    ▼
1️⃣ 先查 OAuth（微信OpenID/支付宝user_id）
    │
    ├── 命中 → 直接登录（即使手机号已换）
    │
    └── 未命中
            │
            ▼
2️⃣ 请求手机号授权
    │
    ├── 手机号存在 → 登录 + 自动绑定当前OAuth
    │
    └── 手机号不存在 → 创建新用户
```

#### 手机号销号风险防范

当运营商回收手机号并分配给新用户时，可能导致账户被盗用。防范措施：

| 措施 | 说明 |
|------|------|
| **OAuth 绑定校验** | 手机号匹配但 OAuth 不匹配时，触发二次验证 |
| **活跃度检测** | 账户超过 180 天未活跃 + 全新设备/IP → 触发额外验证 |
| **敏感操作二次验证** | 余额提现、换绑手机号、账户注销需 OAuth 确认或人脸验证 |
| **登录风险标记** | 记录可疑登录并通知用户 |

#### 风险登录判定规则

```java
public boolean isRiskyLogin(Long userId, LoginRequest request) {
    User user = userService.getById(userId);
    
    // 规则1：长期未活跃 + 全新设备
    if (daysSinceLastActive(user) > 180 && isNewDevice(request)) {
        return true;
    }
    
    // 规则2：手机号绑定很久 + 从未绑定OAuth + 异地登录
    if (daysSincePhoneBind(user) > 365 
        && !hasOAuthBinding(userId) 
        && isNewLocation(request)) {
        return true;
    }
    
    // 规则3：OAuth 与历史记录不匹配
    if (hasOAuthBinding(userId) && !matchesExistingOAuth(request)) {
        return true;
    }
    
    return false;
}
```

#### 换绑手机号流程

用户更换手机号时需通过身份验证：

```
用户发起换绑
    │
    ▼
验证身份（任选其一）
├── 原手机号验证码（如还能收到）
├── 已绑定的微信/支付宝确认
├── 人脸识别（如已实名）
    │
    ▼
输入新手机号 + 验证码
    │
    ▼
更新手机号，账户数据不变 ✅
记录换绑日志用于审计
```

#### 多设备登录策略

- 支持同一账户多设备同时登录（默认最多 5 台）
- 设备 Token 独立管理，互不影响
- 用户可在"我的设备"中查看并踢出可疑设备
- 设备超过 90 天未活跃自动标记为失效

### 性能优化

- 用户信息缓存（Redis）
- 优惠券可用数量使用 Redis 原子操作
- 积分余额使用乐观锁

### 扩展性

- 用户标签系统（已实现）
- 用户画像分析（已实现）
- 营销活动中心（已实现）
- 权益包/月卡（已实现）
- 任务中心（已实现）
- 站点评价（已实现）
- 预留个性化推荐接口
- 预留自动化营销旅程

---

## 相关文档

- [PROJECT-CODING-STANDARDS.md](../overview/PROJECT-CODING-STANDARDS.md) - 编码规范
- [TENANT-CONTEXT-ASYNC-RFC.md](./TENANT-CONTEXT-ASYNC-RFC.md) - 多租户上下文传播
- [evcs_order_tables.sql](../../sql/evcs_order_tables.sql) - 订单表结构

---

## 变更历史

| 日期 | 版本 | 变更说明 |
|------|------|----------|
| 2026-01-13 | v1.6 | 新增账户安全与风险控制：用户识别机制、手机号销号防范、换绑手机号流程、多设备登录策略、风险登录检测 |
| 2026-01-13 | v1.5 | 完善用户创建方式：支持扫码自动注册、管理员创建、批量导入；新增扫码注册流程图 |
| 2026-01-13 | v1.4 | 新增用户运营最佳实践：营销活动中心、权益包/月卡、任务中心、站点评价 |
| 2026-01-13 | v1.3 | 新增用户画像与标签功能：画像数据表、标签体系、行为事件采集、RFM模型、用户分群、画像统计 |
| 2026-01-13 | v1.2 | 新增用户群组与权益功能：群组管理、成员管理、群组权益（折扣/专属计费/优惠券/积分倍率） |
| 2026-01-13 | v1.1 | 补充扩展特性：登录日志、设备管理、余额充值、车辆管理、收藏站点、站内消息、邀请裂变、开票信息、签到、用户协议等 |
| 2026-01-13 | v1.0 | 初始版本 |
