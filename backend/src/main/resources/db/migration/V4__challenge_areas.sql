-- Challenge areas from the Malopolska social-challenges map (ROPS). Seeded here so V4
-- can reference them; SeedRunner no longer inserts data, it only vector-indexes.
insert into challenge_area (name, description)
values ('Starzenie sie spoleczenstwa', 'Wsparcie osob starszych: samotnosc, opieke, aktywnosc, demencja.'),
       ('Zdrowie psychiczne', 'Kryzysy psychiczne, depresja, wsparcie mlodziezy i doroslych, przemoc.'),
       ('Wykluczenie cyfrowe', 'Nierowny dostep do kompetencji cyfrowych i e-uslug, szczegolnie seniorzy.'),
       ('Dostep do uslug spolecznych',
        'Koordynacja i dostepnosc uslug spolecznych, takze dla osob z szczególnymi potrzebami.'),
       ('Samotnosc', 'Izolacja spoleczna osob w kazdym wieku, zwlaszcza osob starszych i mlodziezy.'),
       ('Piecza zastepcza i rodzina',
        'Wsparcie dzieci w pieczy, rodzin biologicznych i zastepczych.') on conflict (name) do nothing;