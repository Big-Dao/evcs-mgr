-- Cleanup script for H2 test database
-- This script cleans up test data while preserving tenant records

-- Delete charger data (充电桩)
DELETE FROM charger WHERE charger_id > 0;

-- Delete charging station data (充电站)
DELETE FROM charging_station WHERE station_id > 0;

-- Reset auto-increment sequences
ALTER TABLE charger ALTER COLUMN charger_id RESTART WITH 1;
ALTER TABLE charging_station ALTER COLUMN station_id RESTART WITH 1;

-- Note: sys_tenant data is preserved for testing
-- Tenant 1 (SYSTEM) and Tenant 2 (TENANT_002) remain in the database
