# Frontend standards — React 19 + Vite 8 + TS 6

Cel: spójna, lekka SPA. Mało zależności, mało warstw, płaska struktura. Kontrakt z backendem = `openapi.json` w root repo (Spring, REST, `/api/v1/**`, port 8083).

## 1. Zasady ogólne

- **Najpierw platforma**: React + przeglądarka + CSS. Nowa zależność tylko gdy kilka linijek kodu nie wystarczy — i z uzasadnieniem w PR.
- **Zero abstrakcji „na zapas”**: bez generycznych wrapperów, fabryk, HOC-ów, providerów dla jednego użycia.
- **TypeScript strict-ish**: obecny `tsconfig.app.json` (noUnused\*, `verbatimModuleSyntax`) zostaje. Bez `any` — użyj `unknown` + zawężenie.
- `npm run lint` i `npm run build` muszą przechodzić przed merge.

## 2. Struktura katalogów

```
src/
  main.tsx            # bootstrap: fonty, style globalne, <App/>
  App.tsx             # shell: sesja, guardy ról, wybór layoutu i ekranu
  screens.ts          # mapa: nazwa trasy → komponent ekranu
  session.tsx         # jedyny Context: me/rola, obszary, regiony, powiadomienia, toast
  api/
    client.ts         # jeden fetch wrapper (cookie sesji + CSRF + błędy) + funkcje per endpoint
    types.ts          # typy DTO z openapi.json
    useApi.ts         # hook { data, error, loading, reload, setData }
  components/
    ds.js / ds.d.ts   # design system Kompas Małopolski (vendored z claude.ai/design — nie edytować ręcznie)
    PageState.tsx     # loading / błąd / 401→logowanie
  features/<nazwa>/   # ekran/domena: komponenty + css tej funkcji
    IdeasPage.tsx
    IdeasPage.module.css
  styles/
    tokens.css        # tokeny DS (kolory, typografia, spacing) — jedyne źródło wartości
    global.css        # base + klasy użytkowe layoutu/typografii (container, stack, h1, muted…)
  lib/                # czyste helpery: router, format, labels, errors, useMedia
```

- Feature-first: kod żyje przy funkcji, która go używa. Do `components/` przenosimy dopiero przy **drugim** użyciu.
- Bez `index.ts` barrel files — importy bezpośrednio z pliku.
- Bez głębokiego zagnieżdżania: max `features/<x>/Plik.tsx`.

## 3. Nazewnictwo

| Co             | Konwencja                      | Przykład                      |
| -------------- | ------------------------------ | ----------------------------- |
| Komponent      | `PascalCase.tsx`, named export | `export function IdeaCard()`  |
| Hook           | `useCamelCase.ts`              | `useIdeas.ts`                 |
| CSS komponentu | `Nazwa.module.css`             | `IdeaCard.module.css`         |
| Helper         | `camelCase.ts`                 | `formatDate.ts`               |
| Typ/props      | `PascalCase`, `Props` lokalnie | `type Props = { idea: Idea }` |

- Named exports wszędzie (wyjątek: nic). Ułatwia grep i refaktor.
- Teksty UI po polsku, kod/identyfikatory po angielsku.

## 4. Komponenty

- Funkcje, nie klasy. Props typowane przez `type Props = {...}`, destrukturyzacja w sygnaturze.
- Jeden komponent eksportowany na plik; małe pomocnicze komponenty mogą być w tym samym pliku (nieeksportowane).
- Komponent > ~150 linii → wydziel podkomponent lub hook.
- Logika (fetch, stan złożony) w hookach `useX`, JSX w komponencie.
- Semantyczny HTML: `<button>` dla akcji, `<a>` dla nawigacji, `<label>` przy polach, `alt` przy obrazkach. Fokus widoczny.

## 5. Stan

Drabina — zatrzymaj się na pierwszym szczeblu, który wystarcza:

1. Wartość wyliczalna → licz w renderze (bez `useState` + `useEffect`).
2. Lokalny stan → `useState` / `useReducer`.
3. Współdzielony przez rodzeństwo → podnieś do rodzica.
4. Globalny (zalogowany user, słowniki, toast) → `useSession()` z `session.tsx`. Nowy globalny stan dopisujemy tam, nie tworzymy kolejnych Contextów.
5. Biblioteka stanu (Zustand/Redux) — **nie**, dopóki 1–4 realnie nie bolą.

- Stan w URL (`?filter=`, `#/ideas/12`) gdy ma przetrwać odświeżenie lub być linkowalny.
- `useEffect` tylko do synchronizacji z zewnętrznym światem (fetch, subskrypcje). Nie do przeliczania stanu.

## 6. Dane / API

- Jeden plik `src/api/client.ts`: `fetch` z `credentials: 'include'`, nagłówek CSRF (token z `GET /api/v1/csrf`), JSON, rzucanie `ApiError { status, fields }` przy `!res.ok`.
- Wszystkie endpointy jako funkcje obiektu `api` w `client.ts` (`api.ideas()`, `api.match(body)`) — ekrany nie wołają `fetch` bezpośrednio.
- Logowanie/wylogowanie: Spring `formLogin` (`POST /login`, `/logout`, form-encoded) — obsłużone w `api.login/logout`.
- Typy DTO odpowiadają `openapi.json`. Ręcznie w `api/types.ts`; generator (`openapi-typescript`) dopiero gdy API zacznie często się zmieniać.
- Odczyt danych ekranu: `useApi(() => api.x(...), [deps])` + `<PageState loading error />` (401 → przekierowanie na logowanie, 403/404 → strona błędu). Każdy ekran obsługuje **loading, error, empty**.
- Błędy formularzy: `fieldErrors(e)` przy polach, reszta przez `showToast(msg, 'danger')`.
- Bez React Query, dopóki nie potrzebujemy cache/refetch/invalidacji na wielu ekranach.
- Dev: Vite proxy `/api`, `/login`, `/logout` → `http://localhost:8083` (lub `BACKEND_URL`) w `vite.config.ts` — jedno origin, cookies bez CORS.
- Sekretów w kodzie frontu nie ma. Konfiguracja tylko przez `import.meta.env.VITE_*`.

## 7. Style

- Źródło prawdy: tokeny design systemu (Kompas Małopolski) jako CSS custom properties w `styles/tokens.css`. **Żadnych surowych kolorów/odstępów w komponentach** — tylko `var(--…)`.
- Style komponentu: CSS Modules (`*.module.css`, natywnie w Vite). Klasy `camelCase`.
- Najpierw komponenty DS (`components/ds`), potem klasy z `global.css`, dopiero potem własny `*.module.css`.
- Bez Tailwinda, styled-components, CSS-in-JS. Inline `style` tylko dla wartości dynamicznych (np. szerokość paska wykresu).
- Responsywność: mobile-first, `min-width` media queries, layout przez flex/grid.
- Ikony: `<Icon name="…"/>` z DS (nazwy Lucide). Fonty: `@fontsource/*` (self-hosted, importy w `main.tsx`).

## 8. Routing

- Hash routing w `lib/router.ts`: tabela regex → nazwa trasy, `useHash()`, `go(path)`, `setQuery(route, patch)`. Nowy ekran = wpis w `ROUTES` + `screens.ts`.
- Ekran dostaje `{ route }` (params, query) i jest remountowany przy zmianie ścieżki — stan początkowy bierz z `route.query`.
- Guardy ról (`AUTH_ROUTES`, `STAFF_ROUTES`, `ADMIN_ROUTES`) tylko w `App.tsx`.
- `react-router` dopiero gdy pojawią się zagnieżdżone layouty / loadery.

## 9. Formularze

- Natywne `<form onSubmit>` + `FormData` lub kontrolowane `useState`.
- Walidacja HTML (`required`, `type="email"`, `minLength`) + błędy z backendu wyświetlane przy polu.
- Bez bibliotek formularzy, dopóki formularz nie ma >10 pól z zależnościami.

- Dyktowanie (Whisper): pod polem tekstowym `<Dictation label onText={dictate(get, set, multi)} />` z `features/dictation` (`dictate = useInsertDictated()`). Tekst zawsze **dopisywany**, z toastem „Cofnij”; `big` dla głównego pola ekranu. Nigdy dla e-maila, hasła, URL-i, dat.

## 10. Testy

- Logika w `lib/` i nietrywialne hooki: Vitest (dochodzi gdy pojawi się pierwsza taka logika).
- Ekrany: sprawdzane ręcznie / Playwright smoke dla krytycznych ścieżek (login, zgłoszenie).

## 11. Checklist PR

- [ ] lint + build zielone
- [ ] brak nowych zależności lub uzasadnienie
- [ ] tylko tokeny w CSS
- [ ] loading / error / empty obsłużone
- [ ] a11y: label, alt, `<button>` vs `<a>`, fokus
