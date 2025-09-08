-- 02_fix.sql — индексы + переписанный запрос (быстро)
\timing on

-- 1) Функциональный столбец и индекс для email_lower
ALTER TABLE customers
  ADD COLUMN IF NOT EXISTS email_lower TEXT GENERATED ALWAYS AS (lower(email)) STORED;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                 WHERE c.relname='idx_customers_email_lower' AND n.nspname=current_schema()) THEN
    EXECUTE 'CREATE INDEX idx_customers_email_lower ON customers(email_lower)';
  END IF;
END$$;

-- 2) Покрывающий индекс для DISTINCT ON + JOIN
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                 WHERE c.relname='idx_orders_cust_created' AND n.nspname=current_schema()) THEN
    EXECUTE 'CREATE INDEX idx_orders_cust_created ON orders(customer_id, created_at DESC)';
  END IF;
END$$;

-- 3) Индекс для order_items(order_id)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                 WHERE c.relname='idx_order_items_order_id' AND n.nspname=current_schema()) THEN
    EXECUTE 'CREATE INDEX idx_order_items_order_id ON order_items(order_id)';
  END IF;
END$$;

-- 4) Обновить статистику (важно для глубокого плана)
ANALYZE;

-- 5) Переписанный запрос
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
WITH cte_orders AS (
  SELECT o.id, o.customer_id, o.created_at, o.amount
  FROM orders o
  WHERE o.created_at >= DATE '2025-08-01'
    AND o.created_at <  DATE '2025-09-01'    -- ✨ диапазон вместо date_trunc
    AND o.amount > 100
),
last_orders AS (
  SELECT customer_id, max(created_at) AS last_order_at
  FROM orders
  GROUP BY customer_id
)
SELECT DISTINCT ON (c.id)
       c.id,
       c.name,
       lo.last_order_at
FROM customers c
JOIN cte_orders co ON co.customer_id = c.id
LEFT JOIN last_orders lo ON lo.customer_id = c.id
WHERE c.email_lower LIKE 'vip%'
ORDER BY c.id, c.email_lower;
