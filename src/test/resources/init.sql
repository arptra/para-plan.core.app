
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS items CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS regions CASCADE;

CREATE TABLE customers (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  region_id INT
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
  tags TEXT[]
);

CREATE TABLE order_items(
  order_id BIGINT NOT NULL REFERENCES orders(id),
  item_id BIGINT NOT NULL REFERENCES items(id),
  qty INT NOT NULL DEFAULT 1,
  PRIMARY KEY(order_id, item_id)
);

CREATE TABLE regions(
  id INT PRIMARY KEY,
  name TEXT NOT NULL
);

INSERT INTO regions(id,name) VALUES (1,'EMEA'),(2,'APAC'),(3,'AMER');

INSERT INTO customers(name, email, region_id)
SELECT 'User '||g,
       CASE WHEN g % 10 = 0 THEN 'foo' || g || '@example.com' ELSE 'user'||g||'@mail.local' END,
       (random()*2)::int + 1
FROM generate_series(1, 15000) g;

INSERT INTO items(sku, price, tags)
SELECT 'SKU-'||g, round((random()*200)::numeric, 2),
       ARRAY['tag'||(g%5), 'grp'||(g%3)]
FROM generate_series(1, 500) g;

INSERT INTO orders(customer_id, created_at, amount, status)
SELECT (random()*14999)::int + 1,
       now() - (random()*interval '120 days'),
       round((random()*2000)::numeric,2),
       (ARRAY['NEW','PAID','CANCELLED','SHIPPED'])[1 + (random()*3)::int]
FROM generate_series(1, 60000);

INSERT INTO order_items(order_id, item_id, qty)
SELECT (random()*59999)::int + 1, (random()*499)::int + 1, (random()*5)::int + 1
FROM generate_series(1, 120000);

CREATE OR REPLACE VIEW v_order_value AS
SELECT o.id, o.customer_id, SUM(oi.qty*i.price) AS calc_amount
FROM orders o
JOIN order_items oi ON oi.order_id=o.id
JOIN items i ON i.id=oi.item_id
GROUP BY o.id, o.customer_id;

ANALYZE;
