# Frontend (modul osobny)

Tutaj powstaje frontend. Backend to osobny modul w `../backend` (Spring Boot, czysto REST —
Thymeleaf usuniety; frontend komunikuje sie wylacznie z API).

## Kontrakt API — nic wiecej nie trzeba

- **Swagger UI (na zywym stacku):** http://localhost:8083/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8083/v3/api-docs
- **OpenAPI YAML:** http://localhost:8083/v3/api-docs.yaml
- **Snapshoty w repo (offline):** `../openapi.json`, `../openapi.yaml` — na codegen
  typów/klienta, np. `npx openapi-typescript ../openapi.json -o src/api/schema.d.ts`

Start stacku do pracy: `docker compose up -d` w katalogu glównym (app: 8083, db: 5439,
embed: 8081, chat: 8082).

## Konwencje

- Sciezki API: `/api/v1/**` (endpointy w tabeli w glównym README).
- Dev-proxy w Vite: `server.proxy = { '/api': 'http://localhost:8083' }` — zero CORS.
- Auth demo: `POST /login` (form `username`/`password`/`_csrf`) -> ciastko `JSESSIONID`;
  `fetch(..., { credentials: 'include' })`. `/api/v1/**` dziala tez bez logowania (demo).
- Odpowiedzi JSON w UTF-8 z polskimi znakami; nazwa pola `similarity` to 0..1.

## Zasady w tym katalogu

- Nie ruszac backendu z tego modulu — zmiany kontraktu ida przez backend + przeeksport
  `openapi.json`/`openapi.yaml` (`curl http://localhost:8083/v3/api-docs -o openapi.json`).
- Node_modules/buildy trzymane poza git (.gitignore ponizej).