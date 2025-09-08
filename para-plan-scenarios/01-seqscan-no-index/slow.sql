SELECT id, amount
    FROM orders
    WHERE created_at >= now() - interval '30 days'
    ORDER BY created_at DESC
    LIMIT 500;
