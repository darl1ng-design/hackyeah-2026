import { api } from "../../api/client";
import type { ResourceKind } from "../../api/types";
import { useApi } from "../../api/useApi";
import { Icon } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { host } from "../../lib/format";
import { LABELS } from "../../lib/labels";
import s from "./ResourcesPage.module.css";

const KIND_ICON: Record<ResourceKind, string> = {
  BIBLIOTEKA: "book-open",
  RAPORTY: "file-text",
  STATYSTYKI: "percent",
  MAPA: "map",
  PUBLIKACJE: "newspaper",
  CANVAS: "layout-grid",
};

export function ResourcesPage() {
  const { data, error, loading } = useApi(() => api.resources(), []);
  const groups = (Object.keys(LABELS.resourceKind) as ResourceKind[])
    .map((k) => ({ kind: k, items: (data ?? []).filter((x) => x.kind === k) }))
    .filter((g) => g.items.length);

  return (
    <div
      className="container stack"
      style={{ "--gap": "var(--space-8)" } as React.CSSProperties}
    >
      <div
        className="stack narrow"
        style={{ "--gap": "var(--space-2)" } as React.CSSProperties}
      >
        <h1 className="h1">Zasoby</h1>
        <p className="lead">
          Materiały, które pomogą zaplanować i rozwinąć innowację społeczną.
          Linki otwierają się w nowej karcie.
        </p>
      </div>
      {!data ? (
        <PageState loading={loading} error={error} />
      ) : groups.length ? (
        <div className={s.groups}>
          {groups.map((g) => (
            <section key={g.kind} className={s.group}>
              <h2 className={`h3 ${s.groupTitle}`}>
                <span className={s.groupIcon}>
                  <Icon name={KIND_ICON[g.kind]} size={20} />
                </span>
                {LABELS.resourceKind[g.kind]}
              </h2>
              <ul className={s.list}>
                {g.items.map((x) => (
                  <li key={x.id} className={s.item}>
                    <a
                      href={x.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={s.link}
                    >
                      <span className={s.linkText}>
                        <span className={s.name}>{x.name}</span>
                        <span className="small muted">{host(x.url)}</span>
                      </span>
                      <Icon
                        name="external-link"
                        size={20}
                        color="var(--red-600)"
                        label="otwiera się w nowej karcie"
                      />
                    </a>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      ) : (
        <div className="empty">
          <Icon name="book-open" size={40} color="var(--gray-600)" />
          <p className="muted">Nie ma jeszcze opublikowanych zasobów.</p>
        </div>
      )}
    </div>
  );
}
