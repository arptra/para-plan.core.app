CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE TABLE big_table(id bigserial, name text);
INSERT INTO big_table(name)
SELECT md5(random()::text) FROM generate_series(1, 200000);
ANALYZE big_table;
