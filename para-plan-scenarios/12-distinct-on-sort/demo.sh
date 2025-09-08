#!/usr/bin/env bash
set -euo pipefail
echo "Bringing up Postgres on port 5414..."
docker compose up -d
CID=$(docker compose ps -q pg)
echo "Waiting for DB ready..."
until docker exec "$CID" pg_isready -U paraplan -d demo >/dev/null 2>&1; do sleep 1; done

echo "Loading init.sql ..."
psql "postgresql://paraplan:paraplan@localhost:5414/demo" -v ON_ERROR_STOP=1 -f init.sql

echo "---- SLOW PHASE ----"
echo "Trying EXPLAIN (may be slow or timeout)"
psql "postgresql://paraplan:paraplan@localhost:5414/demo" -v ON_ERROR_STOP=0 -c "SET statement_timeout='5s'; EXPLAIN (ANALYZE, BUFFERS) $(cat slow.sql)" || echo "OK: observed slowness/timeout"

echo "Calling analyzer (POST /api/analyze) ..."
b64=$(base64 -w0 slow.sql 2>/dev/null || base64 -b 0 -i slow.sql)
curl -s -X POST http://localhost:8080/api/analyze -H "Content-Type: application/json" --data '{"sqlB64":"'"$b64"'","options":{"enableLandscape":true,"enableDcc":true,"mcSamples":8}}' | jq '.' || true

echo "---- FIX PHASE ----"
psql "postgresql://paraplan:paraplan@localhost:5414/demo" -v ON_ERROR_STOP=1 -f fix.sql

echo "Re-run EXPLAIN after fix"
psql "postgresql://paraplan:paraplan@localhost:5414/demo" -v ON_ERROR_STOP=1 -c "EXPLAIN (ANALYZE, BUFFERS) $(cat slow.sql)"

docker compose down
echo "Done."
