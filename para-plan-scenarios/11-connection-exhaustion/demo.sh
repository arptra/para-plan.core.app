#!/usr/bin/env bash
set -euo pipefail
docker compose up -d
CID=$(docker compose ps -q pg)
until docker exec "$CID" pg_isready -U paraplan -d demo >/dev/null 2>&1; do sleep 1; done

echo "Open many sessions to exhaust connections..."
for i in $(seq 1 30); do
  ( psql "postgresql://paraplan:paraplan@localhost:5413/demo" -c "SELECT pg_sleep(30);" >/dev/null 2>&1 || true ) &
done
sleep 2
echo "Try new connection (expect failure)"
! psql "postgresql://paraplan:paraplan@localhost:5413/demo" -c "SELECT 1" && echo "OK: connection refused due to max_connections"

b64=$(printf "%s" "SELECT 1" | base64 -w0 2>/dev/null || printf "%s" "SELECT 1" | base64 -b 0)
jq -n --arg sql "$b64" '{"sqlB64":$sql}' | curl -s -X POST http://localhost:8080/api/analyze -H "Content-Type: application/json" --data @- | jq '.advice,.recommendations' || true

docker compose down
echo "Done."
