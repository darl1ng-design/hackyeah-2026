# Hub Innowacji Spolecznych — MVP (HackYeah / ROPS Krakow)

Spring Boot 4.1 + Spring AI 2.0 + Thymeleaf + Postgres 18 (pgvector + Timescale pg_textsearch BM25)
+ llama.cpp (Qwen3-Embedding-0.6B, Qwen3-4B-Instruct) — **100% lokalne, zero API, zero kosztów zapytan**.

Matchmaking spoleczny (modul I, obligatoryjny) + Zasobnik wiedzy (II) + Kreator pomyslów (III) + Panel admina (VI).

## Uruchomienie

```bash
# 1. modele (jednorazowo, ~3 GB GGUF na dysku)
MODELS_DIR=${MODELS_DIR:-$HOME/models} sh ~/models/fetch_models.sh
# 2. wszystko: db (pg18+pgvector+bm25) + embed (0.6B) + chat (4B) + aplikacja
docker compose up -d --build           # http://localhost:8080
```

Katalog modeli to `~/models` (nadpisz przez `MODELS_DIR`). `docker compose down`
zostawia dane w wolumenie `hub-pgdata`; `down -v` czyści bazę do zera.

Konta demonstracyjne: `admin/admin123` (panel `/admin`), `user/user123`.
Bez chatu aplikacja starta i dziala — matchmaking fallbackuje do kolejnosci RRF (wektor + BM25).

## API dla frontendu

REST pod `/api/v1/**` (server-renderowany Thymeleaf zostaje — API to równoległa warstwa):

| Metoda | Sciezka | Opis |
|---|---|---|
| GET | `/api/v1/innovations?areaId=&q=` | biblioteka innowacji (filtr obszaru / slowo kluczowe) |
| GET | `/api/v1/innovations/{id}` | jedna innowacja (404 jesli brak) |
| GET | `/api/v1/areas` | obszary Mapy Wyzwan |
| GET | `/api/v1/resources` | zasoby ROPS |
| POST | `/api/v1/matches` | `{description, region?, authorName?}` -> `{reportId, reportStatus, area, matches[]}` (matchmaking hybrydowy) |
| GET | `/api/v1/ideas` | fiszki pomyslow (nowe na górze) |
| POST | `/api/v1/ideas` | `{title*, essence?, targetGroup?, stage?, description?}` -> 201 + fiszka |
| POST | `/api/v1/ideas/assistant` | `{message}` -> `{reply}` (asystent AI; fallback gdy chat wylaczony) |

Dokumentacja: **Swagger UI** `http://localhost:8080/swagger-ui.html`,
maszynowo `http://localhost:8080/v3/api-docs` (JSON, generowany w runtime — zawsze aktualny).
`openapi.json` w glównym katalogu repo to wyeksportowany snapshot tego specu.

Auth demo: `POST /login` (form: `username`, `password`, `_csrf`) -> ciastko `JSESSIONID`;
w `fetch` uzywaj `credentials: 'include'`. Endpointy `/api/v1/**` sa w demo otwarte
(bez logowania i CSRF) — do zamkniecia przed wdrozeniem.

## Architektura matchmakeingu (modul I)

1. Zgloszenie (`POST /match`) trafia do `problem_report`; LLM przypisuje obszar z Mapy Wyzwan (structured output).
2. **Hybrydowe wyszukanie**: pgvector cosine (semantyka, Qwen3-Embedding-4B) + **BM25** (`pg_textsearch`,
   indeks `innovation_bm25_idx`, konfig `hub_pl` — polskie stopwordsy + skladanie diakrytykow
   `translate()` po obu stronach zapytania) — polaczone **reciprocal-rank fusion** (k=60).
3. Rerank LLM (Qwen3-4B) zwraca `MatchExplanations` (JSON przez Spring AI `.entity()`)
   z uzasadnieniem „dlaczego pasuje\" + score RRF znormalizowany do 0-100%.
4. Fallback: kolejnosc RRF/BM25 — demo nie pada bez modeli.

Uwaga: Qwen3-Embedding-0.6B ma 1024 wymiary — miesci sie w limicie HNSW pgvector (2000),
wiec indeks `NONE` (dokladne przeszukiwanie; przy bibliotece ~tys. pozycji to ~ms) mozna
w razie potrzeby zamienic na HNSW.

## Dane

`V4`/`V5` (Flyway): obszary Mapy Wyzwan + **18 prawdziwych pozycji** z Biblioteki Innowacji
Spolecznych ROPS Krakow (BaWita, Merkury, koMIX zyciowy, Patryk i Kropka, Edki, Puzzle 3D
Braille, Kody QR...) z **prawdziwymi linkami**: strony katalogowe rops.krakow.pl, filmy
YouTube, materialy CC-BY + ogolnopolskie linie (116 111, 800 70 2222, Niebieska Linia).
Kazda karta ma klikalne „Strona innowacji\" i „Zobacz film\".

## Moduly

| Sciezka | Modul |
|---|---|
| `/match` | I. Matchmaking spoleczny (obligatoryjny) |
| `/wiedza` | II. Zasobnik wiedzy (wyzwania + biblioteka z linkami) |
| `/pomysly` | III. Kreator pomyslów (fiszki + asystent AI / Canwa) |
| `/admin` | VI. Panel administratora (zgloszenia, dodawanie innowacji + URL zrodla) |

## Dostepnosc (WCAG 2.1 AA)

- skip-link, semantyczne landmarki (`header/nav/main`), etykiety przy kazdym polu,
- kontrat niebieski-900 na bieli ≥ 7:1, widoczny focus (3px outline), baza 17px dla seniorow,
- `aria-live` przy czacie asystenta, formularze obslugiwane klawiatura.

## Roadmapa (po hackathonie)

- IV. Tester innowacji, V. watki komunikacji, VII. Asystent „Middleman\"
- generator wnioskow (RAG nad regulaminem naboru), powiadomienia e-mail, integracja z baza grantowa

## Koszty utrzymania

Model lokalny = 0 EUR/zymtanie. VPS z GPU albo CPU 32 GB (oba modele Q4 mieszcza sie w ~15 GB RAM)
~15-30 EUR/mies. + 0,2 FTE. Alternatywnie tanie API (OpenAI) ~0,01 EUR/zapytanie.

## Znane limity demo

- Rerank LLM na CPU: ~30-50 s/zgloszenie (latwe do skrocenia: max_tokens, mniejszy model, GPU).
- BM25 bez stemmingu PL (snowball nie ma polskiego): formy slow musza sie zgadzac co do litery
  po zlozeniu diakrytykow; synonimow (toalety~lazienki) pilnuja wektory.
- In-memory users (demo); przed wdrozeniem: tabela users + migracja.