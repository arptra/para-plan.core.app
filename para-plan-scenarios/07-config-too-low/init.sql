CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE TABLE big(id bigserial, grp int, val numeric, created timestamptz);
    INSERT INTO big(grp, val, created)
    SELECT (random()*100)::int, random()*1000, now() - ((random()*400)::int || ' days')::interval
    FROM generate_series(1, 2000000);
    ANALYZE big;
