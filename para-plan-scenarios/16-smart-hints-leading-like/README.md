# Подсказка: ведущий wildcard в LIKE

**Что не так:** поиск `%foo%` не использует индекс.

**Что обнаружит `/api/sql-hints`:** подсказка убрать ведущий wildcard и заменить `SELECT *`.

## Как проверить
```bash
docker-compose up -d
./demo.sh
```
