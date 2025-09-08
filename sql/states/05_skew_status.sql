UPDATE orders SET status='PAID' WHERE random() < 0.9;
ANALYZE orders;
