
-- Demo schema for PARA-PLAN
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS items CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS regions CASCADE;

CREATE TABLE regions(
  id INT PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE customers (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  region_id INT REFERENCES regions(id)
);

CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  customer_id BIGINT NOT NULL REFERENCES customers(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  amount NUMERIC(12,2) NOT NULL,
  status TEXT NOT NULL DEFAULT 'NEW'
);

CREATE TABLE items(
  id BIGSERIAL PRIMARY KEY,
  sku TEXT NOT NULL,
  price NUMERIC(10,2) NOT NULL,
  tags TEXT[],
  meta JSONB
);

CREATE TABLE order_items(
  order_id BIGINT NOT NULL REFERENCES orders(id),
  item_id BIGINT NOT NULL REFERENCES items(id),
  qty INT NOT NULL DEFAULT 1,
  PRIMARY KEY(order_id, item_id)
);

INSERT INTO regions(id,name) VALUES (1,'EMEA'),(2,'APAC'),(3,'AMER');

INSERT INTO customers(name, email, region_id)
SELECT 'User '||g,
       CASE WHEN g % 10 = 0 THEN 'foo' || g || '@example.com' ELSE 'user'||g||'@mail.local' END,
       (random()*2)::int + 1
FROM generate_series(1, 20000) g;

INSERT INTO items(sku, price, tags, meta)
SELECT 'SKU-'||g, round((random()*200)::numeric, 2),
       ARRAY['tag'||(g%5), 'grp'||(g%3)],
       jsonb_build_object('color', (ARRAY['red','green','blue'])[1 + (random()*2)::int],
                          'weight', round((random()*10)::numeric,2))
FROM generate_series(1, 800) g;

INSERT INTO orders(customer_id, created_at, amount, status)
SELECT (random()*19999)::int + 1,
       now() - (random()*interval '150 days'),
       round((random()*2000)::numeric,2),
       (ARRAY['NEW','PAID','CANCELLED','SHIPPED'])[1 + (random()*3)::int]
FROM generate_series(1, 90000);

INSERT INTO order_items(order_id, item_id, qty)
SELECT DISTINCT ON (order_id, item_id)
       order_id, item_id, qty
FROM (
    SELECT (random()*89999)::int + 1 AS order_id,
           (random()*799)::int + 1 AS item_id,
           (random()*5)::int + 1 AS qty
    FROM generate_series(1, 180000)
) s
ORDER BY order_id, item_id
ON CONFLICT (order_id, item_id) DO NOTHING;

CREATE OR REPLACE VIEW v_order_value AS
SELECT o.id, o.customer_id, SUM(oi.qty*i.price) AS calc_amount
FROM orders o
JOIN order_items oi ON oi.order_id=o.id
JOIN items i ON i.id=oi.item_id
GROUP BY o.id, o.customer_id;

ANALYZE;
