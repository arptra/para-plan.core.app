# Большой SeqScan без индекса

**Что не так:** Фильтр/сортировка по created_at без индекса → полный обход таблицы.

**Как проявляется:** Высокий I/O, медленный ответ, возможен таймаут.

**Что обнаружит анализатор:** features.seqScan=true, predicted.ioRisk=2, совет INDEX(created_at) и/или покрывающий ORDER BY.

**Исправление:** Создать индекс по created_at (DESC) и выполнить ANALYZE.

## Как запустить
```bash
docker compose up -d
./demo.sh
```
