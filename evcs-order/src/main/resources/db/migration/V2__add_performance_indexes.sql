-- Week 4 Day 4: Database Query Optimization
-- Add performance indexes for charging_order table

-- Index for tenant-based time range queries (used in reconciliation and reports)
-- Covers: tenant_id + start_time for efficient time-based filtering
CREATE INDEX IF NOT EXISTS idx_charging_order_tenant_start_time 
ON charging_order(tenant_id, start_time);

-- Index for charger status queries (used in charger management)
-- Covers: charger_id + status for efficient charger order lookup
CREATE INDEX IF NOT EXISTS idx_charging_order_charger_status 
ON charging_order(charger_id, status);

-- Index for station-based queries through charger relationship
-- Note: Assumes there's a charger table with station_id and status columns
-- This index is added to the charger table for efficient station filtering
-- CREATE INDEX IF NOT EXISTS idx_charger_station_status 
-- ON charger(station_id, status);
-- Commented out as charger table is in evcs-station module

COMMENT ON INDEX idx_charging_order_tenant_start_time IS 'Performance index for tenant-based time range queries';
COMMENT ON INDEX idx_charging_order_charger_status IS 'Performance index for charger status queries';
