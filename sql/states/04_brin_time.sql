DROP INDEX IF EXISTS idx_orders_created_brin;
CREATE INDEX idx_orders_created_brin ON orders USING brin (created_at);
ANALYZE orders;
