# Małopolski Hub Innowacji Społecznych

Projekt Maven z modułem `backend/` (Spring Boot 4.1, Spring AI 2.0, PostgreSQL)
oraz osobnym `frontend/`. Główny `pom.xml` zarządza wspólnymi zależnościami.

## Uruchomienie i testy backendu

Backend wymaga PostgreSQL z rozszerzeniami pgvector i pg_textsearch. Parametry
połączenia: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`. Konfiguracja
modeli lokalnych używa `LLM_EMBED_BASE_URL` i `LLM_CHAT_BASE_URL`. Migracje
Flyway uruchamiają się automatycznie przy starcie aplikacji.

```bash
mvn -pl backend test
mvn -pl backend spring-boot:run
```

API działa domyślnie na porcie 8080 (`8083` przy mapowaniu z `docker-compose.yml`).
Swagger UI: `/swagger-ui.html`; aktualny JSON: `/v3/api-docs`. Pliki
`openapi.json` i `openapi.yaml` w katalogu głównym są snapshotami tego kontraktu.

## Konta, sesja i CSRF

Mieszkaniec może bez konta zgłosić problem, uruchomić dopasowanie, rozmawiać
z asystentem i przesłać pomysł. Pomysły mieszkańców trafiają do moderacji.
Opcjonalne konto daje dostęp do `GET /api/v1/ideas?mine=true`, gdzie widać także
własne pomysły oczekujące na moderację. Rejestracja to e-mail i hasło (minimum
12 znaków), obecnie bez potwierdzania adresu e-mail.

Pracownicy i administratorzy logują się. Konta początkowe są tworzone z par
zmiennych środowiskowych `HUB_STAFF_EMAIL` / `HUB_STAFF_PASSWORD` oraz
`HUB_ADMIN_EMAIL` / `HUB_ADMIN_PASSWORD`. Każda skonfigurowana para musi mieć
hasło o długości co najmniej 12 znaków. Nie ma kont z domyślnymi hasłami.
Zmienne są przekazywane także przez `docker-compose.yml`.

Klient przeglądarkowy:

1. Pobiera `GET /api/v1/csrf` i zachowuje sesję oraz zwrócony token.
2. Przy każdym `POST` i `PATCH` wysyła token w nagłówku `X-CSRF-TOKEN`
   (dla formularza logowania może użyć pola `_csrf`). W `fetch` ustawia
   `credentials: 'include'`.
3. Rejestruje konto przez `POST /api/v1/register`, jeśli użytkownik chce
   korzystać z „Moich pomysłów”. Loguje się przez `POST /login` z formularzem
   `username` (adres e-mail) i `password`; po zalogowaniu ponownie pobiera
   token CSRF. `GET /api/v1/me` zwraca e-mail i role; bez sesji zwraca 401.

Publiczne odczyty katalogu i zatwierdzonych pomysłów nie wymagają sesji.
`GET /api/v1/reports` wymaga roli STAFF lub ADMIN, a `/api/v1/admin/**`
wymaga roli ADMIN. Żądania zmieniające dane wymagają tokenu CSRF także wtedy,
gdy są dostępne dla gościa.

## API v1

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/api/v1/innovations?page=0&size=20&sort=createdAt,desc&region=MALOPOLSKA&areaId=&q=` | Stronicowana lista; `size` 1–100; sortowanie po `createdAt`, `title` lub `status` |
| GET | `/api/v1/innovations/{id}` | Szczegóły innowacji z `createdAt` |
| GET | `/api/v1/regions`, `/api/v1/areas`, `/api/v1/resources` | Słowniki i zasoby |
| POST | `/api/v1/matches` | Publiczne dopasowanie problemu; zwraca `reportId`, status i wyniki |
| GET | `/api/v1/matches/{reportId}` | Trwały, udostępnialny wynik dopasowania |
| GET | `/api/v1/ideas` | Publiczna lista zatwierdzonych pomysłów |
| GET | `/api/v1/ideas?mine=true` | Własne pomysły zalogowanego użytkownika, także oczekujące |
| GET | `/api/v1/ideas/{id}` | Szczegóły pomysłu; oczekujące widzi właściciel i personel |
| POST | `/api/v1/ideas` | Publiczne zgłoszenie pomysłu; wynik ma autora i status `PENDING` |
| POST | `/api/v1/ideas/assistant` | Publiczna porada; żądanie przyjmuje `message`, `history` i `ideaContext` |
| POST | `/api/v1/register` | Opcjonalne konto mieszkańca |
| GET | `/api/v1/me`, `/api/v1/csrf` | Stan sesji i token CSRF |
| GET | `/api/v1/reports` | Lista zgłoszeń dla pracownika i administratora |
| GET | `/api/v1/admin/ideas?status=PENDING` | Kolejka moderacji |
| PATCH | `/api/v1/admin/ideas/{id}/moderation` | Zmiana statusu moderacji |
| POST | `/api/v1/admin/innovations` | Dodanie innowacji |
| PATCH | `/api/v1/admin/reports/{id}/status` | Zmiana statusu zgłoszenia |

Wartości `stage`, `status`, `reportStatus`, `kind`, `region` i statusu
moderacji są enumami w OpenAPI. `history` asystenta jest listą tur `USER` /
`ASSISTANT` (do 20 wpisów); klient przesyła ją ponownie przy następnym
pytaniu. Opcjonalny `ideaContext` zawiera dane formularza pomysłu.

## Dopasowanie

Zgłoszenie trafia do `problem_report`, a znalezione innowacje i uzasadnienia
do `report_match`. Wynik można odczytać później pod własnym adresem. Silnik
łączy wyszukiwanie wektorowe i BM25, a model językowy klasyfikuje problem oraz
uzasadnia dopasowania. Przy niedostępności modelu działa ścieżka zapasowa.

Migracje `V4`–`V6` zawierają obszary, innowacje i zasoby ROPS. Migracja `V9`
dodaje konta, status moderacji, słowniki, czas utworzenia innowacji i zapisane
wyniki dopasowań.
