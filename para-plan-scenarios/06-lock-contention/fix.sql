SELECT pg_terminate_backend(pid)
    FROM pg_stat_activity
    WHERE wait_event_type='Lock' AND query LIKE '%FOR UPDATE%';
