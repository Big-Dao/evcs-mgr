-- Week 4 Day 4: Database Query Optimization
-- Add performance index for charger table

-- Index for station-based charger queries with status filtering
-- Covers: station_id + status for efficient station charger lookup
CREATE INDEX IF NOT EXISTS idx_charger_station_status 
ON charger(station_id, status);

COMMENT ON INDEX idx_charger_station_status IS 'Performance index for station-based charger status queries';
