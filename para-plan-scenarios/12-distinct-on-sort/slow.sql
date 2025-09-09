SELECT DISTINCT ON (k) k, ts, val
    FROM metrics
    WHERE ts >= now() - interval '30 days'
    ORDER BY k, ts DESC;
