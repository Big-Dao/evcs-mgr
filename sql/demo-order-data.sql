-- EVCS Manager demo dataset for tenant PLATFORM-001 (tenant_id = 1001)
-- Populates billing plan segments and sample orders used by the admin frontend.

BEGIN;

INSERT INTO billing_plan (
    id, tenant_id, station_id, name, code, status, is_default,
    create_time, update_time, create_by, update_by, deleted, version,
    priority, effective_start_date, effective_end_date
)
VALUES (
    11001, 1001, 2, '星辰能源标准计费方案', 'PLT1001-STD', 1, 1,
    CURRENT_TIMESTAMP - INTERVAL '90 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    1010, 1010, 0, 1, 1,
    CURRENT_DATE - INTERVAL '90 days',
    CURRENT_DATE + INTERVAL '365 days'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO billing_plan_segment (
    id, tenant_id, plan_id, segment_index,
    start_time, end_time, energy_price, service_fee,
    create_time, update_time, create_by, update_by, deleted
)
VALUES
    (210001, 1001, 11001, 1, '00:00', '08:00', 0.68, 0.35,
     CURRENT_TIMESTAMP - INTERVAL '90 days',
     CURRENT_TIMESTAMP - INTERVAL '90 days', 1010, 1010, 0),
    (210002, 1001, 11001, 2, '08:00', '18:00', 0.85, 0.40,
     CURRENT_TIMESTAMP - INTERVAL '90 days',
     CURRENT_TIMESTAMP - INTERVAL '90 days', 1010, 1010, 0),
    (210003, 1001, 11001, 3, '18:00', '24:00', 0.78, 0.38,
     CURRENT_TIMESTAMP - INTERVAL '90 days',
     CURRENT_TIMESTAMP - INTERVAL '90 days', 1010, 1010, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO charging_order (
    id, tenant_id, station_id, charger_id, session_id, user_id,
    start_time, end_time, energy, duration, amount, billing_plan_id,
    payment_trade_id, paid_time, status,
    create_time, update_time, create_by, update_by, deleted, version
)
VALUES
    (310001, 1001, 2, 3, 'SCN-202411-0001', 1010,
     CURRENT_TIMESTAMP - INTERVAL '5 days' - INTERVAL '1 hour',
     CURRENT_TIMESTAMP - INTERVAL '5 days', 36.8000, 54, 51.9600, 11001,
     'PAY-PLT-0001', CURRENT_TIMESTAMP - INTERVAL '5 days', 11,
     CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days',
     1010, 1010, 0, 1),
    (310002, 1001, 2, 4, 'SCN-202411-0002', 1010,
     CURRENT_TIMESTAMP - INTERVAL '3 days' - INTERVAL '2 hours',
     CURRENT_TIMESTAMP - INTERVAL '3 days' - INTERVAL '1 hour', 28.4000, 65, 40.1200, 11001,
     'PAY-PLT-0002', CURRENT_TIMESTAMP - INTERVAL '3 days' - INTERVAL '1 hour', 11,
     CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days',
     1010, 1010, 0, 1),
    (310003, 1001, 2, 3, 'SCN-202411-0003', 1010,
     CURRENT_TIMESTAMP - INTERVAL '2 days' - INTERVAL '2 hours',
     CURRENT_TIMESTAMP - INTERVAL '2 days' - INTERVAL '30 minutes', 18.7500, 90, 26.8000, 11001,
     NULL, NULL, 10,
     CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days',
     1010, 1010, 0, 1),
    (310004, 1001, 2, 4, 'SCN-202411-0004', 1010,
     CURRENT_TIMESTAMP - INTERVAL '1 day' - INTERVAL '1 hour',
     CURRENT_TIMESTAMP - INTERVAL '1 day', 22.5000, 48, 31.4000, 11001,
     'PAY-PLT-0004', CURRENT_TIMESTAMP - INTERVAL '23 hours', 12,
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '12 hours',
     1010, 1010, 0, 1),
    (310005, 1001, 2, 3, 'SCN-202411-0005', 1010,
     CURRENT_TIMESTAMP - INTERVAL '6 hours', NULL, NULL, NULL, NULL, 11001,
     NULL, NULL, 0,
     CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '6 hours',
     1010, 1010, 0, 1)
ON CONFLICT (id) DO NOTHING;

SELECT setval('billing_plan_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM billing_plan), 1));
SELECT setval('billing_plan_segment_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM billing_plan_segment), 1));
SELECT setval('charging_order_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM charging_order), 1));

COMMIT;
