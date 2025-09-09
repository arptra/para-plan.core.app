SELECT count(*) FROM logs WHERE date_trunc('day', ts)::date >= CURRENT_DATE - INTERVAL '30 days';
