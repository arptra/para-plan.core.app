SELECT grp, sum(val)
    FROM big
    WHERE created >= now() - interval '180 days'
    GROUP BY grp
    ORDER BY sum(val) DESC
    LIMIT 20;
