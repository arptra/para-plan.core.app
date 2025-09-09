SELECT user_id, page_id, ts
    FROM clicks
    WHERE user_id = 42
    ORDER BY ts DESC
    LIMIT 100;
