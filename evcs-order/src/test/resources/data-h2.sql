-- Test data for H2 database

-- Insert test billing plans - Split into individual statements
INSERT INTO billing_plan (id, tenant_id, station_id, name, code, status, is_default, priority, create_time, update_time, create_by, update_by, deleted, version)
VALUES (100, 1, NULL, 'Default Plan', 'DEFAULT', 1, 1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

INSERT INTO billing_plan (id, tenant_id, station_id, name, code, status, is_default, priority, create_time, update_time, create_by, update_by, deleted, version)
VALUES (101, 1, 1001, 'Station 1001 Plan', 'STATION_1001', 1, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

INSERT INTO billing_plan (id, tenant_id, station_id, name, code, status, is_default, priority, create_time, update_time, create_by, update_by, deleted, version)
VALUES (102, 1, 1003, 'Station 1003 Plan', 'STATION_1003', 1, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

INSERT INTO billing_plan (id, tenant_id, station_id, name, code, status, is_default, priority, create_time, update_time, create_by, update_by, deleted, version)
VALUES (103, 1, NULL, 'Premium Plan', 'PREMIUM', 1, 0, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

INSERT INTO billing_plan (id, tenant_id, station_id, name, code, status, is_default, priority, create_time, update_time, create_by, update_by, deleted, version)
VALUES (104, 1, 1005, 'Station 1005 Plan', 'STATION_1005', 1, 0, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

-- Insert test billing plan segments (time-of-use periods) - Split into individual statements
INSERT INTO billing_plan_segment (id, tenant_id, plan_id, start_time, end_time, create_time, update_time, create_by, update_by, deleted, version)
VALUES (1, 1, 100, '00:00:00', '06:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

INSERT INTO billing_plan_segment (id, tenant_id, plan_id, start_time, end_time, create_time, update_time, create_by, update_by, deleted, version)
VALUES (2, 1, 100, '06:00:00', '22:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

INSERT INTO billing_plan_segment (id, tenant_id, plan_id, start_time, end_time, create_time, update_time, create_by, update_by, deleted, version)
VALUES (3, 1, 100, '22:00:00', '23:59:59', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

-- Insert test billing rates - Split into individual statements
INSERT INTO billing_rate (id, tenant_id, station_id, segment_id, rate_type, rate, tou_enabled, peak_start, peak_end, peak_price, offpeak_price, flat_price, service_fee, status, create_time, update_time, create_by, update_by, deleted, version)
VALUES (1, 1, NULL, 1, 1, 0.80, 1, '06:00', '22:00', 1.50, 0.80, 1.00, 0.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

INSERT INTO billing_rate (id, tenant_id, station_id, segment_id, rate_type, rate, tou_enabled, peak_start, peak_end, peak_price, offpeak_price, flat_price, service_fee, status, create_time, update_time, create_by, update_by, deleted, version)
VALUES (2, 1, 1001, 2, 1, 1.50, 1, '06:00', '22:00', 1.50, 0.80, 1.00, 0.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);

INSERT INTO billing_rate (id, tenant_id, station_id, segment_id, rate_type, rate, tou_enabled, peak_start, peak_end, peak_price, offpeak_price, flat_price, service_fee, status, create_time, update_time, create_by, update_by, deleted, version)
VALUES (3, 1, NULL, 3, 1, 0.80, 1, '06:00', '22:00', 1.50, 0.80, 1.00, 0.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 1, 0, 1);
