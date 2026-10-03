-- Official ROPS/hubMI.pl resources (decoded from the hackathon QR poster, hubMI.pl).
create table if not exists resource (
    id bigserial primary key,
    name varchar(255) not null,
    url varchar(500) not null,
    kind varchar(50) not null   -- BIBLIOTEKA | RAPORTY | STATYSTYKI | MAPA | PUBLIKACJE | CANVAS
);

insert into resource (name, url, kind) values
 ('Biblioteka Innowacji Społecznych',
  'https://rops.krakow.pl/innowacje-spoleczne/biblioteka-innowacji-spolecznych/kategorie', 'BIBLIOTEKA'),
 ('Baza Raportów',
  'https://rops.krakow.pl/badania-analizy-raporty/raporty-z-badan', 'RAPORTY'),
 ('Internetowy Obserwator Statystyk Społecznych',
  'https://obserwator.rops.krakow.pl/', 'STATYSTYKI'),
 ('Mapa Wyzwań Społecznych',
  'https://rops.krakow.pl/mpliki/IS/IWS_20/za._nr_2._Mapa_Wyzwa_Spoecznych.pdf', 'MAPA'),
 ('Publikacje ze Świata Innowacji',
  'https://rops.krakow.pl/innowacje-spoleczne/publikacje-ze-swiata-innowacji', 'PUBLIKACJE'),
 ('Canvas Innowacji (AGH Social Canvas)',
  'https://rops.krakow.pl/mpliki/IS/Moj_folder/INNO_AGH_-_SOCIAL_CANVAS.pdf', 'CANVAS')
on conflict do nothing;