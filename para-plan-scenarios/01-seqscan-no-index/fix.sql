CREATE INDEX IF NOT EXISTS idx_orders_created_at_desc ON orders(created_at DESC);
    ANALYZE orders;
