#!/bin/bash
set -e

for dir in /para-plan-scenarios/*; do
  [ -d "$dir" ] || continue
  schema=$(basename "$dir" | tr '-' '_')
  init_sql="$dir/init.sql"
  if [ -f "$init_sql" ]; then
    echo "Initializing schema $schema from $init_sql"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<EOSQL
CREATE SCHEMA IF NOT EXISTS "$schema";
SET search_path TO "$schema";
\i '$init_sql'
EOSQL
  fi
done

