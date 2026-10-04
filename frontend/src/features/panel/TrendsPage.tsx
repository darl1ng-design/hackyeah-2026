import { useState } from "react";
import { api } from "../../api/client";
import type { Trends } from "../../api/types";
import { useApi } from "../../api/useApi";
import { PageState } from "../../components/PageState";
import { Button, TextField } from "../../components/ds";
import { formatMonth } from "../../lib/format";
import { useSession } from "../../session";
import s from "./Panel.module.css";

const today = () => new Date().toISOString().slice(0, 10);

export function TrendsPage() {
  const { showToast } = useSession();
  const [from, setFrom] = useState("2026-04-01");
  const [to, setTo] = useState(today);
  const [range, setRange] = useState({ from, to });
  const { data, error, loading } = useApi(() => api.trends(range), [range.from, range.to]);

  const show = () => {
    if (from && to && from > to) return showToast("Data „Od” musi być wcześniejsza niż „Do”.", "danger");
    setRange({ from, to });
  };

  return (
    <div className={s.page}>
      <div className={s.head}>
        <h1 className="h1">Trendy</h1>
        <p className="muted">Raporty z dopasowań według obszaru, regionu, statusu i miesiąca.</p>
      </div>
      <form
        className={`${s.card} ${s.filterCard}`}
        onSubmit={(e) => {
          e.preventDefault();
          show();
        }}
      >
        <TextField
          id="tr-from"
          type="date"
          label="Od"
          value={from}
          onChange={(e) => setFrom(e.target.value)}
          style={{ width: 200, maxWidth: "100%" }}
        />
        <TextField
          id="tr-to"
          type="date"
          label="Do"
          value={to}
          onChange={(e) => setTo(e.target.value)}
          style={{ width: 200, maxWidth: "100%" }}
        />
        <Button type="submit" variant="secondary" disabled={loading}>
          Pokaż
        </Button>
      </form>
      {data ? <TrendsView tr={data} /> : <PageState loading={loading} error={error} panel />}
    </div>
  );
}

function TrendsView({ tr }: { tr: Trends }) {
  const blocks: [string, { label: string; count: number }[], string][] = [
    ["Według obszaru", tr.byArea, "var(--red-600)"],
    ["Według regionu", tr.byRegion, "var(--black)"],
    ["Według statusu", tr.byStatus, "var(--yellow-500)"],
    ["Według miesiąca", tr.byMonth.map((m) => ({ label: formatMonth(m.month), count: m.count })), "var(--gray-700)"],
  ];
  return (
    <>
      <div className={`${s.card} ${s.cardLg}`} style={{ gap: "var(--space-1)" }}>
        <span className="overline">Raporty w wybranym okresie</span>
        <span className={s.total}>{tr.total}</span>
      </div>
      <div className={s.blocks}>
        {blocks.map(([title, rows, color]) => {
          const max = Math.max(1, ...rows.map((x) => x.count));
          return (
            <section key={title} className={`${s.card} ${s.cardLg}`} style={{ gap: "var(--space-3)" }}>
              <h2 className="h4">{title}</h2>
              {rows.length ? (
                <ul className={s.bars}>
                  {rows.map((r) => (
                    <li key={r.label} className={s.barRow}>
                      <span className={s.barLabel} title={r.label}>
                        {r.label}
                      </span>
                      <span className={s.barTrack} aria-hidden="true">
                        <span
                          className={s.barFill}
                          style={{ width: Math.round((r.count / max) * 100) + "%", "--bar": color } as React.CSSProperties}
                        />
                      </span>
                      <span className={s.barCount}>{r.count}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="muted">Brak danych w tym okresie.</p>
              )}
            </section>
          );
        })}
      </div>
    </>
  );
}
