CREATE INDEX IF NOT EXISTS idx_metrics_k_ts_desc ON metrics(k, ts DESC);
    ANALYZE metrics;
