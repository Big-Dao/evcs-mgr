-- 订单表反范式化：冗余站点名称/省市与充电桩编码，消除查询时对 station 服务表的 JOIN
-- （数据所有权归 station 服务，写入时经内部 API 解析；一次性回填沿用共享库）
ALTER TABLE charging_order ADD COLUMN IF NOT EXISTS station_name VARCHAR(100);
ALTER TABLE charging_order ADD COLUMN IF NOT EXISTS charger_code VARCHAR(50);
ALTER TABLE charging_order ADD COLUMN IF NOT EXISTS province VARCHAR(50);
ALTER TABLE charging_order ADD COLUMN IF NOT EXISTS city VARCHAR(50);

UPDATE charging_order co
SET station_name = cs.station_name, province = cs.province, city = cs.city
FROM charging_station cs
WHERE co.station_id = cs.station_id AND co.station_name IS NULL;

UPDATE charging_order co
SET charger_code = c.charger_code
FROM charger c
WHERE co.charger_id = c.charger_id AND co.charger_code IS NULL;
