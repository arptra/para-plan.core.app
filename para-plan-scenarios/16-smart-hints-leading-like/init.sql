CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE TABLE users(id bigserial, name text);
INSERT INTO users(name)
SELECT md5(random()::text) FROM generate_series(1,10000);
ANALYZE users;
