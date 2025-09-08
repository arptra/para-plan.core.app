SELECT id, sku FROM items
    WHERE updated_at >= now() - interval '7 days'
    ORDER BY updated_at DESC
    LIMIT 200;
