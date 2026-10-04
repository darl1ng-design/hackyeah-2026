import { useState, type CSSProperties } from "react";
import { api } from "../../api/client";
import { useApi } from "../../api/useApi";
import { Alert, Badge, Button, Card, Icon, Tabs } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { formatDate } from "../../lib/format";
import { LABELS, MOD_TONE } from "../../lib/labels";
import { go, loginHref } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import s from "./IdeasPage.module.css";

export function IdeasPage({ route }: ScreenProps) {
  const { me } = useSession();
  const [tab, setTab] = useState(route.query.tab === "moje" ? "mine" : "all");
  const { data, loading, error } = useApi(
    () =>
      Promise.all([
        api.ideas(false),
        me ? api.ideas(true) : Promise.resolve([]),
      ]),
    [me?.username],
  );
  const mineTab = tab === "mine";
  const list = data ? (mineTab ? data[1] : data[0]) : [];

  return (
    <div className="container stack">
      <div className={s.head}>
        <div
          className="stack"
          style={{ "--gap": "var(--space-2)" } as CSSProperties}
        >
          <h1 className="h1">Pomysły mieszkańców</h1>
          <p className="lead">
            Pomysły zgłoszone przez mieszkańców i zatwierdzone przez zespół
            Kompasu Małopolskiego.
          </p>
        </div>
        <Button iconLeft="plus" onClick={() => go("/pomysly/nowy")}>
          Zgłoś pomysł
        </Button>
      </div>
      <Tabs
        tabs={[
          { value: "all", label: "Wszystkie", count: data?.[0].length },
          {
            value: "mine",
            label: "Moje",
            count: me ? data?.[1].length : undefined,
          },
        ]}
        value={tab}
        onChange={setTab}
      />
      {mineTab && !me ? (
        <div
          className="stack narrow"
          style={{ "--gap": "var(--space-4)" } as CSSProperties}
        >
          <Alert tone="info" title="Zaloguj się, żeby zobaczyć swoje pomysły">
            Tu znajdziesz wszystkie zgłoszone przez siebie pomysły — także te,
            które czekają na sprawdzenie.
          </Alert>
          <div className="row">
            <Button
              variant="secondary"
              onClick={() => go(loginHref("/pomysly?tab=moje"))}
            >
              Zaloguj się
            </Button>
            <Button variant="outline" onClick={() => go("/rejestracja")}>
              Załóż konto
            </Button>
          </div>
        </div>
      ) : !data ? (
        <PageState loading={loading} error={error} />
      ) : list.length ? (
        <div className="grid">
          {list.map((d) => (
            <Card
              key={d.id}
              eyebrow={LABELS.stage[d.stage]}
              title={d.title}
              footer={`${d.author} · ${formatDate(d.createdAt)}`}
              onClick={() => go("/pomysly/" + d.id)}
            >
              <span className={s.cardBody}>
                {mineTab && (
                  <Badge tone={MOD_TONE[d.moderationStatus]}>
                    {LABELS.moderation[d.moderationStatus]}
                  </Badge>
                )}
                {d.essence && <span>{d.essence}</span>}
              </span>
            </Card>
          ))}
        </div>
      ) : (
        <div className="empty">
          <Icon name="lightbulb" size={40} color="var(--gray-600)" />
          <h2 className="h3">
            {mineTab
              ? "Nie masz jeszcze pomysłów"
              : "Nie ma jeszcze zatwierdzonych pomysłów"}
          </h2>
          <p className="muted">
            {mineTab
              ? "Zgłoszone pomysły pojawią się tutaj razem ze statusem sprawdzenia."
              : "Zgłoś pomysł — po sprawdzeniu pojawi się na tej liście."}
          </p>
          <Button iconLeft="plus" onClick={() => go("/pomysly/nowy")}>
            Zgłoś pierwszy pomysł
          </Button>
        </div>
      )}
      <p className={`small muted ${s.nudge}`}>
        <Icon
          name="message-circle"
          size={16}
          style={{ marginTop: 3, flex: "none" }}
        />
        <span>
          Nie wiesz, od czego zacząć? Formularz pomysłu ma wbudowanego
          asystenta, który pomoże uporządkować myśli.
        </span>
      </p>
    </div>
  );
}
