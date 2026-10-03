# Hub Innowacji Spolecznych — MVP (HackYeah / ROPS Krakow)

Spring Boot 4.1 + Spring AI 2.0 + Thymeleaf/HTMX-style pages + Postgres/pgvector.
Matchmaking spoleczny (modul I, obligatoryjny) + Zasobnik wiedzy (II) + Kreator pomyslów (III) + Panel admina (VI).

## Uruchomienie

```bash
docker compose up -d                                   # Postgres 16 + pgvector
export OPENAI_API_KEY=***                        # embeddings + rerank LLM
./mvnw spring-boot:run                                 # http://localhost:8080
```

Konta demonstracyjne: `admin/admin123` (panel /admin), `user/user123`.
Bez klucza API aplikacja starta i dziala — matchmaking fallbackuje do dopasowania slowami kluczowymi.

## Architektura matchmakeingu (modul I)

1. Zgloszenie problemu (`POST /match`) trafia do `problem_report`; LLM przypisuje obszar z Mapy Wyzwan (structured output).
2. `MatchmakingService`: top-k po wektorach (cosine, HNSW w `vector_store`) + dopelnienie slowami kluczowymi (LIKE) — hybrid.
3. Rerank LLM zwraca `MatchExplanations` (JSON, walidowany przez Spring AI `.entity()`) z uzasadnieniem „dlaczego pasuje".
4. Fallback bez LLM: kolejnosc wektorowa + scoring slow kluczowych (demo nie pada bez sieci).

Innowacje indeksowane sa przy seedzie (`SeedRunner`) i przy dodaniu przez panel admina.

## Moduly

| Sciezka | Modul |
|---|---|
| `/match` | I. Matchmaking spoleczny (obligatoryjny) |
| `/wiedza` | II. Zasobnik wiedzy (wyzwania + biblioteka innowacji) |
| `/pomysly` | III. Kreator pomyslów (fiszki + asystent AI / Canwa) |
| `/admin` | VI. Panel administratora (zgloszenia, dodawanie innowacji) |

## Dostepnosc (WCAG 2.1 AA)

- skip-link, semantyczne landmarki (`header/nav/main`), etykiety przy kazdym polu,
- kontrat niebieski-900 na bieli ≥ 7:1, widoczny focus (3px outline), baza 17px dla seniorow,
- `aria-live` przy czacie asystenta, formularze obslugiwane klawiatura.

## Roadmapa (po hackathonie)

- IV. Tester innowacji (zapisy na testy + oceny), V. watki komunikacji, VII. Asystent „Middleman"
- generator wnioskow (RAG nad regulaminem naboru), powiadomienia e-mail, integracja z baza grantowa (REST/webhook)

## Koszty utrzymania (szacunek)

VPS ~5 EUR/mies. + OpenAI ~0,01 EUR/zapytanie (embeddings + rerank) + 0,2 FTE utrzymywania.