# Hub Innowacji Spolecznych — MVP (HackYeah / ROPS Krakow)

Spring Boot 4.1 + Spring AI 2.0 + Thymeleaf + Postgres 18 (pgvector + Timescale pg_textsearch BM25)
+ llama.cpp (Qwen3-Embedding-4B, Qwen3-4B-Instruct) — **100% lokalne, zero API, zero kosztów zapytan**.

Matchmaking spoleczny (modul I, obligatoryjny) + Zasobnik wiedzy (II) + Kreator pomyslów (III) + Panel admina (VI).

## Uruchomienie

```bash
# 1. modele (jednorazowo, ~5 GB)
sh ~/models/fetch_models.sh            # lub pobierz GGUF z HF
# 2. baza: Postgres 18 + pgvector + pg_textsearch (BM25)
docker build -t hub-pg:latest -f docker/Dockerfile.pg docker/
docker compose up -d
# 3. lokalne modele jako OpenAI-compatible API
sh ~/models/run_embed.sh &             # :8081 embeddings (Qwen3-Embedding-4B, 2560d)
sh ~/models/run_chat.sh  &             # :8082 chat (Qwen3-4B-Instruct)
# 4. aplikacja
./mvnw spring-boot:run                 # http://localhost:8080
```

Konta demonstracyjne: `admin/admin123` (panel `/admin`), `user/user123`.
Bez modeli aplikacja starta i dziala — matchmaking fallbackuje do samej kolejnosci BM25.

## Architektura matchmakeingu (modul I)

1. Zgloszenie (`POST /match`) trafia do `problem_report`; LLM przypisuje obszar z Mapy Wyzwan (structured output).
2. **Hybrydowe wyszukanie**: pgvector cosine (semantyka, Qwen3-Embedding-4B) + **BM25** (`pg_textsearch`,
   indeks `innovation_bm25_idx`, konfig `hub_pl` — polskie stopwordsy + skladanie diakrytykow
   `translate()` po obu stronach zapytania) — polaczone **reciprocal-rank fusion** (k=60).
3. Rerank LLM (Qwen3-4B) zwraca `MatchExplanations` (JSON przez Spring AI `.entity()`)
   z uzasadnieniem „dlaczego pasuje\" + score RRF znormalizowany do 0-100%.
4. Fallback: kolejnosc RRF/BM25 — demo nie pada bez modeli.

Uwaga: Qwen3-Embedding-4B ma 2560 wymiarow > limit HNSW pgvector (2000) — indeks `NONE`
(dokladne przeszukiwanie; przy bibliotece ~tys. pozycji to ~ms). Przy skali: Qwen3-Embedding-0.6B
(1024d) + HNSW.

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