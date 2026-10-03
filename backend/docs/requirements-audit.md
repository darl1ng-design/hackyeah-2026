# Ocena backendu względem wyzwania ROPS Kraków

Stan na 2026-10-04. Ocena dotyczy kodu w tym repozytorium oraz wymagań podanych
w opisie „TEMPLATE WYZWANIA”. „Częściowo” oznacza działający wycinek modułu,
bez wszystkich wymienionych funkcji. Tylko matchmaking jest obowiązkowy w MVP;
pozostałe moduły podnoszą ocenę i gotowość do wdrożenia.

| Moduł | Ocena | Dowody w backendzie | Braki |
|---|---|---|---|
| I. Matchmaking społeczny | Częściowo | `POST /api/v1/matches`, `GET /api/v1/matches/{reportId}`; wyniki zapisane w `report_match`; wektory + BM25 + zapasowe wyszukiwanie słów kluczowych | Brak wyszukiwania podobnych *zgłoszeń/przypadków*; trafność na rzeczywistych danych i modelach niezmierzona |
| II. Zasobnik wiedzy | Częściowo | Obszary, katalog innowacji z filtrem i paginacją, regiony, linki do zasobów i filmów | Brak zagregowanych trendów potrzeb dostępnych tylko administratorowi; brak pełnej treści raportów/Mapy Wyzwań i szybkiej edycji istniejących zasobów; próbka obejmuje 18 innowacji |
| III. Kreator pomysłów | Częściowo | Publiczne zgłoszenie, autor, etap, moderacja, „Moje pomysły”, asystent z historią rozmowy, link do Canvas | Brak generatora wniosków zależnego od naboru i wizualizacji pomysłu; brak ścieżki odpowiedzi do autora |
| IV. Tester innowacji | Brak | — | Brak zapisów do testów, ocen i informacji zwrotnej |
| V. Komunikacja | Brak | — | Brak rozmów z mentorem/ROPS, partnerstw i powiadomień |
| VI. Panel administratora | Częściowo | Dodanie innowacji, podgląd i zmiana statusu zgłoszeń, moderacja pomysłów; role STAFF/ADMIN | Brak edycji/usuwania wiedzy, agregatów trendów i powiadomienia o nowym pomyśle |
| VII. Middleman Innowacji | Brak | — | Brak asystenta adaptacji innowacji do usługi instytucji |

## Scenariusze sprawdzone

Testy HTTP z bazą H2 przechodzą przez: token CSRF i publiczne zapisy bez konta;
utworzenie i późniejszy odczyt raportu z dopasowaniem; odczyt zgłoszeń przez
pracownika i zmianę statusu przez administratora; rejestrację i logowanie
mieszkańca; własny pomysł oczekujący na moderację, zatwierdzenie i publiczny
odczyt; publiczne zgłoszenie pomysłu przez gościa; dodanie innowacji przez
administratora, filtrowanie katalogu, obszary, regiony i zasoby; żądanie do
asystenta z historią rozmowy. Osobny test sprawdza logowanie kont personelu
utworzonych z konfiguracji, a test matchmakingu wykrył i potwierdził poprawkę
zapasowego wyszukiwania słów kluczowych.

Ostatni pełny przebieg `mvn -o -q test`: 43 testy, 0 błędów, 0 niepowodzeń,
1 pominięty test zależny od zewnętrznej infrastruktury. Testy H2 nie dowodzą
poprawnego działania rozszerzeń PostgreSQL, migracji Flyway w docelowej bazie,
modeli językowych ani faktycznej trafności dopasowania. Nie uruchamiano ani
nie budowano kontenerów Docker.

## Wniosek i kolejność poprawek

Backend nie spełnia całego wyzwania. Zapewnia API dla obowiązkowego
matchmakingu oraz fragmenty zasobnika, kreatora i panelu, ale nie kompletne
„cyfrowe serce” Hubu. Najważniejsze dalsze prace, w kolejności pilności:

1. **Interfejs i demo:** `frontend/src/App.tsx` jest nadal domyślną stroną
   Vite/React, bez wywołań API. Brakuje przepływu zgłoszenie → dopasowanie,
   makiet UX/UI i potwierdzenia WCAG 2.1 AA. Repo nie zawiera prezentacji/filmu
   ani linku do działającego demo.
2. **Walidacja docelowego stosu:** uruchomić migracje oraz przepływ
   pgvector/BM25/LLM na PostgreSQL i modelach, porównać trafność wyników na
   reprezentatywnych opisach problemów i innowacji. Obecna próbka 18 pozycji
   nie reprezentuje całego portfolio ROPS.
3. **Obsługa pracy Hubu:** dodać trendy potrzeb tylko dla administratora,
   edycję/aktualizację wiedzy, powiadomienie o nowym pomyśle oraz odpowiedź
   do autora. To odpowiada wprost na kryterium walidacji ścieżki komunikacji.
4. **Rozwój platformy:** zaplanować tester innowacji, dialog z mentorami,
   partnerstwa, generator wniosków i Middlemana; dodać paginację do pomysłów
   i zgłoszeń oraz zasady przechowywania i usuwania danych użytkowników.
5. **Materiały formalne:** doprecyzować nazwę produktu i opis rozwiązania,
   przygotować prezentację lub film, adres demo, makiety oraz realistyczny
   koszt utrzymania i zasoby.

Nie należy deklarować pełnej zgodności z wyzwaniem ani gotowości wdrożeniowej
na podstawie samych przechodzących testów backendu.
