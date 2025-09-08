-- init.sql — «тяжёлый» сценарий для трёхэтажного запроса

-- 1) Чистка (идемпотентный запуск)
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS customers CASCADE;

-- 2) Схема
CREATE TABLE customers (
  id           BIGSERIAL PRIMARY KEY,
  name         TEXT NOT NULL,
  email        TEXT NOT NULL
  -- ВАЖНО: без email_lower и без индексов — это «до»
);

CREATE TABLE orders (
  id           BIGSERIAL PRIMARY KEY,
  customer_id  BIGINT NOT NULL REFERENCES customers(id),
  created_at   TIMESTAMPTZ NOT NULL,
  amount       NUMERIC(12,2) NOT NULL
  -- ВАЖНО: нет индекса по (customer_id, created_at), это «до»
);

CREATE TABLE order_items (
  id           BIGSERIAL PRIMARY KEY,
  order_id     BIGINT NOT NULL REFERENCES orders(id),
  product_id   INT NOT NULL,
  qty          INT NOT NULL,
  price        NUMERIC(12,2) NOT NULL
  -- ВАЖНО: нет индекса по (order_id), это «до»
);

-- 3) Параметры нагрузки (меняйте при необходимости)
-- кол-во клиентов ~50k
WITH params AS (
  SELECT 50000::int AS n_customers
)
-- 3.1) Генерация customers
INSERT INTO customers (name, email)
SELECT
  'Customer ' || i,
  CASE
    WHEN random() < 0.02  -- ~2% VIP
      THEN 'vip' || i || '@example.com'
    ELSE 'user' || i || '@example.com'
  END AS email
FROM params, generate_series(1, (SELECT n_customers FROM params)) AS g(i);

-- 3.2) Вспомогательный диапазон дат (2024-01-01..2025-09-01)
--   будем генерить заказы с перекосом по недавним месяцам
WITH date_span AS (
  SELECT
    ts::date AS d,
    extract(epoch FROM (ts - date '2024-01-01')) / extract(epoch FROM (date '2025-09-01' - date '2024-01-01')) AS frac
  FROM generate_series(timestamp '2024-01-01', timestamp '2025-09-01', interval '1 day') ts
),
params AS (
  SELECT
    (SELECT count(*) FROM customers) AS n_customers,
    800000::int AS n_orders,      -- ~800k заказов
    2.4::float  AS items_per_order_avg  -- в среднем 2.4 позиции
)

-- 3.3) Генерация orders (~800k)
-- распределим заказы по пользователям и датам с лёгким перекосом к недавнему времени
INSERT INTO orders (customer_id, created_at, amount)
SELECT
  -- случайный клиент (равномерно)
  (SELECT id FROM customers ORDER BY random() LIMIT 1),
  -- дата: чем ближе к концу периода, тем немного чаще (frac^0.7)
  (SELECT (d + make_interval(hours := (random() * 23)::int, minutes := (random() * 59)::int))::timestamptz
   FROM date_span
   ORDER BY pow(frac, 0.7) * random() DESC  -- перекос к «свежим» датам
   LIMIT 1),
  -- сумма: смесь распределений, чтобы amount>100 выбирал ощутимую долю
  ROUND( CASE
           WHEN random() < 0.15 THEN 20 + random()*60     -- малые заказы
           WHEN random() < 0.75 THEN 80 + random()*120    -- средние (~100-200)
           ELSE 200 + random()*800                        -- крупные
         END , 2)
FROM params, generate_series(1, (SELECT n_orders FROM params)) AS g(i);

-- 3.4) Генерация order_items (~ n_orders * (1..5), среднее ~2.4)
-- Каждой строке orders добавим 1..5 позиций
INSERT INTO order_items (order_id, product_id, qty, price)
SELECT
  o.id,
  (1 + (random()*5000)::int),                -- ~5k разных продуктов
  GREATEST(1, (random()*4)::int),            -- 1..4 шт
  ROUND( (10 + random()*90)::numeric, 2 )    -- цена 10..100
FROM orders o
JOIN LATERAL generate_series(1, 1 + (random()*4)::int) AS gs(n) ON TRUE;

-- 4) Дополнительно: немного «грязных данных» и разброса по email
--   чтобы lower(email) LIKE 'vip%' находил и не-VIP кейсы (переименования)
UPDATE customers
SET email = 'VIP' || id || '@corp.local'
WHERE random() < 0.001;  -- ~0.1% случайных VIP в другом домене

-- 5) Контрольные подсчёты (для sanity-check)
DO $$
DECLARE
  c1 BIGINT; c2 BIGINT; c3 BIGINT;
  v1 BIGINT; v2 BIGINT;
BEGIN
  SELECT count(*) INTO c1 FROM customers;
  SELECT count(*) INTO c2 FROM orders;
  SELECT count(*) INTO c3 FROM order_items;

  -- сколько клиентов с vip*?
  SELECT count(*) INTO v1 FROM customers WHERE lower(email) LIKE 'vip%';
  -- доля заказов amount > 100
  SELECT count(*) INTO v2 FROM orders WHERE amount > 100;

  RAISE NOTICE 'customers=%  orders=%  order_items=%', c1, c2, c3;
  RAISE NOTICE 'vip_emails=%  (%.2f%% of customers)', v1, 100.0 * v1 / c1;
  RAISE NOTICE 'orders_amount_gt_100=%  (%.2f%% of orders)', v2, 100.0 * v2 / c2;
END$$;

-- 6) Никаких полезных индексов и ANALYZE — это «до»
-- При демонстрации «после»:
--   * добавить функциональный столбец/индекс по lower(email)
--   * индекс по (customer_id, created_at DESC) на orders
--   * индекс по order_items(order_id)
--   * ANALYZE; и выполнить переписанный запрос
