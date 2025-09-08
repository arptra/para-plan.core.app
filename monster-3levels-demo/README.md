# Monster 3-level Query Demo

## What it shows
- Deep, multi-CTE query with correlated subquery, functions in filters, and sorting by expression.
- Analyzer should flag: high depth, functions in filters, seq scans, sort nodes, spill risk.
- After fixes (indexes + rewrite), plan shrinks, IO & p95 drop drastically.

## How to run
```bash
docker compose up -d
# or use the helper:
./scripts/run.sh
```

- Output plans: `db/01_slow.out` and `db/02_fix.out`.
- Use your analyzer: POST the SQL from `db/01_slow.sql` and the rewritten part from `db/02_fix.sql` to `/api/analyze`.
