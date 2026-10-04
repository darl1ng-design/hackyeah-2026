import { api } from '../../api/client';
import { Button, Card, Icon, StatusBadge } from '../../components/ds';
import { PageState } from '../../components/PageState';
import { reportNo, similarityLabel } from '../../lib/format';
import { LABELS, REPORT_BADGE } from '../../lib/labels';
import { go } from '../../lib/router';
import { useApi } from '../../api/useApi';
import type { ScreenProps } from '../../screens';
import { useSession } from '../../session';
import s from './MatchResultPage.module.css';

export function MatchResultPage({ route }: ScreenProps) {
  const { regions, showToast } = useSession();
  const id = Number(route.params.id);
  const { data, loading, error } = useApi(
    () =>
      Promise.all([
        api.getMatch(id),
        api
          .resources()
          .then((r) => r.filter((x) => x.kind === 'CANVAS' || x.kind === 'BIBLIOTEKA').slice(0, 3))
          .catch(() => []),
      ]),
    [id],
  );
  if (!data) return <div className="container"><PageState loading={loading} error={error} /></div>;
  const [rep, res] = data;
  const regionLabel = (code: string) => regions.find((r) => r.code === code)?.label || '';

  const copyLink = () =>
    (navigator.clipboard ? navigator.clipboard.writeText(location.href) : Promise.reject()).then(
      () => showToast('Link do raportu skopiowany.', 'success'),
      () => showToast('Skopiuj adres z paska przeglądarki.'),
    );

  return (
    <div className={`container ${s.page}`}>
      <div className={`stack ${s.head}`} style={{ '--gap': 'var(--space-3)' } as React.CSSProperties}>
        <div className="row">
          <span className="mono">Raport {reportNo(rep.reportId)}</span>
          <StatusBadge status={REPORT_BADGE[rep.reportStatus]}>{LABELS.reportStatus[rep.reportStatus]}</StatusBadge>
        </div>
        <h1 className="h1">Twój problem dotyczy obszaru „{rep.area?.name ?? ''}”</h1>
        {rep.area?.description && <p className="lead">{rep.area.description}</p>}
        <div className="row">
          <Button variant="outline" size="sm" iconLeft="link" onClick={copyLink}>
            Kopiuj link
          </Button>
          <Button
            variant="ghost"
            size="sm"
            iconRight="arrow-right"
            onClick={() => go('/innowacje' + (rep.area ? '?areaId=' + rep.area.id : ''))}
          >
            Wszystkie w tym obszarze
          </Button>
        </div>
      </div>

      <section className="stack" style={{ '--gap': 'var(--space-4)' } as React.CSSProperties}>
        <h2 className="h3">Pasujące innowacje</h2>
        {rep.matches.length ? (
          <div className={s.list}>
            {rep.matches.map(({ innovation: i, why, similarity }) => (
              <Card
                key={i.id}
                eyebrow={similarityLabel(similarity)}
                title={i.title}
                footer={[regionLabel(i.region), LABELS.innovationStatus[i.status]].filter(Boolean).join(' · ')}
                onClick={() => go(`/innowacje/${i.id}?z=${rep.reportId}`)}
              >
                <span className={s.why}>
                  <strong>Dlaczego pasuje</strong>
                  <span>{why}</span>
                </span>
              </Card>
            ))}
          </div>
        ) : (
          <p className="muted">Nie znaleźliśmy innowacji pasujących do tego opisu.</p>
        )}
      </section>

      <section className={s.cta}>
        <h2 className="h3">Nie ma tu dokładnie tego, czego szukasz?</h2>
        <p>
          Rozwiń problem we własny pomysł — zespół Kompasu Małopolskiego go przeczyta i odpowie. Możesz też opisać problem
          inaczej.
        </p>
        <div className="row">
          <Button iconLeft="lightbulb" onClick={() => go('/pomysly/nowy?z=' + rep.reportId)}>
            Rozwiń w pomysł
          </Button>
          <Button variant="outline" onClick={() => go('/dopasuj')}>
            Nowe dopasowanie
          </Button>
        </div>
        {res.length > 0 && (
          <div className={s.res}>
            <span className="overline">Przydatne materiały</span>
            {res.map((x) => (
              <a key={x.id} href={x.url} target="_blank" rel="noopener" className="icon-link">
                {x.name}
                <Icon name="external-link" size={16} />
              </a>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
