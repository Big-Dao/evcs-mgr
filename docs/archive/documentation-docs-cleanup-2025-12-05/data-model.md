# EVCS Manager 鏁版嵁妯″瀷璁捐
 
> **鐗堟湰**: v2.1 | **鏈€鍚庢洿鏂?*: 2025-11-10 | **缁存姢鑰?*: 鏁版嵁鏋舵瀯甯?| **鐘舵€?*: 娲昏穬
>
> 馃梽锔?**鐢ㄩ€?*: 鎻忚堪鏍稿績涓氬姟琛ㄧ粨鏋勩€佺害鏉熶笌澶氱鎴风瓥鐣?
## 馃搵 姒傝堪

鏈枃妗ｅ畾涔変簡 EVCS Manager 鍏呯數绔欑鐞嗗钩鍙扮殑瀹屾暣鏁版嵁妯″瀷璁捐锛屽寘鎷暟鎹簱琛ㄧ粨鏋勩€佸疄浣撳叧绯汇€佹暟鎹害鏉熺瓑銆?
### 馃幆 璁捐鐩爣
- **澶氱鎴锋敮鎸?*: 鏀寔绉熸埛鏁版嵁瀹屽叏闅旂
- **鏁版嵁涓€鑷存€?*: 纭繚鏁版嵁鐨勫畬鏁存€у拰涓€鑷存€?- **鎬ц兘浼樺寲**: 鍚堢悊鐨勭储寮曡璁★紝浼樺寲鏌ヨ鎬ц兘
- **鎵╁睍鎬?*: 鏀寔涓氬姟鍔熻兘鎵╁睍
- **鏁版嵁瀹夊叏**: 鏁忔劅鏁版嵁鍔犲瘑瀛樺偍

## 馃梽锔?鏁版嵁搴撹璁?
### 璁捐鍘熷垯
1. **鎵€鏈変笟鍔¤〃鍖呭惈绉熸埛瀛楁**: `tenant_id`
2. **缁熶竴鐨勪富閿瓥鐣?*: 浣跨敤鑷涓婚敭
3. **杞垹闄ゆ満鍒?*: 浣跨敤 `deleted` 瀛楁鏍囪鍒犻櫎鐘舵€?4. **瀹¤瀛楁**: 鍖呭惈鍒涘缓浜恒€佸垱寤烘椂闂淬€佹洿鏂颁汉銆佹洿鏂版椂闂?5. **鍛藉悕瑙勮寖**: 浣跨敤灏忓啓瀛楁瘝鍜屼笅鍒掔嚎

### 鏍稿績琛ㄨ璁?
#### 1. 绉熸埛绠＄悊

##### 绉熸埛琛?(sys_tenant)
```sql
CREATE TABLE sys_tenant (
    tenant_id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(32) NOT NULL UNIQUE,
    tenant_name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    tenant_type INTEGER NOT NULL DEFAULT 3 COMMENT '绉熸埛绫诲瀷锛?-骞冲彴鏂癸紝2-杩愯惀鍟嗭紝3-鍚堜綔浼欎即',
    contact_name VARCHAR(50) COMMENT '鑱旂郴浜哄鍚?,
    contact_phone VARCHAR(20) COMMENT '鑱旂郴鐢佃瘽',
    contact_email VARCHAR(100) COMMENT '鑱旂郴閭',
    status INTEGER NOT NULL DEFAULT 1 COMMENT '鐘舵€侊細1-鍚敤锛?-绂佺敤',
    ancestors VARCHAR(500) COMMENT '绁栫骇璺緞锛屽锛?100,101,',
    level INTEGER NOT NULL DEFAULT 1 COMMENT '灞傜骇娣卞害',
    sort_order INTEGER DEFAULT 0 COMMENT '鎺掑簭',
    remark TEXT COMMENT '澶囨敞',
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0 COMMENT '鍒犻櫎鏍囪瘑锛?-姝ｅ父锛?-鍒犻櫎',

    CONSTRAINT uk_tenant_code UNIQUE (tenant_code),
    CONSTRAINT fk_tenant_parent FOREIGN KEY (parent_id) REFERENCES sys_tenant(tenant_id)
);

-- 绱㈠紩
CREATE INDEX idx_tenant_parent_id ON sys_tenant(parent_id, deleted);
CREATE INDEX idx_tenant_type_status ON sys_tenant(tenant_type, status, deleted);
CREATE INDEX idx_tenant_code ON sys_tenant(tenant_code, deleted);
```

> **TenantID 鐢熸垚涓庝娇鐢ㄨ鍒?*
> - `tenant_id` 鐢辨暟鎹簱 `BIGSERIAL` 鑷姩鐢熸垚锛岀姝㈡墜宸ユ寚瀹氭垨鏇存敼銆?> - 绠＄悊绔粎鍏佽閰嶇疆 `tenant_name` 涓?`tenant_code`锛堢郴缁熺敓鎴愬彲璇荤紪鐮侊級锛屼笉鍙洿鎺ョ紪杈?`tenant_id`銆?> - 鎵€鏈夊井鏈嶅姟鍐呴儴銆佹棩蹇椼€佸璁′笌涓婁笅鏂囦紶閫掔粺涓€浣跨敤 `tenant_id` 浣滀负鍞竴涓婚敭锛沗tenant_code` 浠呯敤浜庡睍绀烘垨浜哄伐璇嗗埆銆?
##### 鐢ㄦ埛琛?(sys_user)
```sql
CREATE TABLE sys_user (
    user_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt鍔犲瘑',
    real_name VARCHAR(50) COMMENT '鐪熷疄濮撳悕',
    email VARCHAR(100) COMMENT '閭',
    phone VARCHAR(20) COMMENT '鎵嬫満鍙?,
    avatar VARCHAR(255) COMMENT '澶村儚URL',
    status INTEGER NOT NULL DEFAULT 1 COMMENT '鐘舵€侊細1-姝ｅ父锛?-绂佺敤',
    last_login_time TIMESTAMP COMMENT '鏈€鍚庣櫥褰曟椂闂?,
    last_login_ip VARCHAR(50) COMMENT '鏈€鍚庣櫥褰旾P',
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    CONSTRAINT uk_user_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id)
);

-- 绱㈠紩
CREATE INDEX idx_user_tenant_id ON sys_user(tenant_id, deleted);
CREATE INDEX idx_user_username ON sys_user(username, deleted);
CREATE INDEX idx_user_email ON sys_user(email, deleted);
CREATE INDEX idx_user_phone ON sys_user(phone, deleted);
```

##### 瑙掕壊琛?(sys_role)
```sql
CREATE TABLE sys_role (
    role_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) COMMENT '瑙掕壊鎻忚堪',
    status INTEGER NOT NULL DEFAULT 1,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    CONSTRAINT uk_role_tenant_code UNIQUE (tenant_id, role_code),
    CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id)
);

-- 绱㈠紩
CREATE INDEX idx_role_tenant_id ON sys_role(tenant_id, deleted);
CREATE INDEX idx_role_code ON sys_role(role_code, deleted);
```

##### 鐢ㄦ埛瑙掕壊鍏宠仈琛?(sys_user_role)
```sql
CREATE TABLE sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(role_id)
);

-- 绱㈠紩
CREATE INDEX idx_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX idx_user_role_role_id ON sys_user_role(role_id);
```

#### 2. 鍏呯數绔欑鐞?
##### 鍏呯數绔欒〃 (charging_station)
```sql
CREATE TABLE charging_station (
    station_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_code VARCHAR(64) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    address VARCHAR(200) COMMENT '璇︾粏鍦板潃',
    province VARCHAR(50) COMMENT '鐪佷唤',
    city VARCHAR(50) COMMENT '鍩庡競',
    district VARCHAR(50) COMMENT '鍖哄幙',
    latitude DECIMAL(10, 8) COMMENT '绾害',
    longitude DECIMAL(11, 8) COMMENT '缁忓害',
    operator_name VARCHAR(100) COMMENT '杩愯惀鍟嗗悕绉?,
    service_phone VARCHAR(20) COMMENT '鏈嶅姟鐢佃瘽',
    construction_type INTEGER COMMENT '寤鸿绫诲瀷锛?-鍏叡锛?-涓撶敤锛?-绉佷汉',
    station_type INTEGER COMMENT '绔欑偣绫诲瀷锛?-鐩存祦绔欙紝2-浜ゆ祦绔欙紝3-浜ょ洿娴佷竴浣撶珯',
    park_count INTEGER DEFAULT 0 COMMENT '鍋滆溅浣嶆暟閲?,
    station_pic VARCHAR(500) COMMENT '绔欑偣鍥剧墖',
    equipment_info TEXT COMMENT '璁惧淇℃伅',
    status INTEGER NOT NULL DEFAULT 1 COMMENT '鐘舵€侊細1-鍚敤锛?-鍋滅敤',
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    CONSTRAINT uk_station_tenant_code UNIQUE (tenant_id, station_code),
    CONSTRAINT fk_station_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id)
);

-- 绱㈠紩
CREATE INDEX idx_station_tenant_id ON charging_station(tenant_id, deleted);
CREATE INDEX idx_station_code ON charging_station(station_code, deleted);
CREATE INDEX idx_station_status ON charging_station(status, deleted);
CREATE INDEX idx_station_location ON charging_station(latitude, longitude, deleted);
CREATE INDEX idx_station_city ON charging_station(city, deleted);
```

##### 鍏呯數妗╄〃 (charger)
```sql
CREATE TABLE charger (
    charger_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    charger_code VARCHAR(64) NOT NULL,
    charger_name VARCHAR(100) COMMENT '鍏呯數妗╁悕绉?,
    manufacturer VARCHAR(100) COMMENT '璁惧鍘傚晢',
    model VARCHAR(100) COMMENT '璁惧鍨嬪彿',
    charger_type INTEGER NOT NULL COMMENT '鍏呯數妗╃被鍨嬶細1-鐩存祦锛?-浜ゆ祦锛?-浜ょ洿娴佷竴浣?,
    power_rate DECIMAL(8, 2) COMMENT '棰濆畾鍔熺巼(kW)',
    voltage_level INTEGER COMMENT '鐢靛帇绛夌骇(V)',
    current_rate DECIMAL(8, 2) COMMENT '棰濆畾鐢垫祦(A)',
    protocol_type VARCHAR(20) DEFAULT 'OCPP' COMMENT '鍗忚绫诲瀷',
    firmware_version VARCHAR(50) COMMENT '鍥轰欢鐗堟湰',
    install_date DATE COMMENT '瀹夎鏃ユ湡',
    last_maintenance_date DATE COMMENT '鏈€鍚庣淮鎶ゆ棩鏈?,
    status INTEGER NOT NULL DEFAULT 1 COMMENT '鐘舵€侊細0-绂荤嚎锛?-绌洪棽锛?-鍏呯數涓紝3-鏁呴殰',
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    CONSTRAINT uk_charger_tenant_code UNIQUE (tenant_id, charger_code),
    CONSTRAINT uk_charger_station_code UNIQUE (station_id, charger_code),
    CONSTRAINT fk_charger_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id),
    CONSTRAINT fk_charger_station FOREIGN KEY (station_id) REFERENCES charging_station(station_id)
);

-- 绱㈠紩
CREATE INDEX idx_charger_tenant_id ON charger(tenant_id, deleted);
CREATE INDEX idx_charger_station_id ON charger(station_id, deleted);
CREATE INDEX idx_charger_code ON charger(charger_code, deleted);
CREATE INDEX idx_charger_status ON charger(status, deleted);
CREATE INDEX idx_charger_type ON charger(charger_type, deleted);
```

#### 3. 璁㈠崟绠＄悊

##### 璁¤垂鏂规琛?(billing_plan)
```sql
CREATE TABLE billing_plan (
    plan_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    plan_type INTEGER NOT NULL DEFAULT 1 COMMENT '鏂规绫诲瀷锛?-鏍囧噯璁¤垂锛?-鍒嗘椂璁¤垂锛?-闃舵璁¤垂',
    description VARCHAR(255) COMMENT '鏂规鎻忚堪',
    is_default INTEGER DEFAULT 0 COMMENT '鏄惁榛樿鏂规锛?-鏄紝0-鍚?,
    status INTEGER NOT NULL DEFAULT 1 COMMENT '鐘舵€侊細1-鍚敤锛?-绂佺敤',
    effective_time TIMESTAMP COMMENT '鐢熸晥鏃堕棿',
    expire_time TIMESTAMP COMMENT '澶辨晥鏃堕棿',
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    CONSTRAINT fk_plan_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id)
);

-- 绱㈠紩
CREATE INDEX idx_plan_tenant_id ON billing_plan(tenant_id, deleted);
CREATE INDEX idx_plan_status ON billing_plan(status, deleted);
CREATE INDEX idx_plan_default ON billing_plan(is_default, deleted);
```

##### 璁¤垂鏂规娈佃〃 (billing_plan_segment)
```sql
CREATE TABLE billing_plan_segment (
    segment_id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    segment_name VARCHAR(100) NOT NULL,
    start_time TIME COMMENT '寮€濮嬫椂闂?鍒嗘椂璁¤垂)',
    end_time TIME COMMENT '缁撴潫鏃堕棿(鍒嗘椂璁¤垂)',
    min_energy DECIMAL(10, 2) COMMENT '鏈€灏忕數閲?kWh锛岄樁姊璐?',
    max_energy DECIMAL(10, 2) COMMENT '鏈€澶х數閲?kWh锛岄樁姊璐?',
    energy_price DECIMAL(8, 4) NOT NULL COMMENT '鐢佃垂鍗曚环(鍏?kWh)',
    service_price DECIMAL(8, 4) NOT NULL COMMENT '鏈嶅姟璐瑰崟浠?鍏?kWh)',
    parking_price DECIMAL(8, 4) DEFAULT 0 COMMENT '鍋滆溅璐瑰崟浠?鍏?鍒嗛挓)',
    sort_order INTEGER DEFAULT 0 COMMENT '鎺掑簭',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_segment_plan FOREIGN KEY (plan_id) REFERENCES billing_plan(plan_id)
);

-- 绱㈠紩
CREATE INDEX idx_segment_plan_id ON billing_plan_segment(plan_id);
CREATE INDEX idx_segment_sort ON billing_plan_segment(plan_id, sort_order);
```

##### 鍏呯數璁㈠崟琛?(charging_order)
```sql
CREATE TABLE charging_order (
    order_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    charger_id BIGINT NOT NULL,
    session_id VARCHAR(64) COMMENT '鍏呯數浼氳瘽ID',
    billing_plan_id BIGINT COMMENT '璁¤垂鏂规ID',

    -- 鏃堕棿淇℃伅
    start_time TIMESTAMP COMMENT '寮€濮嬪厖鐢垫椂闂?,
    end_time TIMESTAMP COMMENT '缁撴潫鍏呯數鏃堕棿',
    duration INTEGER COMMENT '鍏呯數鏃堕暱(绉?',

    -- 鐢甸噺淇℃伅
    start_energy DECIMAL(10, 2) COMMENT '寮€濮嬬數琛ㄨ鏁?kWh)',
    end_energy DECIMAL(10, 2) COMMENT '缁撴潫鐢佃〃璇绘暟(kWh)',
    total_energy DECIMAL(10, 2) COMMENT '鎬诲厖鐢甸噺(kWh)',

    -- 閲戦淇℃伅
    energy_amount DECIMAL(10, 2) DEFAULT 0 COMMENT '鐢佃垂閲戦(鍏?',
    service_amount DECIMAL(10, 2) DEFAULT 0 COMMENT '鏈嶅姟璐归噾棰?鍏?',
    parking_amount DECIMAL(10, 2) DEFAULT 0 COMMENT '鍋滆溅璐归噾棰?鍏?',
    total_amount DECIMAL(10, 2) DEFAULT 0 COMMENT '鎬婚噾棰?鍏?',
    discount_amount DECIMAL(10, 2) DEFAULT 0 COMMENT '浼樻儬閲戦(鍏?',
    actual_amount DECIMAL(10, 2) DEFAULT 0 COMMENT '瀹炰粯閲戦(鍏?',

    -- 鐘舵€佷俊鎭?    status INTEGER NOT NULL DEFAULT 1 COMMENT '璁㈠崟鐘舵€侊細1-鍏呯數涓紝2-宸插畬鎴愶紝3-宸插彇娑堬紝4-寰呮敮浠橈紝5-宸叉敮浠橈紝6-宸查€€娆?,
    payment_status INTEGER DEFAULT 0 COMMENT '鏀粯鐘舵€侊細0-鏈敮浠橈紝1-鏀粯涓紝2-宸叉敮浠橈紝3-閫€娆句腑锛?-宸查€€娆?,
    remark VARCHAR(255) COMMENT '澶囨敞',

    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    CONSTRAINT fk_order_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    CONSTRAINT fk_order_station FOREIGN KEY (station_id) REFERENCES charging_station(station_id),
    CONSTRAINT fk_order_charger FOREIGN KEY (charger_id) REFERENCES charger(charger_id),
    CONSTRAINT fk_order_plan FOREIGN KEY (billing_plan_id) REFERENCES billing_plan(plan_id)
);

-- 绱㈠紩
CREATE INDEX idx_order_tenant_id ON charging_order(tenant_id, deleted);
CREATE INDEX idx_order_user_id ON charging_order(user_id, deleted);
CREATE INDEX idx_order_station_id ON charging_order(station_id, deleted);
CREATE INDEX idx_order_charger_id ON charging_order(charger_id, deleted);
CREATE INDEX idx_order_no ON charging_order(order_no, deleted);
CREATE INDEX idx_order_status ON charging_order(status, deleted);
CREATE INDEX idx_order_create_time ON charging_order(create_time, deleted);
CREATE INDEX idx_order_session_id ON charging_order(session_id, deleted);
```

#### 4. 鏀粯绠＄悊

##### 鏀粯璁㈠崟琛?(payment_order)
```sql
CREATE TABLE payment_order (
    payment_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    payment_no VARCHAR(64) NOT NULL UNIQUE,
    third_party_trade_no VARCHAR(64) COMMENT '绗笁鏂逛氦鏄撳彿',
    payment_method VARCHAR(20) NOT NULL COMMENT '鏀粯鏂瑰紡锛歛lipay,wechat,unionpay',
    payment_channel VARCHAR(50) COMMENT '鏀粯娓犻亾',
    amount DECIMAL(10, 2) NOT NULL COMMENT '鏀粯閲戦(鍏?',
    currency VARCHAR(3) DEFAULT 'CNY' COMMENT '璐у竵绫诲瀷',
    subject VARCHAR(255) COMMENT '鏀粯鏍囬',
    body VARCHAR(500) COMMENT '鏀粯鎻忚堪',
    return_url VARCHAR(255) COMMENT '鍚屾鍥炶皟URL',
    notify_url VARCHAR(255) COMMENT '寮傛鍥炶皟URL',

    -- 鏃堕棿淇℃伅
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    pay_time TIMESTAMP COMMENT '鏀粯鏃堕棿',
    expire_time TIMESTAMP COMMENT '杩囨湡鏃堕棿',

    -- 鐘舵€佷俊鎭?    status INTEGER NOT NULL DEFAULT 1 COMMENT '鏀粯鐘舵€侊細1-寰呮敮浠橈紝2-鏀粯涓紝3-宸叉敮浠橈紝4-鏀粯澶辫触锛?-宸插叧闂紝6-宸查€€娆?,
    error_code VARCHAR(50) COMMENT '閿欒鐮?,
    error_message VARCHAR(255) COMMENT '閿欒淇℃伅',

    -- 鎵╁睍淇℃伅
    extra_data TEXT COMMENT '鎵╁睍鏁版嵁(JSON鏍煎紡)',

    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES charging_order(order_id),
    CONSTRAINT fk_payment_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id)
);

-- 绱㈠紩
CREATE INDEX idx_payment_tenant_id ON payment_order(tenant_id, deleted);
CREATE INDEX idx_payment_order_id ON payment_order(order_id, deleted);
CREATE INDEX idx_payment_no ON payment_order(payment_no, deleted);
CREATE INDEX idx_payment_third_no ON payment_order(third_party_trade_no, deleted);
CREATE INDEX idx_payment_status ON payment_order(status, deleted);
CREATE INDEX idx_payment_create_time ON payment_order(create_time, deleted);
```

##### 閫€娆捐褰曡〃 (refund_record)
```sql
CREATE TABLE refund_record (
    refund_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    third_party_refund_no VARCHAR(64) COMMENT '绗笁鏂归€€娆惧彿',
    refund_amount DECIMAL(10, 2) NOT NULL COMMENT '閫€娆鹃噾棰?鍏?',
    refund_reason VARCHAR(255) COMMENT '閫€娆惧師鍥?,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    refund_time TIMESTAMP COMMENT '閫€娆炬椂闂?,
    status INTEGER NOT NULL DEFAULT 1 COMMENT '閫€娆剧姸鎬侊細1-閫€娆句腑锛?-閫€娆炬垚鍔燂紝3-閫€娆惧け璐?,
    error_code VARCHAR(50) COMMENT '閿欒鐮?,
    error_message VARCHAR(255) COMMENT '閿欒淇℃伅',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,

    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payment_order(payment_id),
    CONSTRAINT fk_refund_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id)
);

-- 绱㈠紩
CREATE INDEX idx_refund_tenant_id ON refund_record(tenant_id, deleted);
CREATE INDEX idx_refund_payment_id ON refund_record(payment_id, deleted);
CREATE INDEX idx_refund_no ON refund_record(refund_no, deleted);
CREATE INDEX idx_refund_status ON refund_record(status, deleted);
```

## 馃敆 瀹炰綋鍏崇郴鍥?
```mermaid
erDiagram
    sys_tenant ||--o{ sys_user : "鎷ユ湁"
    sys_tenant ||--o{ sys_role : "鎷ユ湁"
    sys_user ||--o{ sys_user_role : "鎷ユ湁"
    sys_role ||--o{ sys_user_role : "鎷ユ湁"

    sys_tenant ||--o{ charging_station : "鎷ユ湁"
    charging_station ||--o{ charger : "鍖呭惈"

    sys_tenant ||--o{ billing_plan : "鎷ユ湁"
    billing_plan ||--o{ billing_plan_segment : "鍖呭惈"

    sys_user ||--o{ charging_order : "鍒涘缓"
    charging_station ||--o{ charging_order : "浜х敓"
    charger ||--o{ charging_order : "浜х敓"
    billing_plan ||--o{ charging_order : "浣跨敤"

    charging_order ||--o{ payment_order : "鏀粯"
    payment_order ||--o{ refund_record : "閫€娆?

    sys_tenant {
        bigint tenant_id PK
        varchar tenant_code UK
        varchar tenant_name
        bigint parent_id FK
        integer tenant_type
        varchar contact_name
        varchar contact_phone
        varchar contact_email
        integer status
        varchar ancestors
        integer level
        integer sort_order
        text remark
        bigint create_by
        timestamp create_time
        bigint update_by
        timestamp update_time
        integer deleted
    }

    sys_user {
        bigint user_id PK
        bigint tenant_id FK
        varchar username
        varchar password
        varchar real_name
        varchar email
        varchar phone
        varchar avatar
        integer status
        timestamp last_login_time
        varchar last_login_ip
        bigint create_by
        timestamp create_time
        bigint update_by
        timestamp update_time
        integer deleted
    }

    charging_station {
        bigint station_id PK
        bigint tenant_id FK
        varchar station_code
        varchar station_name
        varchar address
        varchar province
        varchar city
        varchar district
        decimal latitude
        decimal longitude
        varchar operator_name
        varchar service_phone
        integer construction_type
        integer station_type
        integer park_count
        varchar station_pic
        text equipment_info
        integer status
        bigint create_by
        timestamp create_time
        bigint update_by
        timestamp update_time
        integer deleted
    }

    charger {
        bigint charger_id PK
        bigint tenant_id FK
        bigint station_id FK
        varchar charger_code
        varchar charger_name
        varchar manufacturer
        varchar model
        integer charger_type
        decimal power_rate
        integer voltage_level
        decimal current_rate
        varchar protocol_type
        varchar firmware_version
        date install_date
        date last_maintenance_date
        integer status
        bigint create_by
        timestamp create_time
        bigint update_by
        timestamp update_time
        integer deleted
    }

    charging_order {
        bigint order_id PK
        bigint tenant_id FK
        varchar order_no UK
        bigint user_id FK
        bigint station_id FK
        bigint charger_id FK
        varchar session_id
        bigint billing_plan_id FK
        timestamp start_time
        timestamp end_time
        integer duration
        decimal start_energy
        decimal end_energy
        decimal total_energy
        decimal energy_amount
        decimal service_amount
        decimal parking_amount
        decimal total_amount
        decimal discount_amount
        decimal actual_amount
        integer status
        integer payment_status
        varchar remark
        bigint create_by
        timestamp create_time
        bigint update_by
        timestamp update_time
        integer deleted
    }
```

## 馃敀 鏁版嵁瀹夊叏

### 鏁忔劅鏁版嵁鍔犲瘑
```sql
-- 鐢ㄦ埛瀵嗙爜浣跨敤BCrypt鍔犲瘑
-- 鎵嬫満鍙峰拰閭浣跨敤AES鍔犲瘑瀛樺偍
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 鍔犲瘑鍑芥暟绀轰緥
CREATE OR REPLACE FUNCTION encrypt_sensitive_data(data TEXT) RETURNS TEXT AS $$
BEGIN
    RETURN encode(encrypt(data::bytea, 'encryption_key', 'aes'), 'base64');
END;
$$ LANGUAGE plpgsql;

-- 瑙ｅ瘑鍑芥暟绀轰緥
CREATE OR REPLACE FUNCTION decrypt_sensitive_data(encrypted_data TEXT) RETURNS TEXT AS $$
BEGIN
    RETURN convert_from(decrypt(decode(encrypted_data, 'base64'), 'encryption_key', 'aes'), 'UTF8');
END;
$$ LANGUAGE plpgsql;
```

### 鏁版嵁鑴辨晱
```sql
-- 鍒涘缓鑴辨晱瑙嗗浘
CREATE OR REPLACE VIEW user_masked AS
SELECT
    user_id,
    tenant_id,
    username,
    '***' AS password,
    real_name,
    CASE
        WHEN email IS NOT NULL THEN
            CONCAT(LEFT(email, 2), '***@', SPLIT_PART(email, '@', 2))
        ELSE NULL
    END AS email,
    CASE
        WHEN phone IS NOT NULL THEN
            CONCAT(LEFT(phone, 3), '****', RIGHT(phone, 4))
        ELSE NULL
    END AS phone,
    avatar,
    status,
    create_time,
    update_time
FROM sys_user
WHERE deleted = 0;
```

## 馃搳 鏁版嵁绾︽潫

### 涓氬姟绾︽潫
```sql
-- 鍏呯數绔欑紪鐮佸湪鍚屼竴绉熸埛鍐呭敮涓€
ALTER TABLE charging_station
ADD CONSTRAINT uk_station_tenant_code
UNIQUE (tenant_id, station_code);

-- 鍏呯數妗╃紪鐮佸湪鍚屼竴鍏呯數绔欏唴鍞竴
ALTER TABLE charger
ADD CONSTRAINT uk_charger_station_code
UNIQUE (station_id, charger_code);

-- 鐢ㄦ埛鍚嶅湪鍚屼竴绉熸埛鍐呭敮涓€
ALTER TABLE sys_user
ADD CONSTRAINT uk_user_tenant_username
UNIQUE (tenant_id, username);

-- 妫€鏌ョ害鏉?ALTER TABLE charging_order
ADD CONSTRAINT chk_order_energy
CHECK (end_energy >= start_energy);

ALTER TABLE charging_order
ADD CONSTRAINT chk_order_amount
CHECK (total_amount >= 0);

ALTER TABLE charger
ADD CONSTRAINT chk_charger_power
CHECK (power_rate > 0);
```

### 瑙﹀彂鍣?```sql
-- 鏇存柊鏃堕棿瑙﹀彂鍣?CREATE OR REPLACE FUNCTION update_modified_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 搴旂敤鍒扮浉鍏宠〃
CREATE TRIGGER trg_station_update_time
    BEFORE UPDATE ON charging_station
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_time();

CREATE TRIGGER trg_charger_update_time
    BEFORE UPDATE ON charger
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_time();

CREATE TRIGGER trg_order_update_time
    BEFORE UPDATE ON charging_order
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_time();
```

## 馃殌 鎬ц兘浼樺寲

### 绱㈠紩浼樺寲
```sql
-- 澶嶅悎绱㈠紩浼樺寲鏌ヨ鎬ц兘
CREATE INDEX idx_order_tenant_status_time ON charging_order(tenant_id, status, create_time DESC);
CREATE INDEX idx_charger_station_status ON charger(station_id, status, deleted);
CREATE INDEX idx_payment_order_status ON payment_order(order_id, status, create_time);

-- 閮ㄥ垎绱㈠紩
CREATE INDEX idx_active_stations ON charging_station(tenant_id, status) WHERE status = 1 AND deleted = 0;
CREATE INDEX idx_charging_orders ON charging_order(tenant_id, status) WHERE status = 1 AND deleted = 0;

-- 琛ㄨ揪寮忕储寮?CREATE INDEX idx_user_username_lower ON sys_user(LOWER(username)) WHERE deleted = 0;
CREATE INDEX idx_station_name_search ON charging_station USING gin(to_tsvector('chinese', station_name));
```

### 鍒嗗尯琛ㄨ璁?```sql
-- 璁㈠崟琛ㄦ寜鏈堝垎鍖?CREATE TABLE charging_order (
    order_id BIGSERIAL,
    tenant_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- 鍏朵粬瀛楁...
    deleted INTEGER DEFAULT 0
) PARTITION BY RANGE (create_time);

-- 鍒涘缓鍒嗗尯
CREATE TABLE charging_order_2025_11 PARTITION OF charging_order
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE charging_order_2025_12 PARTITION OF charging_order
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- 鑷姩鍒涘缓鍒嗗尯鐨勫嚱鏁?CREATE OR REPLACE FUNCTION create_monthly_partition()
RETURNS void AS $$
DECLARE
    start_date date;
    end_date date;
    partition_name text;
BEGIN
    start_date := date_trunc('month', CURRENT_DATE + interval '1 month');
    end_date := start_date + interval '1 month';
    partition_name := 'charging_order_' || to_char(start_date, 'YYYY_MM');

    EXECUTE format('CREATE TABLE %I PARTITION OF charging_order FOR VALUES FROM (%L) TO (%L)',
                   partition_name, start_date, end_date);
END;
$$ LANGUAGE plpgsql;
```

## 馃搱 鏁版嵁缁熻

### 缁熻瑙嗗浘
```sql
-- 鍏呯數绔欑粺璁¤鍥?CREATE OR REPLACE VIEW station_statistics AS
SELECT
    s.tenant_id,
    COUNT(*) as total_stations,
    COUNT(CASE WHEN s.status = 1 THEN 1 END) as active_stations,
    COUNT(CASE WHEN s.status = 0 THEN 1 END) as inactive_stations,
    COUNT(c.charger_id) as total_chargers,
    COUNT(CASE WHEN c.status = 1 THEN 1 END) as active_chargers,
    COUNT(CASE WHEN c.status = 2 THEN 1 END) as charging_chargers,
    COUNT(CASE WHEN c.status = 3 THEN 1 END) as faulty_chargers,
    SUM(c.power_rate) as total_power
FROM charging_station s
LEFT JOIN charger c ON s.station_id = c.station_id AND c.deleted = 0
WHERE s.deleted = 0
GROUP BY s.tenant_id;

-- 璁㈠崟缁熻瑙嗗浘
CREATE OR REPLACE VIEW order_statistics AS
SELECT
    tenant_id,
    DATE_TRUNC('day', create_time) as stat_date,
    COUNT(*) as total_orders,
    COUNT(CASE WHEN status = 5 THEN 1 END) as paid_orders,
    COUNT(CASE WHEN status = 1 THEN 1 END) as charging_orders,
    SUM(total_energy) as total_energy,
    SUM(total_amount) as total_amount,
    AVG(total_amount) as avg_amount
FROM charging_order
WHERE deleted = 0
GROUP BY tenant_id, DATE_TRUNC('day', create_time);
```

### 瀹氭椂缁熻浠诲姟
```sql
-- 鍒涘缓缁熻琛?CREATE TABLE daily_statistics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    total_orders INTEGER DEFAULT 0,
    total_users INTEGER DEFAULT 0,
    total_stations INTEGER DEFAULT 0,
    total_chargers INTEGER DEFAULT 0,
    total_energy DECIMAL(12, 2) DEFAULT 0,
    total_amount DECIMAL(12, 2) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_daily_stats UNIQUE (tenant_id, stat_date)
);

-- 缁熻鍑芥暟
CREATE OR REPLACE FUNCTION calculate_daily_statistics(target_date DATE DEFAULT CURRENT_DATE)
RETURNS void AS $$
BEGIN
    INSERT INTO daily_statistics (tenant_id, stat_date, total_orders, total_energy, total_amount)
    SELECT
        tenant_id,
        target_date,
        COUNT(*),
        COALESCE(SUM(total_energy), 0),
        COALESCE(SUM(total_amount), 0)
    FROM charging_order
    WHERE DATE(create_time) = target_date AND deleted = 0
    GROUP BY tenant_id
    ON CONFLICT (tenant_id, stat_date)
    DO UPDATE SET
        total_orders = EXCLUDED.total_orders,
        total_energy = EXCLUDED.total_energy,
        total_amount = EXCLUDED.total_amount;
END;
$$ LANGUAGE plpgsql;
```

## 馃敡 鏁版嵁杩佺Щ

### 杩佺Щ鑴氭湰绀轰緥
```sql
-- V1__create_base_tables.sql
-- 鍩虹琛ㄧ粨鏋?
-- V2__add_billing_system.sql
-- 娣诲姞璁¤垂绯荤粺鐩稿叧琛?ALTER TABLE charging_order ADD COLUMN billing_plan_id BIGINT;
CREATE TABLE billing_plan (...);

-- V3__optimize_indexes.sql
-- 浼樺寲绱㈠紩
CREATE INDEX CONCURRENTLY idx_order_tenant_status_time ON charging_order(tenant_id, status, create_time DESC);

-- V4__add_payment_system.sql
-- 娣诲姞鏀粯绯荤粺
CREATE TABLE payment_order (...);
ALTER TABLE charging_order ADD COLUMN payment_status INTEGER DEFAULT 0;
```

## 馃搵 鏁版嵁瀛楀吀

### 琛ㄥ瓧娈佃鏄?| 琛ㄥ悕 | 瀛楁鍚?| 绫诲瀷 | 璇存槑 | 绾︽潫 |
|------|--------|------|------|------|
| sys_tenant | tenant_id | BIGSERIAL | 绉熸埛ID | 涓婚敭 |
| sys_tenant | tenant_code | VARCHAR(32) | 绉熸埛缂栫爜 | 鍞竴锛岄潪绌?|
| sys_tenant | tenant_name | VARCHAR(100) | 绉熸埛鍚嶇О | 闈炵┖ |
| sys_tenant | tenant_type | INTEGER | 绉熸埛绫诲瀷 | 1-骞冲彴鏂癸紝2-杩愯惀鍟嗭紝3-鍚堜綔浼欎即 |
| sys_tenant | status | INTEGER | 鐘舵€?| 1-鍚敤锛?-绂佺敤 |
| charging_station | station_id | BIGSERIAL | 鍏呯數绔橧D | 涓婚敭 |
| charging_station | station_code | VARCHAR(64) | 鍏呯數绔欑紪鐮?| 鍞竴锛岄潪绌?|
| charging_station | latitude | DECIMAL(10,8) | 绾害 | |
| charging_station | longitude | DECIMAL(11,8) | 缁忓害 | |
| charger | charger_id | BIGSERIAL | 鍏呯數妗㊣D | 涓婚敭 |
| charger | charger_type | INTEGER | 鍏呯數妗╃被鍨?| 1-鐩存祦锛?-浜ゆ祦锛?-浜ょ洿娴佷竴浣?|
| charger | power_rate | DECIMAL(8,2) | 棰濆畾鍔熺巼 | 鍗曚綅锛歬W |
| charger | status | INTEGER | 鐘舵€?| 0-绂荤嚎锛?-绌洪棽锛?-鍏呯數涓紝3-鏁呴殰 |
| charging_order | order_id | BIGSERIAL | 璁㈠崟ID | 涓婚敭 |
| charging_order | order_no | VARCHAR(64) | 璁㈠崟鍙?| 鍞竴锛岄潪绌?|
| charging_order | total_energy | DECIMAL(10,2) | 鎬诲厖鐢甸噺 | 鍗曚綅锛歬Wh |
| charging_order | total_amount | DECIMAL(10,2) | 鎬婚噾棰?| 鍗曚綅锛氬厓 |

---

**鐩稿叧鏂囨。**:
- [浜у搧闇€姹傛枃妗(./requirements.md)
- [鎶€鏈灦鏋勮璁(./architecture.md)
- [API鎺ュ彛璁捐](./api-design.md)
- [寮€鍙戣鑼僝(../development/coding-standards.md)

