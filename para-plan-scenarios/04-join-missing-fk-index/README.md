# JOIN без индекса по внешнему ключу

**Что не так:** orders(user_id) не проиндексирован.

**Как проявляется:** Hash Join с большим build/seq scan; долго.

**Что обнаружит анализатор:** Совет INDEX на FK и на колонку фильтра по дате.

**Исправление:** Создать индексы orders(user_id), orders(created_at).

## Как запустить
```bash
docker compose up -d
./demo.sh
```
