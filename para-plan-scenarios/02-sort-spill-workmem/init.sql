CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE TABLE events(id bigserial, ts timestamptz NOT NULL, payload text);
    INSERT INTO events(ts, payload)
    SELECT now() - ((random()*400)::int || ' days')::interval, md5(random()::text)
    FROM generate_series(1, 2000000);
    ANALYZE events;
    ALTER SYSTEM SET work_mem = '1MB';
    SELECT pg_reload_conf();
