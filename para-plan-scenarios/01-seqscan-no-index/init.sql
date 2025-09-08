CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE TABLE orders(
      id bigserial PRIMARY KEY,
      user_id bigint,
      amount numeric(12,2),
      created_at timestamptz NOT NULL
    );
    INSERT INTO orders(user_id, amount, created_at)
    SELECT (random()*100000)::bigint,
           round((random()*1000)::numeric,2),
           now() - ((random()*365)::int || ' days')::interval
    FROM generate_series(1, 1500000);
    ANALYZE orders;
