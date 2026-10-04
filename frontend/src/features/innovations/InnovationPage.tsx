import { api } from "../../api/client";
import { useApi } from "../../api/useApi";
import { Badge, Breadcrumbs, Button, Icon, Tag } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { formatDate } from "../../lib/format";
import { INN_STATUS_TONE, LABELS } from "../../lib/labels";
import { go } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import { InnovationCard } from "./InnovationCard";
import s from "./InnovationPage.module.css";

export function InnovationPage({ route }: ScreenProps) {
  const { regions } = useSession();
  const id = Number(route.params.id);
  const reportId = route.query.z;
  const { data, error, loading } = useApi(async () => {
    const inn = await api.innovation(id);
    const sim = inn.area
      ? (await api.innovations({ areaId: inn.area.id, size: 4 })).content
      : [];
    return { inn, similar: sim.filter((x) => x.id !== inn.id).slice(0, 3) };
  }, [id]);

  if (!data)
    return (
      <div className="container">
        <PageState loading={loading} error={error} />
      </div>
    );
  const { inn: i, similar } = data;
  const areaName = i.area?.name ?? "Bez obszaru";
  const areaHref = "/innowacje" + (i.area ? "?areaId=" + i.area.id : "");
  const facts = [
    ["Obszar", i.area?.name || "—"],
    ["Region", regions.find((r) => r.code === i.region)?.label || "—"],
    ["Status", LABELS.innovationStatus[i.status]],
    ["Grupa docelowa", i.targetGroup || "—"],
    ["Dodano", formatDate(i.createdAt)],
  ];

  return (
    <div
      className="container stack"
      style={{ "--gap": "var(--space-8)" } as React.CSSProperties}
    >
      <div
        className="stack"
        style={{ "--gap": "var(--space-3)" } as React.CSSProperties}
      >
        <Breadcrumbs
          items={[
            { label: "Start", href: "#/" },
            { label: "Katalog", href: "#/innowacje" },
            ...(i.area ? [{ label: i.area.name, href: "#" + areaHref }] : []),
            { label: i.title },
          ]}
        />
        {reportId && (
          <a href={`#/dopasuj/${reportId}`} className="icon-link">
            <Icon name="arrow-left" size={16} />
            Wróć do wyniku dopasowania
          </a>
        )}
      </div>

      <div className={s.layout}>
        <article className={s.article}>
          <div
            className="row"
            style={{ "--gap": "var(--space-2)" } as React.CSSProperties}
          >
            <Badge tone={INN_STATUS_TONE[i.status]}>
              {LABELS.innovationStatus[i.status]}
            </Badge>
            <Tag onClick={() => go(areaHref)}>{areaName}</Tag>
          </div>
          <h1 className="h1">{i.title}</h1>
          <p className="lead">{i.summary}</p>
          <div className={`prose ${s.body}`}>{i.description}</div>
          {i.videoUrl && (
            <div className={s.video}>
              <Icon name="circle-play" size={48} />
              <strong>Wideo o innowacji</strong>
              <a href={i.videoUrl} target="_blank" rel="noopener noreferrer">
                Otwórz wideo w nowej karcie
              </a>
            </div>
          )}
          {i.sourceUrl && (
            <a
              href={i.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="icon-link"
            >
              Zobacz źródło
              <Icon name="external-link" size={16} />
            </a>
          )}
        </article>
        <aside className={s.aside}>
          <dl className={s.facts}>
            {facts.map(([label, value]) => (
              <div key={label} className={s.fact}>
                <dt className="overline">{label}</dt>
                <dd>{value}</dd>
              </div>
            ))}
          </dl>
        </aside>
      </div>

      <section className={s.cta} aria-labelledby="inn-cta">
        <div className={s.ctaText}>
          <span className={s.ctaOverline}>Co dalej</span>
          <h2 id="inn-cta" className={s.ctaTitle}>
            Masz podobny problem w swojej okolicy?
          </h2>
          <span className={s.ctaLead}>
            Opisz go, a dopasujemy więcej rozwiązań — albo zgłoś własny pomysł.
          </span>
        </div>
        <div
          className="row"
          style={{ "--gap": "var(--space-5)" } as React.CSSProperties}
        >
          <Button
            variant="accent"
            size="lg"
            iconRight="arrow-right"
            onClick={() => go("/dopasuj")}
          >
            Dopasuj rozwiązania
          </Button>
          <a href="#/pomysly/nowy" className={s.ctaLink}>
            Zgłoś własny pomysł
          </a>
        </div>
      </section>

      {similar.length > 0 && (
        <section
          className="stack"
          style={{ "--gap": "var(--space-4)" } as React.CSSProperties}
        >
          <h2 className="h3">Inne w obszarze „{areaName}”</h2>
          <div className="grid">
            {similar.map((x) => (
              <InnovationCard key={x.id} innovation={x} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
