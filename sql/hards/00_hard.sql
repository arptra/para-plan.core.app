WITH cte_orders AS (
  SELECT o.id, o.customer_id, o.created_at, o.amount
  FROM orders o
  -- антипаттерн: функция на колонке мешает pruning/индексу
  WHERE date_trunc('month', o.created_at) = DATE '2025-08-01'
    AND o.amount > 100
),
cte_agg AS (
  SELECT oi.product_id, count(*) AS cnt
  FROM cte_orders co
  JOIN order_items oi ON oi.order_id = co.id
  GROUP BY oi.product_id
  HAVING count(*) > 10
)
SELECT DISTINCT ON (c.id)
       c.id,
       c.name,
       -- коррелированный подзапрос (антипаттерн)
       (SELECT max(o2.created_at)
        FROM orders o2
        WHERE o2.customer_id = c.id) AS last_order_at
FROM customers c
JOIN cte_orders co ON co.customer_id = c.id
LEFT JOIN cte_agg a ON a.product_id = co.id % 100  -- искусственный «грязный» join-key
WHERE lower(c.email) LIKE 'vip%'              -- функция + LIKE без покрытия
ORDER BY c.id, lower(c.email);
