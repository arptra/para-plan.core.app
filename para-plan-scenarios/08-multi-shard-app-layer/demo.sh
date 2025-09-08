#!/usr/bin/env bash
set -euo pipefail
docker-compose up -d
for port in 5411 5412; do
  until PGPASSWORD=paraplan psql -h localhost -p $port -U paraplan -d shard$([ $port = 5411 ] && echo 1 || echo 2) -c "SELECT 1" >/dev/null 2>&1; do sleep 1; done
done
psql "postgresql://paraplan:paraplan@localhost:5411/shard1" -f init-shard1.sql >/dev/null
psql "postgresql://paraplan:paraplan@localhost:5412/shard2" -f init-shard2.sql >/dev/null
jq -Rs '{sql: .}' slow.sql | curl -s -X POST http://localhost:8080/api/analyze -H "Content-Type: application/json" --data @- | jq '.recommendations'

docker-compose down
echo "Done."
