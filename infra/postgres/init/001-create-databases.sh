#!/usr/bin/env bash
set -euo pipefail

# The official postgres image supplies POSTGRES_USER and POSTGRES_DB to init
# scripts. Keep ownership aligned with .env so a local developer can customize
# the non-secret username without breaking Flyway permissions.
for database in identity_db catalog_db order_db inventory_db payment_db notification_db; do
  exists="$(psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --tuples-only --no-align \
    --command "SELECT 1 FROM pg_database WHERE datname = '${database}'")"

  if [[ "$exists" != "1" ]]; then
    psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
      --command "CREATE DATABASE \"${database}\" OWNER \"${POSTGRES_USER}\""
  fi
done
