# Пакет демонстраций PostgreSQL проблем и авто-анализа

В пакете 12 сценариев для `/api/analyze` и ещё 4 сценария для `/api/sql-hints`,
каждый в отдельной папке с `docker-compose.yaml`, `demo.sh`, `README.md` и вспомогательными SQL-файлами.

## Требования
- Docker + docker-compose
- `psql`, `curl`, `jq`
- Ваш анализатор на http://localhost:8080/api/analyze и `/api/sql-hints`

## Запуск любого сценария
```bash
cd 01-seqscan-no-index   # выберите нужную папку
docker-compose up -d
./demo.sh
```
