import { useState, type FormEvent } from "react";
import { api } from "../../api/client";
import type { GrantApplication, GrantApplicationStatus, GrantField, GrantFieldType, GrantCallStatus } from "../../api/types";
import { useApi } from "../../api/useApi";
import { Button, Select, Textarea, TextField } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { errorMessage } from "../../lib/errors";
import { formatDateTime } from "../../lib/format";
import { loginHref } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import s from "../panel/Panel.module.css";

export function GrantCallsPage({ route }: ScreenProps) {
  const { me, role } = useSession();
  const { data, error, loading, reload } = useApi(() => api.grantCalls(), []);
  const selectedId = Number(route.params.id || 0);
  const selected = data?.find((call) => call.id === selectedId);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [errorText, setErrorText] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true); setErrorText(""); setMessage("");
    try {
      const result = await api.submitGrantApplication(selectedId, answers);
      setMessage(`Wniosek „${result.title}” został zapisany. Znajdziesz go w swoich wnioskach.`);
      reload();
    } catch (cause) { setErrorText(errorMessage(cause)); }
    finally { setBusy(false); }
  }

  if (!data) return <div className="container"><PageState loading={loading} error={error} /></div>;
  if (!selectedId) return <div className="container stack">
    <header className="stack"><h1 className="h1">Otwarte nabory</h1><p className="lead">Wybierz nabór, aby zapoznać się z formularzem i złożyć wniosek.</p></header>
    {!data.length ? <p className="empty">Obecnie nie ma otwartych naborów.</p> : <ul className="list">
      {data.map((call) => <li className="list-row" key={call.id}>
        <span className={s.rowMain}><span className={s.rowTitle}>{call.title}</span><span className={s.sub}>{call.payload.description}</span>
          <span className={s.sub}>Termin: {formatDateTime(call.payload.closesAt)}</span></span>
        <a href={`#/nabory/${call.id}`}>{me ? "Wypełnij wniosek" : "Zobacz i zaloguj się, aby złożyć"}</a>
      </li>)}
    </ul>}
  </div>;
  if (!selected) return <div className="container stack"><h1 className="h1">Nabór niedostępny</h1><p>Może został zamknięty lub usunięty.</p><a href="#/nabory">Wróć do naborów</a></div>;

  return <div className="container stack narrow">
    <a href="#/nabory">← Wróć do otwartych naborów</a>
    <header className="stack"><h1 className="h1">{selected.title}</h1><p className="lead">{selected.payload.description}</p>
      <p className="small muted">Wnioski przyjmujemy do {formatDateTime(selected.payload.closesAt)}.</p></header>
    {message ? <div role="status" className="box stack" style={{ padding: "var(--space-6)" }}><h2 className="h3">Wniosek przyjęty</h2><p>{message}</p><a href="#/moje-wnioski">Przejdź do moich wniosków</a></div> : !me ?
      <p><a href={"#" + loginHref(`/nabory/${selected.id}`)}>Zaloguj się, aby wypełnić wniosek.</a></p> : role !== "MEMBER" ?
      <p className="muted">Wniosek mogą złożyć zalogowani mieszkańcy i organizacje.</p> :
      <form className="stack box" style={{ padding: "var(--space-6)" }} onSubmit={submit}>
        {selected.payload.fields.map((field) => <GrantFieldInput key={field.key} field={field} value={answers[field.key] ?? ""}
          onChange={(value) => setAnswers((current) => ({ ...current, [field.key]: value }))} />)}
        <p className="small muted">Po wysłaniu zachowamy treść i wersję formularza użytą w tym naborze.</p>
        <p role="status" aria-live="polite">{busy ? "Wysyłam wniosek…" : ""}</p>
        {errorText && <p role="alert">{errorText}</p>}
        <Button type="submit" disabled={busy}>{busy ? "Wysyłam wniosek…" : "Złóż wniosek"}</Button>
      </form>}
  </div>;
}

function GrantFieldInput({ field, value, onChange }: { field: GrantField; value: string; onChange: (value: string) => void }) {
  const id = `grant-${field.key}`;
  if (field.type === "TEXTAREA") return <Textarea id={id} label={field.label} required={field.required} value={value} maxLength={10000} rows={5} onChange={(e) => onChange(e.target.value)} />;
  if (field.type === "SELECT") return <Select id={id} label={field.label} required={field.required} value={value} options={(field.options ?? []).map((option) => ({ value: option, label: option }))} placeholder="Wybierz odpowiedź" onChange={(e) => onChange(e.target.value)} />;
  if (field.type === "CHECKBOX") return <label className="row"><input id={id} type="checkbox" checked={value === "true"} required={field.required} onChange={(e) => onChange(String(e.target.checked))} /><span>{field.label}</span></label>;
  return <TextField id={id} label={field.label} required={field.required} type={field.type === "NUMBER" ? "number" : "text"} value={value} onChange={(e) => onChange(e.target.value)} />;
}

export function MyApplicationsPage() {
  const { data, error, loading } = useApi(() => api.myGrantApplications(), []);
  if (!data) return <div className="container"><PageState loading={loading} error={error} /></div>;
  return <div className="container stack"><h1 className="h1">Moje wnioski</h1>
    {!data.length ? <p className="empty">Nie złożono jeszcze wniosków. <a href="#/nabory">Zobacz otwarte nabory</a></p> :
      <ul className="list">{data.map((application) => <li className="list-row stack" key={application.id}>
        <h2 className="h3">{application.title}</h2><p>Status: {application.status}</p><p className="small muted">Złożono {formatDateTime(application.createdAt)}</p>
        <details><summary>Pokaż zapisane odpowiedzi</summary><dl>{Object.entries(application.payload.answers).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value}</dd></div>)}</dl></details>
      </li>)}</ul>}
  </div>;
}

const STATUSES: GrantApplicationStatus[] = ["SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED"];
export function StaffGrantApplicationsPage({ route }: ScreenProps) {
  const { data, error, loading, reload } = useApi(() => api.staffGrantApplications(), []);
  const selectedId = Number(route.params.id || 0);
  if (!data) return <PageState loading={loading} error={error} panel />;
  const rows = selectedId ? data.filter((row) => row.id === selectedId) : data;
  async function setStatus(application: GrantApplication, status: GrantApplicationStatus) {
    await api.grantApplicationStatus(application.id, status);
    reload();
  }
  return <div className={s.page}><header className={s.head}><h1 className="h1">Wnioski grantowe</h1><p className="muted">Weryfikuj zgłoszenia i aktualizuj ich status. Autor otrzyma powiadomienie.</p></header>
    {!rows.length ? <p className="empty">Brak wniosków do wyświetlenia.</p> : <ul className="list">{rows.map((application) => <li className="list-row stack" key={application.id}>
      <h2 className="h3">{application.title}</h2><p className="small muted">{application.author} · {formatDateTime(application.createdAt)}</p>
      <dl>{Object.entries(application.payload.answers).map(([key, value]) => <div key={key}><dt>{application.payload.formSnapshot.fields.find((field) => field.key === key)?.label ?? key}</dt><dd>{value}</dd></div>)}</dl>
      <Select id={`application-${application.id}-status`} label="Status wniosku" value={application.status} options={STATUSES.map((value) => ({ value, label: value }))}
        onChange={(event) => void setStatus(application, event.target.value as GrantApplicationStatus)} />
    </li>)}</ul>}
  </div>;
}

const localDateTime = (date: Date) => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
const defaultFields: GrantField[] = [
  { key: "problem", label: "Jaki problem rozwiązuje pomysł?", type: "TEXTAREA", required: true },
  { key: "approach", label: "Jakie rozwiązanie proponujesz?", type: "TEXTAREA", required: true },
  { key: "beneficiaries", label: "Komu pomoże innowacja?", type: "TEXT", required: true },
];

export function AdminGrantCallsPage() {
  const { data, error, loading, reload } = useApi(() => api.adminGrantCalls(), []);
  const [defaultWindow] = useState(() => {
    const now = new Date();
    return {
      opensAt: localDateTime(now),
      closesAt: localDateTime(new Date(now.getTime() + 7 * 86400000)),
    };
  });
  const [selectedId, setSelectedId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [opensAt, setOpensAt] = useState(defaultWindow.opensAt);
  const [closesAt, setClosesAt] = useState(defaultWindow.closesAt);
  const [status, setStatus] = useState<GrantCallStatus>("DRAFT");
  const [fields, setFields] = useState<GrantField[]>(defaultFields);
  const [notice, setNotice] = useState("");
  const [errorText, setErrorText] = useState("");
  const [saving, setSaving] = useState(false);
  if (!data) return <PageState loading={loading} error={error} panel />;
  const selected = data.find((call) => String(call.id) === selectedId);
  function choose(id: string) {
    setSelectedId(id); setNotice(""); setErrorText("");
    const call = data?.find((item) => String(item.id) === id);
    if (!call) { setTitle(""); setDescription(""); setStatus("DRAFT"); setFields(defaultFields); return; }
    setTitle(call.title); setDescription(call.payload.description); setStatus(call.status as GrantCallStatus);
    setOpensAt(localDateTime(new Date(call.payload.opensAt))); setClosesAt(localDateTime(new Date(call.payload.closesAt)));
    setFields(call.payload.fields);
  }
  async function save(event: FormEvent) {
    event.preventDefault(); setErrorText(""); setNotice(""); setSaving(true);
    try {
      const saved = await api.saveGrantCall(selected ? selected.id : null, {
        title, description, opensAt: new Date(opensAt).toISOString(), closesAt: new Date(closesAt).toISOString(), status, fields,
      });
      setNotice(`Zapisano nabór „${saved.title}”.`); setSelectedId(String(saved.id)); reload();
    } catch (cause) { setErrorText(errorMessage(cause)); }
    finally { setSaving(false); }
  }
  function updateField(index: number, patch: Partial<GrantField>) {
    setFields((current) => current.map((field, i) => i === index ? { ...field, ...patch } : field));
  }
  return <div className={s.page}><header className={s.head}><h1 className="h1">Konfiguracja naborów</h1><p className="muted">Twórz nabór i dopasuj pola formularza do jego zasad.</p></header>
    <Select id="grant-edit" label="Edytuj istniejący nabór" value={selectedId} options={data.map((call) => ({ value: String(call.id), label: `${call.title} · ${call.status}` }))}
      placeholder="Utwórz nowy nabór" onChange={(event) => choose(event.target.value)} />
    <form className="stack box" style={{ padding: "var(--space-6)" }} onSubmit={save}>
      <TextField id="grant-title" label="Nazwa naboru" required value={title} onChange={(e) => setTitle(e.target.value)} />
      <Textarea id="grant-description" label="Opis" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={4000} rows={3} />
      <div className="grid"><TextField id="grant-open" label="Otwarcie" type="datetime-local" required value={opensAt} onChange={(e) => setOpensAt(e.target.value)} />
        <TextField id="grant-close" label="Zamknięcie" type="datetime-local" required value={closesAt} onChange={(e) => setClosesAt(e.target.value)} /></div>
      <Select id="grant-status" label="Stan naboru" value={status} options={(["DRAFT", "OPEN", "CLOSED"] as const).map((value) => ({ value, label: value }))} onChange={(e) => setStatus(e.target.value as GrantCallStatus)} />
      <fieldset className="stack"><legend className="h3">Pola formularza</legend>
        {fields.map((field, index) => <fieldset className="box stack" style={{ padding: "var(--space-4)" }} key={`${index}-${field.key}`}>
          <legend>Pole {index + 1}</legend>
          <TextField id={`field-${index}-key`} label="Klucz techniczny (bez spacji)" required value={field.key} onChange={(e) => updateField(index, { key: e.target.value })} />
          <TextField id={`field-${index}-label`} label="Pytanie" required value={field.label} onChange={(e) => updateField(index, { label: e.target.value })} />
          <Select id={`field-${index}-type`} label="Rodzaj odpowiedzi" value={field.type} options={(["TEXT", "TEXTAREA", "NUMBER", "SELECT", "CHECKBOX"] as GrantFieldType[]).map((value) => ({ value, label: value }))} onChange={(e) => updateField(index, { type: e.target.value as GrantFieldType, options: [] })} />
          {field.type === "SELECT" && <TextField id={`field-${index}-options`} label="Opcje, oddzielone przecinkami" required value={(field.options ?? []).join(", ")} onChange={(e) => updateField(index, { options: e.target.value.split(",").map((value) => value.trim()).filter(Boolean) })} />}
          <label className="row"><input type="checkbox" checked={field.required} onChange={(e) => updateField(index, { required: e.target.checked })} /><span>Pole wymagane</span></label>
          <Button type="button" variant="secondary" onClick={() => setFields((current) => current.filter((_, i) => i !== index))}>Usuń pole</Button>
        </fieldset>)}
        <Button type="button" variant="secondary" onClick={() => setFields((current) => [...current, { key: `field_${current.length + 1}`, label: "Nowe pytanie", type: "TEXT", required: false }])}>Dodaj pytanie</Button>
      </fieldset>
      <p role="status" aria-live="polite">{saving ? "Zapisuję nabór…" : notice}</p>
      {errorText && <p role="alert">{errorText}</p>}
      <Button type="submit" disabled={saving}>{saving ? "Zapisuję…" : "Zapisz nabór"}</Button>
    </form>
  </div>;
}
