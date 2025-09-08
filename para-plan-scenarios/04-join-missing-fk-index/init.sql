CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE TABLE users(id bigserial PRIMARY KEY, email text, signup_at timestamptz);
    INSERT INTO users(email, signup_at)
    SELECT 'u'||g||'@ex.com', now() - ((random()*1000)::int || ' days')::interval
    FROM generate_series(1, 300000);
    ANALYZE users;

    CREATE TABLE orders(id bigserial PRIMARY KEY, user_id bigint, amount numeric, created_at timestamptz);
    INSERT INTO orders(user_id, amount, created_at)
    SELECT (random()*300000)::bigint, round((random()*1000)::numeric,2), now() - ((random()*365)::int || ' days')::interval
    FROM generate_series(1, 2000000);
    ANALYZE orders;
    -- no index on orders(user_id)
