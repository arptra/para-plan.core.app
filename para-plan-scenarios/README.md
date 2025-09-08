# Пакет демонстраций PostgreSQL проблем и авто-анализа

В пакете 12 сценариев, каждый в отдельной папке с `docker-compose.yaml`, `init.sql`, `slow.sql`, `fix.sql`, `demo.sh`, `README.md`.

## Требования
- Docker + docker compose
- `psql`, `curl`, `jq`
- Ваш анализатор на http://localhost:8080/api/analyze

## Запуск любого сценария
```bash
cd 01-seqscan-no-index   # выберите нужную папку
docker compose up -d
./demo.sh
```
