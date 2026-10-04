import { api } from "../../api/client";
import { useApi } from "../../api/useApi";
import { Card, SearchField } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { plural } from "../../lib/format";
import { go } from "../../lib/router";
import { useSession } from "../../session";
import { InnovationCard } from "../innovations/InnovationCard";
import s from "./StartPage.module.css";

export function StartPage() {
  const { areas } = useSession();
  const { data, error, loading } = useApi(
    () => Promise.all([api.innovations({ size: 3 }), api.ideas()]),
    [],
  );
  if (!data)
    return (
      <div className="container">
        <PageState loading={loading} error={error} />
      </div>
    );
  const [latest, ideas] = data;
  const innTotal = latest.totalElements;

  return (
    <>
      <section className={s.hero}>
        <div className={s.heroInner}>
          <h1 className={s.title}>
            Sprawdzone rozwiązania dla Twojej społeczności
          </h1>
          <p className={`lead ${s.heroLead}`}>
            Przeglądaj innowacje społeczne z całej Polski, dopasuj je do
            problemu w swojej okolicy albo zgłoś własny pomysł.
          </p>
          <div className={s.search}>
            <SearchField
              size="lg"
              label="Szukaj innowacji"
              placeholder="Np. samotność seniorów, transport, ogrody szkolne"
              buttonLabel="Szukaj"
              onSubmit={(v) =>
                go(
                  "/innowacje" +
                    (v.trim() ? "?q=" + encodeURIComponent(v.trim()) : ""),
                )
              }
            />
          </div>
        </div>
      </section>

      <section className={s.section}>
        <h2 className="h2">Od czego chcesz zacząć?</h2>
        <div className="grid">
          <Card
            icon="search"
            title="Odkryj innowacje"
            footer={`${innTotal} ${plural(innTotal, "innowacja", "innowacje", "innowacji")} w katalogu`}
            href="#/innowacje"
          >
            Przeglądaj sprawdzone rozwiązania według obszaru, regionu lub frazy.
          </Card>
          <Card
            icon="target"
            title="Dopasuj do problemu"
            footer="Bez zakładania konta"
            href="#/dopasuj"
          >
            Opisz, co dzieje się w Twojej okolicy, a pokażemy pasujące
            rozwiązania.
          </Card>
          <Card
            icon="lightbulb"
            title="Zgłoś własny pomysł"
            footer={`${ideas.length} ${plural(ideas.length, "pomysł", "pomysły", "pomysłów")} w banku`}
            href="#/pomysly/nowy"
          >
            Spisz pomysł z pomocą asystenta. Zespół Kompasu Małopolskiego go
            przeczyta i odpowie.
          </Card>
        </div>
      </section>

      {areas.length > 0 && (
        <section className={s.section}>
          <h2 className="h2">Obszary</h2>
          <div className="grid">
            {areas.map((a) => (
              <Card
                key={a.id}
                title={a.name}
                padding={20}
                href={`#/innowacje?areaId=${a.id}`}
              >
                {a.description}
              </Card>
            ))}
          </div>
        </section>
      )}

      <section className={s.section}>
        <div className={s.sectionHead}>
          <h2 className="h2">Najnowsze w katalogu</h2>
          <a href="#/innowacje">Zobacz cały katalog</a>
        </div>
        {latest.content.length ? (
          <div className="grid">
            {latest.content.map((i) => (
              <InnovationCard key={i.id} innovation={i} />
            ))}
          </div>
        ) : (
          <p className="muted">Katalog jest jeszcze pusty.</p>
        )}
      </section>
    </>
  );
}
