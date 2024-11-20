docker rm pg
docker compose down -v && docker compose build && docker compose up
