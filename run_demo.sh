
#!/usr/bin/env bash
set -euo pipefail

echo "==> Starting PostgreSQL via docker-compose"
( cd deploy && docker compose up -d )

echo "==> Building and starting Spring Boot app"
./gradlew --no-daemon bootRun &
APP_PID=$!

# wait a bit
sleep 8

echo "==> Calling /api/analyze"
curl -s http://localhost:8080/api/analyze -H "Content-Type: application/json" -d @- <<'JSON' | sed -e 's/\\u003c/</g' -e 's/\\u003e/>/g' | head -n 80
{
  "sql": "SELECT o.id, o.created_at, c.name FROM orders o JOIN customers c ON c.id=o.customer_id WHERE c.email ILIKE "%@example.com" AND o.created_at >= now() - interval '30 days' ORDER BY o.created_at DESC LIMIT 200",
  "options": { "enableLandscape": true, "enableDcc": true, "mcSamples": 60 }
}
JSON

echo "==> Done. Press Ctrl+C to stop the app (PID $APP_PID)"
wait $APP_PID
