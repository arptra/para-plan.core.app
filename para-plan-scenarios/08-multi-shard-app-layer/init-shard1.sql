CREATE TABLE sales(id bigserial, region text, amount numeric, created timestamptz);
INSERT INTO sales(region, amount, created)
SELECT 'east', random()*1000, now() - ((random()*200)::int || ' days')::interval
FROM generate_series(1, 600000);
ANALYZE sales;
