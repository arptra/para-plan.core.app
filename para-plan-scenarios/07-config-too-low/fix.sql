ALTER SYSTEM SET work_mem = '64MB';
    ALTER SYSTEM SET effective_cache_size = '2GB';
    SELECT pg_reload_conf();
