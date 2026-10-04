import { api } from "../../api/client";
import { PageState } from "../../components/PageState";
import { useApi } from "../../api/useApi";
import { formatDateTime } from "../../lib/format";
import type { ScreenProps } from "../../screens";
import s from "../panel/Panel.module.css";

export function TesterQueuePage({ route }: ScreenProps) {
  const { data, error, loading } = useApi(() => api.testerQueue(), []);
  if (!data) return <PageState loading={loading} error={error} panel />;
  const selected = Number(route.params.id || 0);
  const rows = selected ? data.filter((row) => row.id === selected) : data;
  return <div className={s.page}>
    <header className={s.head}><h1 className="h1">Zgłoszenia testerów</h1><p className="muted">Opinie i propozycje usprawnień od osób testujących innowacje.</p></header>
    {!rows.length ? <p className="empty">Brak zgłoszeń testerów.</p> : <ul className="list">
      {rows.map((row) => <li key={row.id} className="list-row stack">
        <h2 className="h3">{row.title}</h2><p className="small muted">{row.author ?? "Użytkownik"} · {formatDateTime(row.createdAt)}</p>
        <p><strong>Ocena:</strong> {String(row.payload.rating ?? "brak")}</p>
        <p><strong>Opinia:</strong> {String(row.payload.feedback || "—")}</p>
        <p><strong>Propozycja usprawnienia:</strong> {String(row.payload.suggestion || "—")}</p>
      </li>)}
    </ul>}
  </div>;
}
