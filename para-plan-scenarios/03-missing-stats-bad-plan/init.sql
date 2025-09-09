CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE TABLE t(a int, b int, c text);
    INSERT INTO t
    SELECT (random()*10)::int, (random()*1000)::int, md5(random()::text)
    FROM generate_series(1, 1500000);
    -- deliberately no ANALYZE yet
