# Подсказка: нет индекса

**Что не так:** `orders.created_at` не индексирован.

**Что обнаружит `/api/sql-hints`:** рекомендация создать индекс и подсветка столбца в WHERE.

## Как проверить
```bash
docker-compose up -d   # если ещё не поднят PostgreSQL
psql "$PGURL" -f ../../sql/init.sql  # исходная схема без индекса
./demo.sh
```
