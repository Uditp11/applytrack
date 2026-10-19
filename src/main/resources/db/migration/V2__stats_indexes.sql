-- The statistics read every row, so PostgreSQL answers them with a table scan; no index makes that faster.
-- These indexes serve the filtered lookups next to them (checked with EXPLAIN on 5,000 applications).

-- GET /api/applications?status=DRAFT finds the few rows in one status without reading the whole table.
create index application_status_idx on application (status);

-- One application's history, and the cascade delete. The two-column index also covers every lookup by
-- application_id alone, so the single-column index from V1 is no longer needed.
create index status_transition_application_id_to_status_idx on status_transition (application_id, to_status);
drop index status_transition_application_id_idx;
