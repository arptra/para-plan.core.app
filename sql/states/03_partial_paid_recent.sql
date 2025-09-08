CREATE INDEX IF NOT EXISTS idx_orders_paid_recent ON orders (created_at) WHERE status='PAID';
ANALYZE orders;
