.PHONY: dev build test db-up db-down

dev:
	npm run dev

build:
	npm run build

test:
	npm run test

db-up:
	docker compose up -d

db-down:
	docker compose down
