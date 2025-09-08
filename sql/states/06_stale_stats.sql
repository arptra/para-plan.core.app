-- Make stats stale by bulk insert without analyze
INSERT INTO customers(name,email,region_id)
SELECT 'Skew '||g,'hot'||g||'@example.com',1 FROM generate_series(1, 20000) g;
