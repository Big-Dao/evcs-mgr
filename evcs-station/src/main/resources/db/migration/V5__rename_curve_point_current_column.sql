-- Rename reserved keyword column `current` to `current_a`.
--
-- Motivation:
-- - `current` is a SQL keyword in JSqlParser / MyBatis-Plus tenant interceptor parsing,
--   which can break INSERT parsing and thus fail multi-tenant SQL rewriting.
--
-- Safe for fresh installs:
-- - If V4 created `current`, V5 renames it.
-- - If DB already has `current_a`, this migration will fail; adjust manually if needed.

DO $$
BEGIN
	IF EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = current_schema()
			AND table_name = 'charger_connector_curve_point'
			AND column_name = 'current'
	)
	AND NOT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = current_schema()
			AND table_name = 'charger_connector_curve_point'
			AND column_name = 'current_a'
	)
	THEN
		ALTER TABLE charger_connector_curve_point
			RENAME COLUMN current TO current_a;
	END IF;
END $$;
