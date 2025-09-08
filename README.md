# PARA-PLAN — доказательство работоспособности анализатора

Этот README содержит **полный набор SQL-запросов**, **сценарии моделирования состояний БД** и **скрипты проверки**, демонстрирующие,
что анализатор (`/api/analyze`) корректно:
- оценивает «стоимость» и риск запросов **до выполнения** (через `EXPLAIN (FORMAT JSON)`),
- даёт **конкретные рекомендации** (индексы, конфигурация, переписывание запросов),
- чувствителен к **статистике, индексам, work_mem, планировочным GUC**, и т.д.

> Примечание: в прод-условиях анализатор может работать в read-only (без DDL/DML). Здесь мы показываем *демонстрационные*
скрипты изменения состояния таблиц в **тестовой базе**, чтобы доказать корректность предсказаний и рекомендаций.

--- 

```
1) Инициализация демо-схемы
export PGURL='postgres://user:pass@localhost:5432/dbname'
psql "$PGURL" -f sql/init.sql
2) Запуск приложения
./gradlew bootRun
3) Прогон всех запросов через /api/analyze
bash scripts/run-all.sh
результаты: results/*.json
4) Примеры доказательств до/после
bash scripts/demo-index-time.sh
bash scripts/demo-work-mem.sh
```
bash scripts/demo-analyze.sh


## 0) Быстрый старт

1. Инициализировать тестовую схему (если используешь Testcontainers — файл уже выполняется автоматически):
```sql
\i sql/init.sql
```

2. Запустить приложение (порт 8080):
```bash
./gradlew bootRun
```

3. Проверить один запрос:
```bash
curl -s -X POST http://localhost:8080/api/analyze \  -H 'Content-Type: application/json' \  -d '{"sql":"SELECT id, name FROM customers WHERE email ILIKE '%@example.com' ORDER BY id DESC LIMIT 50 OFFSET 10","options":{"enableLandscape":true,"enableDcc":true,"mcSamples":25}}' | jq
```

---

## 1) Полный набор SQL (готов к /api/analyze)

Ниже — **исчерпывающий** список запросов, покрывающий основные и продвинутые возможности PostgreSQL.
Каждый из них можно отправить как:

```json
{
  "sql": "<ЗАПРОС>",
  "options": { "enableLandscape": true, "enableDcc": true, "mcSamples": 25 }
}
```

### 1.1 SELECT/ORDER BY/LIMIT/OFFSET/Fetch
- `SELECT id, name FROM customers WHERE email ILIKE '%@example.com' ORDER BY id DESC LIMIT 50 OFFSET 10;`
- `SELECT o.* FROM orders o WHERE o.created_at BETWEEN now() - interval '30 days' AND now() ORDER BY o.amount DESC LIMIT 200;`
- `SELECT id FROM orders ORDER BY id FETCH FIRST 100 ROWS ONLY;`

### 1.2 JOIN (INNER/LEFT/RIGHT/FULL/CROSS)
- `SELECT o.id, c.name FROM orders o INNER JOIN customers c ON c.id=o.customer_id WHERE o.created_at >= now() - interval '14 days';`
- `SELECT o.id, c.name FROM orders o LEFT JOIN customers c ON c.id=o.customer_id ORDER BY o.id DESC LIMIT 100;`
- `SELECT * FROM customers c RIGHT JOIN orders o ON o.customer_id=c.id WHERE c.region_id = 1 LIMIT 100;`
- `SELECT * FROM customers c FULL JOIN orders o ON o.customer_id=c.id WHERE (c.id IS NULL OR o.id IS NULL) LIMIT 100;`
- `SELECT * FROM customers c CROSS JOIN regions r WHERE r.id = c.region_id AND c.id < 100;`

### 1.3 LATERAL
- `SELECT c.id, x.total FROM customers c LEFT JOIN LATERAL (SELECT SUM(oi.qty*i.price) total FROM orders o JOIN order_items oi ON oi.order_id=o.id JOIN items i ON i.id=oi.item_id WHERE o.customer_id=c.id LIMIT 1) x ON true WHERE c.id < 500;`

### 1.4 CTE / WITH
- `WITH recent AS (SELECT * FROM orders WHERE created_at > now() - interval '7 days') SELECT r.customer_id, COUNT(*) FROM recent r GROUP BY r.customer_id HAVING COUNT(*)>1 ORDER BY COUNT(*) DESC LIMIT 50;`

### 1.5 EXISTS / IN / ANY / ALL
- `SELECT id FROM customers c WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id=c.id AND o.amount > 100);`
- `SELECT id FROM customers WHERE id IN (SELECT customer_id FROM orders WHERE status='PAID' LIMIT 100);`
- `SELECT id FROM customers WHERE id = ANY (SELECT customer_id FROM orders WHERE amount > 500);`
- `SELECT id FROM customers WHERE 1000 > ALL (SELECT amount FROM orders WHERE orders.customer_id = customers.id);`

### 1.6 Агрегации, GROUP BY, HAVING
- `SELECT c.region_id, COUNT(*) cnt, SUM(o.amount) sum_a FROM customers c JOIN orders o ON o.customer_id=c.id GROUP BY c.region_id HAVING SUM(o.amount) > 1000 ORDER BY sum_a DESC;`

### 1.7 Оконные функции + окна с рамками
- `SELECT o.id, o.customer_id, o.amount, rank() OVER (PARTITION BY o.customer_id ORDER BY o.amount DESC) rnk FROM orders o WHERE o.created_at >= now() - interval '30 days';`
- `SELECT o.id, o.amount, avg(o.amount) OVER (ORDER BY o.created_at ROWS BETWEEN 10 PRECEDING AND CURRENT ROW) AS avg_win FROM orders o WHERE o.created_at >= now() - interval '60 days';`

### 1.8 DISTINCT / DISTINCT ON
- `SELECT DISTINCT c.region_id FROM customers c;`
- `SELECT DISTINCT ON (o.customer_id) o.customer_id, o.id, o.amount FROM orders o ORDER BY o.customer_id, o.amount DESC;`

### 1.9 UNION / INTERSECT / EXCEPT
- `SELECT id FROM customers WHERE region_id=1 UNION ALL SELECT id FROM customers WHERE region_id=2;`
- `SELECT customer_id FROM orders WHERE amount>500 INTERSECT SELECT customer_id FROM orders WHERE status='PAID';`
- `SELECT customer_id FROM orders EXCEPT SELECT customer_id FROM orders WHERE status='CANCELLED';`

### 1.10 CASE / COALESCE / NULLIF / CAST
- `SELECT id, CASE WHEN amount>1000 THEN 'high' WHEN amount>100 THEN 'mid' ELSE 'low' END AS bucket FROM orders ORDER BY id DESC LIMIT 200;`
- `SELECT CAST(id AS BIGINT), COALESCE(name,'N/A') FROM customers WHERE NULLIF(email,'') IS NOT NULL LIMIT 100;`

### 1.11 SIMILAR TO / ILIKE / BETWEEN
- `SELECT * FROM customers WHERE name SIMILAR TO '(User|Admin) %' AND id BETWEEN 100 AND 200;`
- `SELECT * FROM orders WHERE status ILIKE 'PAI%' AND amount BETWEEN 50 AND 150 ORDER BY created_at DESC LIMIT 100;`

### 1.12 Массивы / UNNEST
- `SELECT i.id FROM items i WHERE 'tag1' = ANY(i.tags);`
- `SELECT i.id, t FROM items i, unnest(i.tags) AS t WHERE t LIKE 'grp%';`

### 1.13 JSON/JSONB (операторы @>, ->>, ?)
- `SELECT json_build_object('id', c.id, 'email', c.email) FROM customers c WHERE c.id < 50;`
- `SELECT id FROM items WHERE meta->>'color' = 'red';`
- `SELECT id FROM items WHERE meta @> '{"color":"green"}';`

### 1.14 VIEW
- `SELECT id FROM items WHERE meta ? 'weight';`

### 1.15 DML под EXPLAIN: INSERT / UPDATE / DELETE / CTE+UPDATE
- `SELECT * FROM v_order_value WHERE calc_amount > 500 ORDER BY calc_amount DESC LIMIT 50;`
- `INSERT INTO orders (customer_id, created_at, amount, status) SELECT c.id, now(), 9.99, 'NEW' FROM customers c WHERE c.id < 5;`
- `UPDATE orders SET amount = amount * 1.01 WHERE status='PAID' AND created_at >= now() - interval '3 days';`
- `DELETE FROM orders WHERE status='CANCELLED' AND created_at < now() - interval '90 days';`

### 1.16 Продвинутые агрегации: FILTER, ROLLUP, CUBE
- `WITH top_c AS (SELECT id FROM customers ORDER BY id DESC LIMIT 10) UPDATE orders SET status='SHIPPED' WHERE customer_id IN (SELECT id FROM top_c);`
- `SELECT region_id, COUNT(*) FILTER (WHERE email ILIKE '%@example.com') AS ex_cnt FROM customers GROUP BY region_id;`
- `SELECT region_id, sum(amount) FROM customers c JOIN orders o ON o.customer_id=c.id GROUP BY ROLLUP (region_id);`

### 1.17 Регулярки/строки
- `SELECT region_id, status, sum(amount) FROM customers c JOIN orders o ON o.customer_id=c.id GROUP BY CUBE (region_id, status) LIMIT 100;`
- `SELECT id FROM customers WHERE email ~* '.*@mail\\.local$' LIMIT 100;`

### 1.18 ARRAY_AGG / JSONB_AGG
- `SELECT substring(email from '^(.*)@') AS local FROM customers LIMIT 100;`
- `SELECT customer_id, array_agg(id ORDER BY created_at DESC) FROM orders GROUP BY customer_id LIMIT 50;`

### 1.19 MERGE (PostgreSQL 15+)
- `SELECT customer_id, jsonb_agg(jsonb_build_object('id', id, 'amount', amount)) FROM orders GROUP BY customer_id LIMIT 50;`

> Если тебе нужен ещё больший охват (`GROUPING SETS`, `WINDOW RANGE BETWEEN`, `DISTINCT` в агрегатах, `tsvector`/`GIN` и т.п.) — можно расширить список аналогично.

---

## 2) Моделирование состояний БД (доказуемые эффекты)

Каждый сценарий выполнится командой:
```bash
psql "$PGURL" -f sql/states/<script>.sql
```
После — отправляем те же запросы в `/api/analyze` и сравниваем **predicted/features/landscape/recommendations**.

### 2.1 Индексы времени
- `sql/states/01_index_time.sql`  (создаём B-Tree по `orders.created_at`)
- Эффект: планы для «последних N дней» переходят на Index/Bitmap; падают `predicted.p50ms/p95ms`, снижается `ioRisk`.

### 2.2 Частичный индекс под «горячий» путь
- `sql/states/03_partial_paid_recent.sql`
- Эффект: запросы по `status='PAID'` и свежим периодам значительно дешевле, рекомендации подтверждают эффект.

### 2.3 BRIN для больших таблиц по времени
- `sql/states/04_brin_time.sql`
- Эффект: для больших диапазонов — BRIN + Bitmap Heap, резкое падение чтений.

### 2.4 Скос в данных (селективность)
- `sql/states/05_skew_status.sql`
- Эффект: меняется селективность предиката по `status`; анализатор предложит частичный индекс/переписку запроса.

### 2.5 Устаревшая статистика vs ANALYZE
- `sql/states/06_stale_stats.sql` затем `sql/states/07_analyze_customers.sql`
- Эффект: **до** ANALYZE — неверные оценки `rows`, выше `variance/regret`; **после** — корректно.

### 2.6 default_statistics_target (точность гистограмм)
- `sql/states/09_set_stats_target_small.sql` vs `sql/states/10_set_stats_target_large.sql`
- Эффект: с *малой* целью — `variance` растёт; с *большой* — падает, планы стабильнее.

### 2.7 work_mem и temp spills (демо)
- `sql/states/11_work_mem_small.sql` vs `sql/states/12_work_mem_large.sql`
- Эффект: **малый** work_mem ⇒ высокий `tempSpillRisk`; **большой** ⇒ существенно ниже.

### 2.8 VACUUM / bloat
- `sql/states/08_vacuum_orders.sql`
- Эффект: после обслуживания снижается `ioRisk`, лучшие планы и предсказания.

### 2.9 Planner toggles (ландшафт планов)
- `sql/states/13_planner_toggle_hash_off.sql` (в сессии)
- Эффект: `landscape.variants` фиксирует, как растёт стоимость при отключении hash join — показатель «робастности».

---

## 3) Скрипты запуска

### 3.1 Прогон всех запросов (results/*.json)
Создаст `results/NN.json` для каждого запроса.
```bash
bash scripts/run-all.sh
```

### 3.2 Демонстрация «До/После индекса по времени»
```bash
bash scripts/demo-index-time.sh
```

### 3.3 Доказательство влияния work_mem
```bash
bash scripts/demo-work-mem.sh
```

### 3.4 Устаревшая статистика vs ANALYZE
```bash
bash scripts/demo-analyze.sh
```

---

## 4) Что доказывают эти сценарии

1. **Чувствительность к статистике и индексам** — планы и `predicted` меняются осмысленно (2.1–2.6).
2. **Модель I/O и temp spills** — `ioRisk` и `tempSpillRisk` реагируют на `work_mem` и BRIN/BTree (2.3, 2.7).
3. **Робастность планов** — `landscape.variants`, `regret`, `robustnessScore` меняются ожидаемо при GUC-тогглах (2.9).
4. **Рекомендации** — подтверждаются метриками ДО/ПОСЛЕ: индексы, ANALYZE, конфигурация.

---

## 5) API вызов

```bash
curl -s -X POST http://localhost:8080/api/analyze \  -H 'Content-Type: application/json' \  -d '{"sql":"<QUERY>","options":{"enableLandscape":true,"enableDcc":true,"mcSamples":25}}' | jq
```

> Для батч-запуска используй `scripts/run-all.sh` — он обойдёт все запросы из `scenarios/*.json`.
