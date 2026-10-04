import { api } from "../../api/client";
import type { Moderation } from "../../api/types";
import { useApi } from "../../api/useApi";
import { PageState } from "../../components/PageState";
import { Badge, Icon, Tabs } from "../../components/ds";
import { formatDate } from "../../lib/format";
import { LABELS } from "../../lib/labels";
import { go } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import s from "./Panel.module.css";

const TABS: Record<Moderation, string> = {
  PENDING: "Oczekujące",
  APPROVED: "Zatwierdzone",
  REJECTED: "Odrzucone",
};
const KEYS = Object.keys(TABS) as Moderation[];

export function QueuePage({ route }: ScreenProps) {
  const q = route.query.status as Moderation;
  const status: Moderation = KEYS.includes(q) ? q : "PENDING";
  // All three lists for tab counts (same as prototype).
  const { data, error, loading } = useApi(() => Promise.all(KEYS.map((k) => api.staffIdeas(k))), []);

  if (!data) return <PageState loading={loading} error={error} panel />;
  const lists = Object.fromEntries(KEYS.map((k, i) => [k, data[i]])) as Record<Moderation, typeof data[0]>;
  const list = lists[status];

  return (
    <div className={s.page}>
      <div className={s.head}>
        <h1 className="h1">Pomysły do obsługi</h1>
        <p className="muted">
          Nowe pomysły czekają na sprawdzenie. Odpowiedź trafia do autora jako powiadomienie.
        </p>
      </div>
      <Tabs
        tabs={KEYS.map((k) => ({ value: k, label: TABS[k], count: lists[k].length }))}
        value={status}
        onChange={(v) => go("/panel/pomysly?status=" + v)}
      />
      <div className="list">
        {list.map((d) => (
          <a key={d.id} href={"#/panel/pomysly/" + d.id} className="list-row">
            <span className={s.rowMain}>
              <span className={s.rowTitle}>{d.title}</span>
              <span className={s.sub}>
                {d.author} · {formatDate(d.createdAt)}
              </span>
            </span>
            <Badge tone="neutral">{LABELS.stage[d.stage]}</Badge>
            <Icon name="chevron-right" size={20} />
          </a>
        ))}
        {!list.length && <p className={s.emptyRow}>Brak pomysłów w tej kategorii.</p>}
      </div>
    </div>
  );
}
