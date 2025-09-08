CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE TABLE metrics(id bigserial, k int, ts timestamptz, val numeric);
    INSERT INTO metrics(k, ts, val)
    SELECT (random()*1000)::int,
           now() - ((random()*200)::int || ' days')::interval,
           random()*1000
    FROM generate_series(1, 1500000);
    ANALYZE metrics;
