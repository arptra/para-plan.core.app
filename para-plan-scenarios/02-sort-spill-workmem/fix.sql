CREATE INDEX IF NOT EXISTS idx_events_ts_desc ON events(ts DESC);
    ANALYZE events;
    -- Optionally also: ALTER SYSTEM SET work_mem = '64MB'; SELECT pg_reload_conf();
