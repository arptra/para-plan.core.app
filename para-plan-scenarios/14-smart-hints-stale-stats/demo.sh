#!/usr/bin/env bash
set -euo pipefail

echo "Bringing up Postgres on port 5401..."
docker-compose up -d
CID=$(docker-compose ps -q pg)
until docker exec "$CID" pg_isready -U paraplan -d demo >/dev/null 2>&1; do sleep 1; done

psql "postgresql://paraplan:paraplan@localhost:5401/demo" -v ON_ERROR_STOP=1 -f ../../sql/init.sql
psql "postgresql://paraplan:paraplan@localhost:5401/demo" -v ON_ERROR_STOP=1 -f ../../sql/states/06_stale_stats.sql

ACTUAL=$(curl -s -X POST http://localhost:8080/api/sql-hints -H 'Content-Type: application/json' --data @request.json | jq -S '.')
EXPECTED=$(jq -S '.' expected.json)
diff <(echo "$EXPECTED") <(echo "$ACTUAL")

docker-compose down
