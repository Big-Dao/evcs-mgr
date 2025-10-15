-- Align billing_plan table with service expectations
-- Ensure priority and effective date columns exist for scheduling queries

ALTER TABLE billing_plan
    ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 0;

ALTER TABLE billing_plan
    ADD COLUMN IF NOT EXISTS effective_start_date DATE;

ALTER TABLE billing_plan
    ADD COLUMN IF NOT EXISTS effective_end_date DATE;

-- Backfill NULL priorities to the default value
UPDATE billing_plan
SET priority = COALESCE(priority, 0)
WHERE priority IS NULL;
