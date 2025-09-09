#!/usr/bin/env bash
set -euo pipefail

echo "==> Starting Postgres via docker-compose..."
docker compose up -d pg

echo "==> Waiting for pg to be healthy..."
for i in {1..60}; do
  if docker exec monster3_pg pg_isready -U paraplan -d demo >/dev/null 2>&1; then
    echo "Postgres is ready."
    break
  fi
  sleep 2
done

echo "==> Running 00_init.sql (this may take a few minutes)..."
psql "postgresql://paraplan:paraplan@localhost:5439/demo" -f ./db/00_init.sql

echo "==> Running 01_slow.sql (EXPECT SLOW PLAN)..."
psql "postgresql://paraplan:paraplan@localhost:5439/demo" -f ./db/01_slow.sql | tee ./db/01_slow.out

echo "==> Applying fixes and running 02_fix.sql (EXPECT MUCH FASTER)..."
psql "postgresql://paraplan:paraplan@localhost:5439/demo" -f ./db/02_fix.sql | tee ./db/02_fix.out

echo "==> Done. Compare ./db/01_slow.out vs ./db/02_fix.out"
