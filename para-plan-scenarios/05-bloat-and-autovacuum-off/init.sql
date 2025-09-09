CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
    CREATE TABLE items(id bigserial PRIMARY KEY, sku text, price numeric, updated_at timestamptz);
    INSERT INTO items(sku, price, updated_at)
    SELECT 'sku-'||g, round((random()*1000)::numeric,2), now()
    FROM generate_series(1, 100000);

    DO $$
    BEGIN
      FOR i IN 1..40 LOOP
        UPDATE items SET price = price + (random()*10)::int, updated_at = now();
        DELETE FROM items WHERE random() < 0.04;
        INSERT INTO items(sku, price, updated_at)
        SELECT 'sku-'||g, round((random()*1000)::numeric,2), now()
        FROM generate_series(1, 4000);
      END LOOP;
    END$$;
    ANALYZE items;
