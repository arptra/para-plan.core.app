#!/usr/bin/env bash
set -euo pipefail
docker compose up -d
CID=$(docker compose ps -q pg)
until docker exec "$CID" pg_isready -U paraplan -d demo >/dev/null 2>&1; do sleep 1; done
psql "postgresql://paraplan:paraplan@localhost:5406/demo" -f init.sql >/dev/null

( psql "postgresql://paraplan:paraplan@localhost:5406/demo" <<'SQL'
\set QUIET 1
BEGIN;
UPDATE acc SET balance=balance+10 WHERE id=1;
SELECT pg_sleep(15);
SQL
) &
sleep 1

echo "Trying victim SELECT ... FOR UPDATE (expect wait/timeout)"
timeout 3s psql "postgresql://paraplan:paraplan@localhost:5406/demo" -c "$(cat slow.sql)" || echo "OK: blocked"

b64=$(printf "%s" "SELECT * FROM acc WHERE id = 1 FOR UPDATE" | base64 -w0 2>/dev/null || printf "%s" "SELECT * FROM acc WHERE id = 1 FOR UPDATE" | base64 -b 0)
curl -s -X POST http://localhost:8080/api/analyze -H "Content-Type: application/json" --data '{"sqlB64":"'"$b64"'"}' | jq '.advice,.recommendations'

echo "Apply fix (terminate blocking session or victim)"
psql "postgresql://paraplan:paraplan@localhost:5406/demo" -f fix.sql || true

docker compose down
echo "Done."
