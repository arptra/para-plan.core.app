CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE TABLE clicks(id bigserial, user_id bigint, ts timestamptz, page_id bigint);
    INSERT INTO clicks(user_id, ts, page_id)
    SELECT (random()*100000)::bigint, now() - ((random()*120)::int || ' days')::interval, (random()*1000)::bigint
    FROM generate_series(1, 2000000);
    CREATE INDEX IF NOT EXISTS idx_clicks_page_ts ON clicks(page_id, ts);
    ANALYZE clicks;
