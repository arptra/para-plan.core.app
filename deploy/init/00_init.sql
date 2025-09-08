
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS orders CASCADE;

CREATE TABLE customers (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL
);

CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  customer_id BIGINT NOT NULL REFERENCES customers(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  amount NUMERIC(12,2) NOT NULL
);

INSERT INTO customers(name, email)
SELECT 'User '||g, CASE WHEN g % 10 = 0 THEN 'foo' || g || '@example.com' ELSE 'user'||g||'@mail.local' END
FROM generate_series(1, 50000) g;

INSERT INTO orders(customer_id, created_at, amount)
SELECT (random()*49999)::int + 1, now() - (random()*interval '90 days'), round((random()*1000)::numeric,2)
FROM generate_series(1, 300000);

ANALYZE;
