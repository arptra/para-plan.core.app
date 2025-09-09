# Подсказка: устаревшая статистика

**Что не так:** таблица `customers` имеет устаревшую статистику.

**Что обнаружит `/api/sql-hints`:** рекомендация `ANALYZE customers;` и подсветка имени таблицы.

## Как проверить
```bash
docker-compose up -d
psql "$PGURL" -f ../../sql/init.sql
psql "$PGURL" -f ../../sql/states/06_stale_stats.sql
./demo.sh
```
