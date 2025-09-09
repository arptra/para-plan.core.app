SELECT u.id, u.email, sum(o.amount) total
    FROM users u
    JOIN orders o ON o.user_id = u.id
    WHERE o.created_at >= now() - interval '90 days'
    GROUP BY u.id, u.email
    ORDER BY total DESC
    LIMIT 50;
