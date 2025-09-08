CREATE INDEX IF NOT EXISTS idx_clicks_user_ts_desc ON clicks(user_id, ts DESC);
    ANALYZE clicks;
