-- ============================================================================
-- 为 admin 用户生成大量测试数据
-- 用户: admin (ID: 1)
-- 租户: 系统租户 (ID: 1, tenant_code: SYSTEM)
-- 生成时间: 2025-10-31
-- ============================================================================

-- 清理旧的测试数据（可选，如果需要保留现有数据请注释掉）
-- DELETE FROM charging_order WHERE tenant_id = 1 AND id >= 200000;
-- DELETE FROM charger WHERE tenant_id = 1 AND id >= 2000;
-- DELETE FROM billing_plan_segment WHERE plan_id IN (SELECT id FROM billing_plan WHERE tenant_id = 1 AND id >= 2000);
-- DELETE FROM billing_plan WHERE tenant_id = 1 AND id >= 2000;
-- DELETE FROM charging_station WHERE tenant_id = 1 AND id >= 200;

-- ============================================================================
-- 1. 充电站数据 (50个站点)
-- ============================================================================

-- 深圳市充电站 (15个)
INSERT INTO charging_station (tenant_id, station_code, station_name, province, city, district, address, longitude, latitude, status, operator_name, operator_phone, service_hours, facilities, create_by, update_by, create_time, update_time, deleted) VALUES
(201, 1, 'T1-SZ-001', '系统租户 - 南山科技园站', '广东省', '深圳市', '南山区', '深圳市南山区科技园科技中路1号', 113.9520, 22.5431, 12, 10, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day', 0),
(202, 1, 'T1-SZ-002', '系统租户 - 福田CBD站', '广东省', '深圳市', '福田区', '深圳市福田区福华路88号', 114.0579, 22.5456, 10, 8, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "充电宝"]', 1, 1, NOW() - INTERVAL '85 days', NOW() - INTERVAL '2 days', 0),
(203, 1, 'T1-SZ-003', '系统租户 - 罗湖商业区站', '广东省', '深圳市', '罗湖区', '深圳市罗湖区人民南路3008号', 114.1245, 22.5478, 8, 7, 1, '系统运营商', '0755-86012345', '06:00-23:00', '["wifi", "休息室"]', 1, 1, NOW() - INTERVAL '80 days', NOW() - INTERVAL '3 days', 0),
(204, 1, 'T1-SZ-004', '系统租户 - 宝安中心站', '广东省', '深圳市', '宝安区', '深圳市宝安区新安街道建安一路88号', 113.8849, 22.5546, 15, 12, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '75 days', NOW() - INTERVAL '4 days', 0),
(205, 1, 'T1-SZ-005', '系统租户 - 龙华新区站', '广东省', '深圳市', '龙华区', '深圳市龙华区民治街道梅龙路88号', 114.0296, 22.6569, 10, 9, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '70 days', NOW() - INTERVAL '5 days', 0),
(206, 1, 'T1-SZ-006', '系统租户 - 龙岗中心城站', '广东省', '深圳市', '龙岗区', '深圳市龙岗区龙城街道龙翔大道88号', 114.2471, 22.7208, 12, 11, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "洗手间"]', 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(207, 1, 'T1-SZ-007', '系统租户 - 坪山高铁站', '广东省', '深圳市', '坪山区', '深圳市坪山区坪山站前路1号', 114.3471, 22.7008, 20, 18, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室"]', 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(208, 1, 'T1-SZ-008', '系统租户 - 光明新区站', '广东省', '深圳市', '光明区', '深圳市光明区光明街道光明大道88号', 113.9296, 22.7469, 8, 7, 1, '系统运营商', '0755-86012345', '06:00-23:00', '["wifi", "休息室"]', 1, 1, NOW() - INTERVAL '55 days', NOW(), 0),
(209, 1, 'T1-SZ-009', '系统租户 - 前海自贸区站', '广东省', '深圳市', '南山区', '深圳市南山区前海路88号', 113.8949, 22.5246, 18, 15, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '50 days', NOW(), 0),
(210, 1, 'T1-SZ-010', '系统租户 - 蛇口港站', '广东省', '深圳市', '南山区', '深圳市南山区蛇口港湾大道88号', 113.9149, 22.4846, 10, 9, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '45 days', NOW(), 0),
(211, 1, 'T1-SZ-011', '系统租户 - 盐田港区站', '广东省', '深圳市', '盐田区', '深圳市盐田区盐田路88号', 114.2371, 22.5708, 12, 10, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "洗手间"]', 1, 1, NOW() - INTERVAL '40 days', NOW(), 0),
(212, 1, 'T1-SZ-012', '系统租户 - 大鹏新区站', '广东省', '深圳市', '大鹏新区', '深圳市大鹏新区葵涌街道金岭路88号', 114.4771, 22.6008, 6, 5, 1, '系统运营商', '0755-86012345', '07:00-22:00', '["wifi", "休息室"]', 1, 1, NOW() - INTERVAL '35 days', NOW(), 0),
(213, 1, 'T1-SZ-013', '系统租户 - 深圳北站', '广东省', '深圳市', '龙华区', '深圳市龙华区民治街道致远中路28号', 114.0296, 22.6089, 25, 22, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室", "ATM"]', 1, 1, NOW() - INTERVAL '30 days', NOW(), 0),
(214, 1, 'T1-SZ-014', '系统租户 - 深圳机场站', '广东省', '深圳市', '宝安区', '深圳市宝安区福永街道机场路领航高架桥', 113.8109, 22.6389, 30, 28, 1, '系统运营商', '0755-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室", "ATM", "贵宾室"]', 1, 1, NOW() - INTERVAL '25 days', NOW(), 0),
(215, 1, 'T1-SZ-015', '系统租户 - 欢乐谷站', '广东省', '深圳市', '南山区', '深圳市南山区侨城西街18号', 113.9749, 22.5446, 15, 13, 1, '系统运营商', '0755-86012345', '08:00-22:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '20 days', NOW(), 0);

-- 广州市充电站 (10个)
INSERT INTO charging_station (id, tenant_id, station_code, station_name, province, city, district, address, longitude, latitude, total_chargers, available_chargers, status, operator_name, operator_phone, service_hours, facilities, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(216, 1, 'T1-GZ-001', '系统租户 - 天河CBD站', '广东省', '广州市', '天河区', '广州市天河区天河路208号', 113.3245, 23.1369, 12, 10, 1, '系统运营商', '020-38012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '80 days', NOW(), 0),
(217, 1, 'T1-GZ-002', '系统租户 - 珠江新城站', '广东省', '广州市', '天河区', '广州市天河区珠江东路88号', 113.3345, 23.1169, 15, 13, 1, '系统运营商', '020-38012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '75 days', NOW(), 0),
(218, 1, 'T1-GZ-003', '系统租户 - 白云机场站', '广东省', '广州市', '白云区', '广州市白云区机场路888号', 113.2989, 23.3925, 28, 25, 1, '系统运营商', '020-38012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室", "ATM"]', 1, 1, NOW() - INTERVAL '70 days', NOW(), 0),
(219, 1, 'T1-GZ-004', '系统租户 - 番禺广场站', '广东省', '广州市', '番禺区', '广州市番禺区市桥街道繁华路88号', 113.3845, 22.9369, 10, 8, 1, '系统运营商', '020-38012345', '00:00-24:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(220, 1, 'T1-GZ-005', '系统租户 - 广州南站', '广东省', '广州市', '番禺区', '广州市番禺区石壁街道南站北路', 113.2645, 23.0069, 22, 20, 1, '系统运营商', '020-38012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室"]', 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(221, 1, 'T1-GZ-006', '系统租户 - 黄埔区站', '广东省', '广州市', '黄埔区', '广州市黄埔区黄埔东路88号', 113.4645, 23.1069, 8, 7, 1, '系统运营商', '020-38012345', '06:00-23:00', '["wifi", "休息室"]', 1, 1, NOW() - INTERVAL '55 days', NOW(), 0),
(222, 1, 'T1-GZ-007', '系统租户 - 海珠商圈站', '广东省', '广州市', '海珠区', '广州市海珠区江南大道中88号', 113.3145, 23.0869, 10, 9, 1, '系统运营商', '020-38012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '50 days', NOW(), 0),
(223, 1, 'T1-GZ-008', '系统租户 - 荔湾老城站', '广东省', '广州市', '荔湾区', '广州市荔湾区中山七路88号', 113.2545, 23.1269, 6, 5, 1, '系统运营商', '020-38012345', '07:00-22:00', '["wifi", "休息室"]', 1, 1, NOW() - INTERVAL '45 days', NOW(), 0),
(224, 1, 'T1-GZ-009', '系统租户 - 增城新区站', '广东省', '广州市', '增城区', '广州市增城区荔城街道府佑路88号', 113.8145, 23.2669, 12, 11, 1, '系统运营商', '020-38012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '40 days', NOW(), 0),
(225, 1, 'T1-GZ-010', '系统租户 - 从化温泉站', '广东省', '广州市', '从化区', '广州市从化区温泉镇温泉东路88号', 113.5945, 23.6369, 8, 7, 1, '系统运营商', '020-38012345', '08:00-22:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '35 days', NOW(), 0);

-- 北京市充电站 (8个)
INSERT INTO charging_station (id, tenant_id, station_code, station_name, province, city, district, address, longitude, latitude, total_chargers, available_chargers, status, operator_name, operator_phone, service_hours, facilities, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(226, 1, 'T1-BJ-001', '系统租户 - 中关村站', '北京市', '北京市', '海淀区', '北京市海淀区中关村大街1号', 116.3174, 39.9869, 15, 13, 1, '系统运营商', '010-62012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '90 days', NOW(), 0),
(227, 1, 'T1-BJ-002', '系统租户 - 国贸CBD站', '北京市', '北京市', '朝阳区', '北京市朝阳区建国门外大街1号', 116.4574, 39.9089, 18, 16, 1, '系统运营商', '010-62012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '85 days', NOW(), 0),
(228, 1, 'T1-BJ-003', '系统租户 - 首都机场站', '北京市', '北京市', '顺义区', '北京市顺义区首都机场3号航站楼', 116.5974, 40.0789, 32, 30, 1, '系统运营商', '010-62012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室", "ATM", "贵宾室"]', 1, 1, NOW() - INTERVAL '80 days', NOW(), 0),
(229, 1, 'T1-BJ-004', '系统租户 - 亦庄开发区站', '北京市', '北京市', '大兴区', '北京市大兴区亦庄经济技术开发区科创路88号', 116.5074, 39.7889, 12, 10, 1, '系统运营商', '010-62012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '75 days', NOW(), 0),
(230, 1, 'T1-BJ-005', '系统租户 - 西单商圈站', '北京市', '北京市', '西城区', '北京市西城区西单北大街88号', 116.3774, 39.9189, 10, 8, 1, '系统运营商', '010-62012345', '00:00-24:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '70 days', NOW(), 0),
(231, 1, 'T1-BJ-006', '系统租户 - 望京商务区站', '北京市', '北京市', '朝阳区', '北京市朝阳区望京街道阜通东大街88号', 116.4774, 40.0089, 14, 12, 1, '系统运营商', '010-62012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(232, 1, 'T1-BJ-007', '系统租户 - 通州副中心站', '北京市', '北京市', '通州区', '北京市通州区新华大街88号', 116.6574, 39.9089, 16, 14, 1, '系统运营商', '010-62012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(233, 1, 'T1-BJ-008', '系统租户 - 石景山万达站', '北京市', '北京市', '石景山区', '北京市石景山区石景山路88号', 116.2274, 39.9189, 8, 7, 1, '系统运营商', '010-62012345', '08:00-22:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '55 days', NOW(), 0);

-- 上海市充电站 (8个)
INSERT INTO charging_station (id, tenant_id, station_code, station_name, province, city, district, address, longitude, latitude, total_chargers, available_chargers, status, operator_name, operator_phone, service_hours, facilities, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(234, 1, 'T1-SH-001', '系统租户 - 陆家嘴金融区站', '上海市', '上海市', '浦东新区', '上海市浦东新区陆家嘴环路1000号', 121.5045, 31.2389, 20, 18, 1, '系统运营商', '021-58012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '90 days', NOW(), 0),
(235, 1, 'T1-SH-002', '系统租户 - 南京路步行街站', '上海市', '上海市', '黄浦区', '上海市黄浦区南京东路558号', 121.4845, 31.2389, 12, 10, 1, '系统运营商', '021-58012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '85 days', NOW(), 0),
(236, 1, 'T1-SH-003', '系统租户 - 虹桥枢纽站', '上海市', '上海市', '闵行区', '上海市闵行区申虹路88号', 121.3145, 31.1989, 35, 32, 1, '系统运营商', '021-58012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室", "ATM", "贵宾室"]', 1, 1, NOW() - INTERVAL '80 days', NOW(), 0),
(237, 1, 'T1-SH-004', '系统租户 - 浦东机场站', '上海市', '上海市', '浦东新区', '上海市浦东新区机场大道888号', 121.8045, 31.1489, 30, 28, 1, '系统运营商', '021-58012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室", "ATM"]', 1, 1, NOW() - INTERVAL '75 days', NOW(), 0),
(238, 1, 'T1-SH-005', '系统租户 - 静安寺商圈站', '上海市', '上海市', '静安区', '上海市静安区南京西路1686号', 121.4445, 31.2289, 10, 8, 1, '系统运营商', '021-58012345', '00:00-24:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '70 days', NOW(), 0),
(239, 1, 'T1-SH-006', '系统租户 - 张江高科站', '上海市', '上海市', '浦东新区', '上海市浦东新区张江路88号', 121.6045, 31.2089, 14, 12, 1, '系统运营商', '021-58012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(240, 1, 'T1-SH-007', '系统租户 - 徐家汇商圈站', '上海市', '上海市', '徐汇区', '上海市徐汇区虹桥路88号', 121.4345, 31.1889, 12, 11, 1, '系统运营商', '021-58012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(241, 1, 'T1-SH-008', '系统租户 - 杨浦大学城站', '上海市', '上海市', '杨浦区', '上海市杨浦区五角场淞沪路88号', 121.5145, 31.3089, 10, 9, 1, '系统运营商', '021-58012345', '00:00-24:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '55 days', NOW(), 0);

-- 成都市充电站 (5个)
INSERT INTO charging_station (id, tenant_id, station_code, station_name, province, city, district, address, longitude, latitude, total_chargers, available_chargers, status, operator_name, operator_phone, service_hours, facilities, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(242, 1, 'T1-CD-001', '系统租户 - 天府广场站', '四川省', '成都市', '锦江区', '成都市锦江区人民南路1段88号', 104.0745, 30.6669, 12, 10, 1, '系统运营商', '028-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '70 days', NOW(), 0),
(243, 1, 'T1-CD-002', '系统租户 - 春熙路商圈站', '四川省', '成都市', '锦江区', '成都市锦江区春熙路88号', 104.0845, 30.6569, 10, 8, 1, '系统运营商', '028-86012345', '00:00-24:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(244, 1, 'T1-CD-003', '系统租户 - 双流机场站', '四川省', '成都市', '双流区', '成都市双流区机场路666号', 103.9545, 30.5769, 28, 26, 1, '系统运营商', '028-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室", "ATM"]', 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(245, 1, 'T1-CD-004', '系统租户 - 高新区站', '四川省', '成都市', '武侯区', '成都市武侯区天府大道中段88号', 104.0645, 30.5569, 15, 13, 1, '系统运营商', '028-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '55 days', NOW(), 0),
(246, 1, 'T1-CD-005', '系统租户 - 宽窄巷子站', '四川省', '成都市', '青羊区', '成都市青羊区长顺上街88号', 104.0445, 30.6769, 8, 7, 1, '系统运营商', '028-86012345', '08:00-22:00', '["wifi", "休息室", "便利店"]', 1, 1, NOW() - INTERVAL '50 days', NOW(), 0);

-- 杭州市充电站 (4个)
INSERT INTO charging_station (id, tenant_id, station_code, station_name, province, city, district, address, longitude, latitude, total_chargers, available_chargers, status, operator_name, operator_phone, service_hours, facilities, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(247, 1, 'T1-HZ-001', '系统租户 - 西湖景区站', '浙江省', '杭州市', '西湖区', '杭州市西湖区北山街88号', 120.1345, 30.2589, 10, 8, 1, '系统运营商', '0571-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(248, 1, 'T1-HZ-002', '系统租户 - 滨江高新区站', '浙江省', '杭州市', '滨江区', '杭州市滨江区江南大道88号', 120.2145, 30.2089, 14, 12, 1, '系统运营商', '0571-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅"]', 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(249, 1, 'T1-HZ-003', '系统租户 - 萧山机场站', '浙江省', '杭州市', '萧山区', '杭州市萧山区机场高速公路', 120.4345, 30.2389, 25, 23, 1, '系统运营商', '0571-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间", "餐厅", "母婴室"]', 1, 1, NOW() - INTERVAL '55 days', NOW(), 0),
(250, 1, 'T1-HZ-004', '系统租户 - 武林广场站', '浙江省', '杭州市', '下城区', '杭州市下城区中山北路88号', 120.1645, 30.2789, 12, 10, 1, '系统运营商', '0571-86012345', '00:00-24:00', '["wifi", "休息室", "便利店", "洗手间"]', 1, 1, NOW() - INTERVAL '50 days', NOW(), 0);

-- ============================================================================
-- 2. 充电桩数据 (根据充电站配置生成，共约400个充电桩)
-- ============================================================================

-- 为每个充电站生成充电桩
-- 深圳站点充电桩 (201-215 stations)
DO $$
DECLARE
    station_record RECORD;
    charger_count INTEGER;
    charger_id INTEGER := 2000;
    i INTEGER;
    charger_type VARCHAR(10);
    charger_power NUMERIC(10,2);
    charger_status INTEGER;
BEGIN
    FOR station_record IN 
        SELECT id, station_code, station_name, total_chargers 
        FROM charging_station 
        WHERE id BETWEEN 201 AND 250
        ORDER BY id
    LOOP
        charger_count := station_record.total_chargers;
        
        FOR i IN 1..charger_count LOOP
            charger_id := charger_id + 1;
            
            -- 60% DC快充, 40% AC慢充
            IF i <= (charger_count * 0.6) THEN
                charger_type := 'DC';
                charger_power := CASE 
                    WHEN i % 3 = 0 THEN 180.00
                    WHEN i % 3 = 1 THEN 120.00
                    ELSE 90.00
                END;
            ELSE
                charger_type := 'AC';
                charger_power := CASE 
                    WHEN i % 2 = 0 THEN 11.00
                    ELSE 7.00
                END;
            END IF;
            
            -- 80% 运营中, 15% 充电中, 5% 维护中
            charger_status := CASE 
                WHEN i % 20 = 0 THEN 3  -- 维护中
                WHEN i % 7 = 0 OR i % 11 = 0 THEN 2  -- 充电中
                ELSE 1  -- 运营中
            END;
            
            INSERT INTO charger (
                id, tenant_id, station_id, charger_code, charger_name, 
                charger_type, max_power, status, manufacturer, model,
                qr_code, created_by, updated_by, created_time, updated_time, is_deleted
            ) VALUES (
                charger_id,
                1,
                station_record.id,
                station_record.station_code || '-C' || LPAD(i::TEXT, 3, '0'),
                REPLACE(station_record.station_name, '系统租户 - ', '') || i || '号' || 
                    CASE WHEN charger_type = 'DC' THEN '快充' ELSE '慢充' END,
                charger_type,
                charger_power,
                charger_status,
                CASE i % 4
                    WHEN 0 THEN '特来电'
                    WHEN 1 THEN '星星充电'
                    WHEN 2 THEN '云快充'
                    ELSE 'ABB'
                END,
                'Model-' || charger_type || '-' || charger_power::TEXT,
                'QR-' || station_record.station_code || '-C' || LPAD(i::TEXT, 3, '0'),
                1,
                1,
                NOW() - (INTERVAL '1 day' * (250 - station_record.id)),
                NOW() - (INTERVAL '1 hour' * (i % 24)),
                0
            );
        END LOOP;
    END LOOP;
END $$;

-- ============================================================================
-- 3. 计费方案数据 (30个)
-- ============================================================================

-- 标准计费方案 (5个)
INSERT INTO billing_plan (id, tenant_id, plan_code, plan_name, description, status, is_default, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(2001, 1, 'T1_STANDARD_01', '系统标准计费 - 普通时段', '适用于大部分充电站的标准计费方案', 1, 1, 1, 1, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day', 0),
(2002, 1, 'T1_STANDARD_02', '系统标准计费 - 峰谷电价', '峰谷分时电价计费方案', 1, 0, 1, 1, NOW() - INTERVAL '85 days', NOW() - INTERVAL '2 days', 0),
(2003, 1, 'T1_STANDARD_03', '系统标准计费 - 快充专属', '快充专用计费方案，功率越大越优惠', 1, 0, 1, 1, NOW() - INTERVAL '80 days', NOW() - INTERVAL '3 days', 0),
(2004, 1, 'T1_STANDARD_04', '系统标准计费 - 慢充优惠', '慢充专用优惠方案', 1, 0, 1, 1, NOW() - INTERVAL '75 days', NOW() - INTERVAL '4 days', 0),
(2005, 1, 'T1_STANDARD_05', '系统标准计费 - 夜间优惠', '夜间充电优惠方案（22:00-06:00）', 1, 0, 1, 1, NOW() - INTERVAL '70 days', NOW() - INTERVAL '5 days', 0);

-- 城市专属方案 (深圳5个)
INSERT INTO billing_plan (id, tenant_id, plan_code, plan_name, description, status, is_default, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(2006, 1, 'T1_SZ_01', '深圳 - 科技园专属', '深圳科技园区域专属计费', 1, 0, 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(2007, 1, 'T1_SZ_02', '深圳 - 机场高速', '深圳机场及周边高速充电', 1, 0, 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(2008, 1, 'T1_SZ_03', '深圳 - 前海自贸区', '前海自贸区企业优惠', 1, 0, 1, 1, NOW() - INTERVAL '55 days', NOW(), 0),
(2009, 1, 'T1_SZ_04', '深圳 - 宝安工业区', '宝安工业区物流车辆专属', 1, 0, 1, 1, NOW() - INTERVAL '50 days', NOW(), 0),
(2010, 1, 'T1_SZ_05', '深圳 - 龙华住宅区', '龙华住宅小区居民优惠', 1, 0, 1, 1, NOW() - INTERVAL '45 days', NOW(), 0);

-- 城市专属方案 (广州5个)
INSERT INTO billing_plan (id, tenant_id, plan_code, plan_name, description, status, is_default, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(2011, 1, 'T1_GZ_01', '广州 - 天河CBD', '天河CBD商务区专属', 1, 0, 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(2012, 1, 'T1_GZ_02', '广州 - 白云机场', '白云机场及周边区域', 1, 0, 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(2013, 1, 'T1_GZ_03', '广州 - 番禺新区', '番禺新区住宅优惠', 1, 0, 1, 1, NOW() - INTERVAL '55 days', NOW(), 0),
(2014, 1, 'T1_GZ_04', '广州 - 黄埔开发区', '黄埔开发区企业专属', 1, 0, 1, 1, NOW() - INTERVAL '50 days', NOW(), 0),
(2015, 1, 'T1_GZ_05', '广州 - 海珠商圈', '海珠商圈夜间优惠', 1, 0, 1, 1, NOW() - INTERVAL '45 days', NOW(), 0);

-- 城市专属方案 (北京5个)
INSERT INTO billing_plan (id, tenant_id, plan_code, plan_name, description, status, is_default, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(2016, 1, 'T1_BJ_01', '北京 - 中关村科技', '中关村科技园区专属', 1, 0, 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(2017, 1, 'T1_BJ_02', '北京 - 国贸CBD', '国贸CBD高端商务', 1, 0, 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(2018, 1, 'T1_BJ_03', '北京 - 首都机场', '首都机场交通枢纽', 1, 0, 1, 1, NOW() - INTERVAL '55 days', NOW(), 0),
(2019, 1, 'T1_BJ_04', '北京 - 亦庄开发区', '亦庄经济开发区优惠', 1, 0, 1, 1, NOW() - INTERVAL '50 days', NOW(), 0),
(2020, 1, 'T1_BJ_05', '北京 - 通州副中心', '通州副中心居民专属', 1, 0, 1, 1, NOW() - INTERVAL '45 days', NOW(), 0);

-- 城市专属方案 (上海5个)
INSERT INTO billing_plan (id, tenant_id, plan_code, plan_name, description, status, is_default, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(2021, 1, 'T1_SH_01', '上海 - 陆家嘴金融', '陆家嘴金融区专属', 1, 0, 1, 1, NOW() - INTERVAL '65 days', NOW(), 0),
(2022, 1, 'T1_SH_02', '上海 - 虹桥枢纽', '虹桥交通枢纽优惠', 1, 0, 1, 1, NOW() - INTERVAL '60 days', NOW(), 0),
(2023, 1, 'T1_SH_03', '上海 - 张江高科', '张江高科技园区', 1, 0, 1, 1, NOW() - INTERVAL '55 days', NOW(), 0),
(2024, 1, 'T1_SH_04', '上海 - 浦东机场', '浦东机场及周边', 1, 0, 1, 1, NOW() - INTERVAL '50 days', NOW(), 0),
(2025, 1, 'T1_SH_05', '上海 - 徐汇商圈', '徐汇商圈夜间优惠', 1, 0, 1, 1, NOW() - INTERVAL '45 days', NOW(), 0);

-- 特殊方案 (5个)
INSERT INTO billing_plan (id, tenant_id, plan_code, plan_name, description, status, is_default, created_by, updated_by, created_time, updated_time, is_deleted) VALUES
(2026, 1, 'T1_VIP_01', '系统 - VIP会员专享', 'VIP会员专属优惠方案', 1, 0, 1, 1, NOW() - INTERVAL '40 days', NOW(), 0),
(2027, 1, 'T1_FLEET_01', '系统 - 车队企业合作', '车队企业批量充电优惠', 1, 0, 1, 1, NOW() - INTERVAL '35 days', NOW(), 0),
(2028, 1, 'T1_PROMO_01', '系统 - 节假日促销', '节假日特别促销方案', 1, 0, 1, 1, NOW() - INTERVAL '30 days', NOW(), 0),
(2029, 1, 'T1_EMERGENCY_01', '系统 - 应急充电', '应急情况下的快速充电', 1, 0, 1, 1, NOW() - INTERVAL '25 days', NOW(), 0),
(2030, 1, 'T1_GREEN_01', '系统 - 绿色能源', '使用清洁能源的优惠方案', 1, 0, 1, 1, NOW() - INTERVAL '20 days', NOW(), 0);

-- 为每个计费方案生成时段分段（简化版：3个时段）
DO $$
DECLARE
    plan_id_var INTEGER;
    segment_id INTEGER := 20000;
BEGIN
    FOR plan_id_var IN 2001..2030 LOOP
        segment_id := segment_id + 1;
        -- 谷时段 (00:00-08:00)
        INSERT INTO billing_plan_segment (
            id, plan_id, segment_name, start_time, end_time, 
            energy_price, service_fee, created_by, updated_by, 
            created_time, updated_time, is_deleted
        ) VALUES (
            segment_id, plan_id_var, '谷时', '00:00:00', '08:00:00',
            0.50 + (plan_id_var % 10) * 0.05, 
            0.30 + (plan_id_var % 5) * 0.02,
            1, 1, NOW() - INTERVAL '60 days', NOW(), 0
        );
        
        segment_id := segment_id + 1;
        -- 平时段 (08:00-18:00)
        INSERT INTO billing_plan_segment (
            id, plan_id, segment_name, start_time, end_time, 
            energy_price, service_fee, created_by, updated_by, 
            created_time, updated_time, is_deleted
        ) VALUES (
            segment_id, plan_id_var, '平时', '08:00:00', '18:00:00',
            0.80 + (plan_id_var % 10) * 0.05, 
            0.40 + (plan_id_var % 5) * 0.02,
            1, 1, NOW() - INTERVAL '60 days', NOW(), 0
        );
        
        segment_id := segment_id + 1;
        -- 峰时段 (18:00-24:00)
        INSERT INTO billing_plan_segment (
            id, plan_id, segment_name, start_time, end_time, 
            energy_price, service_fee, created_by, updated_by, 
            created_time, updated_time, is_deleted
        ) VALUES (
            segment_id, plan_id_var, '峰时', '18:00:00', '23:59:59',
            1.20 + (plan_id_var % 10) * 0.05, 
            0.50 + (plan_id_var % 5) * 0.02,
            1, 1, NOW() - INTERVAL '60 days', NOW(), 0
        );
    END LOOP;
END $$;

-- ============================================================================
-- 4. 充电订单数据 (150个订单，涵盖不同状态和时间段)
-- ============================================================================

DO $$
DECLARE
    order_id INTEGER := 200000;
    i INTEGER;
    station_id_var INTEGER;
    charger_id_var INTEGER;
    plan_id_var INTEGER;
    order_status INTEGER;
    start_time_var TIMESTAMP;
    end_time_var TIMESTAMP;
    charging_duration INTEGER;
    energy_consumed NUMERIC(10,2);
    energy_cost NUMERIC(10,2);
    service_cost NUMERIC(10,2);
    total_cost NUMERIC(10,2);
    session_id_var VARCHAR(50);
BEGIN
    FOR i IN 1..150 LOOP
        order_id := order_id + 1;
        
        -- 随机选择站点和充电桩
        station_id_var := 201 + (i % 50);
        charger_id_var := 2000 + ((i * 7) % 400) + 1;
        plan_id_var := 2001 + (i % 30);
        
        -- 生成随机时间（最近30天内）
        start_time_var := NOW() - INTERVAL '1 day' * (i % 30) - INTERVAL '1 hour' * (i % 24);
        
        -- 订单状态分布：70% 已完成已支付，15% 已完成待支付，10% 充电中，5% 已取消
        IF i % 20 = 0 THEN
            order_status := 0;  -- 已取消
            end_time_var := start_time_var + INTERVAL '5 minutes';
            charging_duration := 5;
            energy_consumed := 0;
            energy_cost := 0;
            service_cost := 0;
            total_cost := 0;
        ELSIF i % 10 = 0 THEN
            order_status := 1;  -- 充电中
            end_time_var := NULL;
            charging_duration := EXTRACT(EPOCH FROM (NOW() - start_time_var)) / 60;
            energy_consumed := (i % 50) + 5.5;
            energy_cost := energy_consumed * (0.8 + (i % 10) * 0.05);
            service_cost := energy_consumed * (0.4 + (i % 5) * 0.02);
            total_cost := 0;  -- 未结算
        ELSIF i % 7 = 0 THEN
            order_status := 10;  -- 已完成待支付
            charging_duration := 45 + (i % 180);
            end_time_var := start_time_var + INTERVAL '1 minute' * charging_duration;
            energy_consumed := (i % 60) + 10.5;
            energy_cost := energy_consumed * (0.8 + (i % 10) * 0.05);
            service_cost := energy_consumed * (0.4 + (i % 5) * 0.02);
            total_cost := energy_cost + service_cost;
        ELSE
            order_status := 11;  -- 已完成已支付
            charging_duration := 30 + (i % 240);
            end_time_var := start_time_var + INTERVAL '1 minute' * charging_duration;
            energy_consumed := (i % 70) + 15.8;
            energy_cost := energy_consumed * (0.8 + (i % 10) * 0.05);
            service_cost := energy_consumed * (0.4 + (i % 5) * 0.02);
            total_cost := energy_cost + service_cost;
        END IF;
        
        session_id_var := 'T1_SESSION_' || LPAD(order_id::TEXT, 6, '0');
        
        INSERT INTO charging_order (
            id, tenant_id, session_id, user_id, station_id, charger_id,
            plan_id, start_time, end_time, charging_duration, energy_consumed,
            energy_cost, service_cost, total_cost, status, payment_method,
            payment_time, created_by, updated_by, created_time, updated_time, is_deleted
        ) VALUES (
            order_id,
            1,
            session_id_var,
            1,  -- admin用户
            station_id_var,
            charger_id_var,
            plan_id_var,
            start_time_var,
            end_time_var,
            charging_duration,
            energy_consumed,
            energy_cost,
            service_cost,
            total_cost,
            order_status,
            CASE 
                WHEN order_status = 11 THEN 
                    CASE i % 3
                        WHEN 0 THEN 1  -- 支付宝
                        WHEN 1 THEN 2  -- 微信
                        ELSE 3  -- 余额
                    END
                ELSE NULL
            END,
            CASE WHEN order_status = 11 THEN end_time_var + INTERVAL '2 minutes' ELSE NULL END,
            1,
            1,
            start_time_var,
            CASE WHEN order_status IN (10, 11) THEN end_time_var ELSE NOW() END,
            0
        );
    END LOOP;
END $$;

-- ============================================================================
-- 数据生成完成
-- ============================================================================

-- 统计信息
SELECT '充电站' as 类型, COUNT(*) as 数量 FROM charging_station WHERE tenant_id = 1 AND id >= 201
UNION ALL
SELECT '充电桩' as 类型, COUNT(*) as 数量 FROM charger WHERE tenant_id = 1 AND id >= 2000
UNION ALL
SELECT '计费方案' as 类型, COUNT(*) as 数量 FROM billing_plan WHERE tenant_id = 1 AND id >= 2001
UNION ALL
SELECT '计费时段' as 类型, COUNT(*) as 数量 FROM billing_plan_segment WHERE plan_id >= 2001
UNION ALL
SELECT '充电订单' as 类型, COUNT(*) as 数量 FROM charging_order WHERE tenant_id = 1 AND id >= 200001;

-- 订单状态分布
SELECT 
    CASE status
        WHEN 0 THEN '已取消'
        WHEN 1 THEN '充电中'
        WHEN 10 THEN '已完成待支付'
        WHEN 11 THEN '已完成已支付'
        ELSE '其他'
    END as 订单状态,
    COUNT(*) as 数量,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM charging_order WHERE tenant_id = 1 AND id >= 200001), 2) as 百分比
FROM charging_order 
WHERE tenant_id = 1 AND id >= 200001
GROUP BY status
ORDER BY status;
