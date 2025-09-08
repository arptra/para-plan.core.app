CREATE TABLE logs (id bigserial, ts date NOT NULL, msg text) PARTITION BY RANGE (ts);
    DO $$
    DECLARE d date := (CURRENT_DATE - INTERVAL '365 days')::date;
    BEGIN
      WHILE d <= CURRENT_DATE LOOP
        EXECUTE format('CREATE TABLE logs_%s PARTITION OF logs FOR VALUES FROM (%L) TO (%L);',
                       to_char(d,'YYYYMMDD'), d, d+1);
        d := d + 1;
      END LOOP;
    END$$;
    INSERT INTO logs(ts, msg)
    SELECT (CURRENT_DATE - (random()*120)::int)::date, md5(random()::text)
    FROM generate_series(1, 1000000);
    ANALYZE logs;
