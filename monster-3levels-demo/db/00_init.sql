-- 00_init.sql — «тяжёлый» сценарий для трёхэтажного запроса

-- 1) Чистка (идемпотентный запуск)
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS customers CASCADE;

-- 2) Схема
CREATE TABLE customers (
  id           BIGSERIAL PRIMARY KEY,
  name         TEXT NOT NULL,
  email        TEXT NOT NULL
);

CREATE TABLE orders (
  id           BIGSERIAL PRIMARY KEY,
  customer_id  BIGINT NOT NULL REFERENCES customers(id),
  created_at   TIMESTAMPTZ NOT NULL,
  amount       NUMERIC(12,2) NOT NULL
);

CREATE TABLE order_items (
  id           BIGSERIAL PRIMARY KEY,
  order_id     BIGINT NOT NULL REFERENCES orders(id),
  product_id   INT NOT NULL,
  qty          INT NOT NULL,
  price        NUMERIC(12,2) NOT NULL
);

-- 3) Параметры нагрузки
-- кол-во клиентов ~50k
WITH params AS (
  SELECT 50000::int AS n_customers
)
INSERT INTO customers (name, email)
SELECT
  'Customer ' || i,
  CASE WHEN random() < 0.02 THEN 'vip' || i || '@example.com'
       ELSE 'user' || i || '@example.com' END
FROM params, generate_series(1, (SELECT n_customers FROM params)) AS g(i);

-- 3.2) Диапазон дат (2024-01-01..2025-09-01) с нормализованной долей frac
WITH date_span AS (
  SELECT
    ts::date AS d,
    EXTRACT(EPOCH FROM ts - TIMESTAMP '2024-01-01')
      / EXTRACT(EPOCH FROM TIMESTAMP '2025-09-01' - TIMESTAMP '2024-01-01') AS frac
  FROM generate_series(
    TIMESTAMP '2024-01-01',
    TIMESTAMP '2025-09-01',
    interval '1 day'
  ) ts
),
params AS (
  SELECT
    (SELECT count(*) FROM customers) AS n_customers,
    800000::int AS n_orders,
    2.4::float  AS items_per_order_avg
)
INSERT INTO orders (customer_id, created_at, amount)
SELECT
  (SELECT id FROM customers ORDER BY random() LIMIT 1),
  (SELECT (d + make_interval(hours := (random() * 23)::int, minutes := (random() * 59)::int))::timestamptz
   FROM date_span
   ORDER BY pow(frac, 0.7) * random() DESC
   LIMIT 1),
  ROUND( CASE
           WHEN random() < 0.15 THEN 20 + random()*60
           WHEN random() < 0.75 THEN 80 + random()*120
           ELSE 200 + random()*800
         END , 2)
FROM params, generate_series(1, (SELECT n_orders FROM params)) AS g(i);

-- 3.4) Генерация order_items (~ n_orders * (1..5), среднее ~2.4)
INSERT INTO order_items (order_id, product_id, qty, price)
SELECT
  o.id,
  (1 + (random()*5000)::int),
  GREATEST(1, (random()*4)::int),
  ROUND( (10 + random()*90)::numeric, 2 )
FROM orders o
JOIN LATERAL generate_series(1, 1 + (random()*4)::int) AS gs(n) ON TRUE;

-- 4) Немного «грязи» для разнообразия email
UPDATE customers
SET email = 'VIP' || id || '@corp.local'
WHERE random() < 0.001;

-- 5) Контрольные подсчёты
DO $$
DECLARE
  c1 BIGINT; c2 BIGINT; c3 BIGINT;
  v1 BIGINT; v2 BIGINT;
BEGIN
  SELECT count(*) INTO c1 FROM customers;
  SELECT count(*) INTO c2 FROM orders;
  SELECT count(*) INTO c3 FROM order_items;

  SELECT count(*) INTO v1 FROM customers WHERE lower(email) LIKE 'vip%';
  SELECT count(*) INTO v2 FROM orders WHERE amount > 100;

  RAISE NOTICE 'customers=%  orders=%  order_items=%', c1, c2, c3;
  RAISE NOTICE 'vip_emails=%  (%.2f%% of customers)', v1, 100.0 * v1 / c1;
  RAISE NOTICE 'orders_amount_gt_100=%  (%.2f%% of orders)', v2, 100.0 * v2 / c2;
END$$;
