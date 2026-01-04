-- Generate 1000-5000 orders for each station distributed over the last 2 quarters
-- Usage: Run this script in the database to populate data.

DO $$
DECLARE
    r_station RECORD;
    v_order_count INTEGER;
    v_i INTEGER;
    v_start_time TIMESTAMP;
    v_end_time TIMESTAMP;
    v_duration INTEGER; -- minutes
    v_energy DECIMAL(12,4);
    v_amount DECIMAL(12,4);
    v_charger_id BIGINT;
    v_status INTEGER;
    v_user_id BIGINT;
    v_billing_plan_id BIGINT;
    v_session_id VARCHAR(64);
BEGIN
    -- Loop through all stations
    FOR r_station IN SELECT station_id, tenant_id, station_name FROM charging_station LOOP
        
        -- Random number of orders between 1000 and 5000
        v_order_count := floor(random() * 4001 + 1000)::int;
        
        RAISE NOTICE 'Generating % orders for station: % (ID: %)', v_order_count, r_station.station_name, r_station.station_id;
        
        -- Get a billing plan for the tenant (optional, pick one if exists)
        SELECT id INTO v_billing_plan_id FROM billing_plan WHERE tenant_id = r_station.tenant_id LIMIT 1;

        FOR v_i IN 1..v_order_count LOOP
            -- Pick a random charger for this station
            SELECT charger_id INTO v_charger_id 
            FROM charger 
            WHERE station_id = r_station.station_id 
            ORDER BY random() 
            LIMIT 1;

            -- Generate a random session ID
            v_session_id := 'GEN-' || to_char(NOW(), 'YYYYMMDD') || '-' || floor(random() * 1000000)::text;

            -- Random user ID between 1000 and 1050
            v_user_id := floor(random() * 51 + 1000)::bigint;

            -- Random start time in last 6 months (2 quarters)
            v_start_time := NOW() - (random() * interval '6 months');
            
            -- Random duration between 10 and 120 minutes
            v_duration := floor(random() * 111 + 10)::int;
            v_end_time := v_start_time + (v_duration || ' minutes')::interval;
            
            -- Random energy: roughly 0.5 kWh per minute? say 10 to 100 kWh
            v_energy := (random() * 90 + 10)::decimal(12,4);
            
            -- Random amount: roughly 1.0 to 1.8 RMB per kWh
            v_amount := v_energy * (1.0 + random() * 0.8); 
            
            -- Status: mostly completed (11 - paid), some others
            -- 0-created, 1-completed, 2-cancelled, 10-to_pay, 11-paid, 12-refunding, 13-refunded
            IF random() < 0.9 THEN
                v_status := 11; -- Paid
            ELSE
                v_status := 10; -- To pay
            END IF;

            INSERT INTO charging_order (
                tenant_id, station_id, charger_id, session_id, user_id,
                start_time, end_time, energy, duration, amount,
                billing_plan_id, status, create_time, update_time, deleted, version
            ) VALUES (
                r_station.tenant_id,
                r_station.station_id,
                v_charger_id,
                v_session_id,
                v_user_id,
                v_start_time,
                v_end_time,
                v_energy,
                v_duration,
                v_amount,
                v_billing_plan_id,
                v_status,
                v_start_time, -- create_time
                v_end_time,   -- update_time
                0,
                1
            );
        END LOOP;
    END LOOP;
END $$;
