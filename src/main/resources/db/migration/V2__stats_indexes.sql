-- Counting applications by status.
create index application_status_idx on application (status);

-- The response rate reads (application_id, to_status) from every transition. With both columns in one
-- index, PostgreSQL can answer from the index alone. It also serves lookups by application_id, so the
-- single-column index from V1 is no longer needed.
create index status_transition_application_id_to_status_idx on status_transition (application_id, to_status);
drop index status_transition_application_id_idx;
