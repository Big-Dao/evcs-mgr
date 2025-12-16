-- Minimal schema for evcs-protocol controller tests.
-- BaseControllerTest expects this file to exist and contain at least one executable statement.

CREATE TABLE IF NOT EXISTS __evcs_protocol_test_noop (
	id INT PRIMARY KEY
);
