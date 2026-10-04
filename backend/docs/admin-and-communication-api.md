# Trendy, wiedza i odpowiedzi na pomysły

Żądania modyfikujące opisane poniżej wymagają sesji i tokenu CSRF z `GET /api/v1/csrf`.
`/api/v1/admin/**` wymaga roli `ADMIN`, a `/api/v1/staff/**` wymaga `STAFF` lub
`ADMIN`. Zgłoszenie pomysłu, powiadomienia i odpowiedzi własnego pomysłu
wymagają zalogowania. Dopasowanie problemu i asystent pozostają publiczne.

## Trendy potrzeb

`GET /api/v1/admin/trends?from=2026-01-01&to=2026-12-31` zwraca liczbę zgłoszeń
problemu oraz agregaty `byArea`, `byRegion`, `byStatus` i `byMonth`. Daty są
opcjonalne i włączne; granice zakresu są liczone od północy UTC. Brak obszaru
lub regionu jest pokazany osobno. Wynik nie zawiera opisów problemów ani danych
autorów.

## Aktualizacja wiedzy

- `GET /api/v1/admin/innovations?page=0&size=20` pokazuje także szkice.
- `GET/PUT /api/v1/admin/innovations/{id}` odczytuje lub zastępuje pełny wpis.
- `GET /api/v1/admin/resources` i `GET/PUT /api/v1/admin/resources/{id}`
  odczytują i zastępują zasób; `POST /api/v1/admin/resources` go tworzy.
- `POST /api/v1/admin/areas` i `PUT /api/v1/admin/areas/{id}` tworzą i
  aktualizują słownik obszarów.

`PUT` innowacji przyjmuje `title`, `summary`, `description`, `targetGroup`,
`status`, `region`, `videoUrl`, `sourceUrl`, `areaId` i obowiązkowe `published`.
Nowa innowacja przez `POST /api/v1/admin/innovations` jest szkicem, chyba że
administrator poda `"published": true`. Publiczny katalog i nowe dopasowania
uwzględniają tylko opublikowane innowacje. Edycja odświeża indeks wektorowy;
jeśli model embeddingów jest niedostępny, wpis pozostaje zapisany, a indeks
zostanie ponowiony przy uruchomieniu usługi.

## Pomysły i odpowiedzi

Zgłoszenie pomysłu przez zalogowanego mieszkańca zapisuje powiadomienia `NEW_IDEA` dla kont `STAFF` i
`ADMIN`. Personel widzi oczekujące pomysły przez `GET /api/v1/staff/ideas`
oraz swoje powiadomienia przez `GET /api/v1/notifications`. Odpowiedź tworzy
`POST /api/v1/staff/ideas/{id}/replies` z `{"body":"..."}`. Właściciel
pomysłu dostaje powiadomienie `IDEA_REPLY` i czyta historię przez
`GET /api/v1/ideas/{id}/replies`. `PATCH /api/v1/notifications/{id}/read`
oznacza własne powiadomienie jako przeczytane. Odczyt cudzych odpowiedzi i
powiadomień jest zablokowany.

Odpowiedzi są dostępne w aplikacji dla właściciela z kontem. Nie ma jeszcze
wysyłki e-mail; powiadomienie wymaga otwarcia aplikacji.
