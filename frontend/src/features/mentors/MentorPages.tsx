import { useState, type FormEvent } from "react";
import { api } from "../../api/client";
import { useApi } from "../../api/useApi";
import { Button, TextField, Textarea } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { errorMessage } from "../../lib/errors";
import { formatDateTime } from "../../lib/format";
import { go } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { Dictation } from "../dictation/Dictation";
import { useInsertDictated } from "../dictation/useInsertDictated";
import s from "../panel/Panel.module.css";

// Subject from the dictated message when the user left it empty: first sentence, max 120 chars.
const subjectFrom = (text: string) => {
  const first = text.trim().split(/(?<=[.!?])\s/)[0];
  return first.length > 120 ? first.slice(0, 117).trimEnd() + "…" : first;
};

export function MentorHomePage() {
  const { data, error, loading } = useApi(() => api.myMentorConversations(), []);
  const [subject, setSubject] = useState(""); const [body, setBody] = useState("");
  const [errorText, setErrorText] = useState(""); const [busy, setBusy] = useState(false);
  const dictate = useInsertDictated();
  if (!data) return <div className="container"><PageState loading={loading} error={error} /></div>;
  const dictateBoth = (text: string) => {
    if (!subject.trim()) setSubject(subjectFrom(text));
    dictate(() => body, setBody, true)(text);
  };
  async function open(event: FormEvent) {
    event.preventDefault(); setBusy(true); setErrorText("");
    try { const thread = await api.openMentorConversation(subject, body); go(`/mentorzy/${thread.id}`); }
    catch (cause) { setErrorText(errorMessage(cause)); }
    finally { setBusy(false); }
  }
  return <div className="container stack">
    <header className="stack"><h1 className="h1">Porozmawiaj z ekspertem</h1><p className="lead">Zadaj pytanie zespołowi Hubu. Twoja rozmowa jest widoczna tylko dla Ciebie i pracowników.</p></header>
    <form className="box stack" style={{ padding: "var(--space-6)" }} onSubmit={open}>
      <h2 className="h3">Nowa rozmowa</h2>
      <Dictation big label="Wiadomość do Hubu" fieldId="mentor-message" bigLabel="Powiedz, w czym możemy pomóc"
        bigHint="Wpiszemy wiadomość, a pierwsze zdanie posłuży za temat, jeśli jest pusty. Wszystko możesz poprawić." onText={dictateBoth} />
      <TextField id="mentor-subject" label="Temat" required value={subject} onChange={(e) => setSubject(e.target.value)} />
      <Dictation label="Temat" fieldId="mentor-subject" onText={dictate(() => subject, setSubject)} />
      <Textarea id="mentor-message" label="Wiadomość" required maxLength={4000} rows={5} value={body} onChange={(e) => setBody(e.target.value)} />
      <Dictation label="Wiadomość" fieldId="mentor-message" onText={dictate(() => body, setBody, true)} />
      <p role="status" aria-live="polite">{busy ? "Wysyłam wiadomość…" : ""}</p>
      {errorText && <p role="alert">{errorText}</p>}
      <Button type="submit" disabled={busy}>{busy ? "Wysyłam…" : "Wyślij do Hubu"}</Button>
    </form>
    <section className="stack" aria-labelledby="threads-title"><h2 id="threads-title" className="h2">Twoje rozmowy</h2>
      {!data.length ? <p className="empty">Nie masz jeszcze rozmów z zespołem.</p> : <ul className="list">{data.map((thread) => <li key={thread.id} className="list-row">
        <span className={s.rowMain}><span className={s.rowTitle}>{thread.subject}</span><span className={s.sub}>{thread.status} · {formatDateTime(thread.createdAt)}</span></span>
        <a href={`#/mentorzy/${thread.id}`}>Otwórz rozmowę</a>
      </li>)}</ul>}
    </section>
  </div>;
}

export function MentorDetailPage({ route }: ScreenProps) {
  const id = Number(route.params.id); const { data, error, loading, reload } = useApi(() => api.mentorConversation(id), [id]);
  const [body, setBody] = useState(""); const [errorText, setErrorText] = useState(""); const [busy, setBusy] = useState(false);
  const dictate = useInsertDictated();
  if (!data) return <div className="container"><PageState loading={loading} error={error} /></div>;
  async function send(event: FormEvent) {
    event.preventDefault(); setBusy(true); setErrorText("");
    try { await api.sendMentorMessage(id, body); setBody(""); reload(); }
    catch (cause) { setErrorText(errorMessage(cause)); }
    finally { setBusy(false); }
  }
  return <div className="container stack narrow"><a href="#/mentorzy">← Wróć do rozmów</a><header><h1 className="h1">{data.subject}</h1><p className="small muted">Status: {data.status}</p></header>
    <ol className="list">{data.messages.map((message) => <li key={message.id} className="list-row stack">
      <p><strong>{message.author}</strong> · {message.authorRole === "MEMBER" ? "Ty" : "Zespół Hubu"}</p><p className="prose">{message.body}</p><time className="small muted">{formatDateTime(message.createdAt)}</time>
    </li>)}</ol>
    {data.status === "OPEN" && <form className="stack" onSubmit={send}><Textarea id="mentor-reply" label="Twoja wiadomość" required maxLength={4000} rows={4} value={body} onChange={(e) => setBody(e.target.value)} />
      <Dictation label="Twoja wiadomość" fieldId="mentor-reply" onText={dictate(() => body, setBody, true)} />
      <p role="status" aria-live="polite">{busy ? "Wysyłam wiadomość…" : ""}</p>{errorText && <p role="alert">{errorText}</p>}<Button type="submit" disabled={busy}>{busy ? "Wysyłam…" : "Wyślij odpowiedź"}</Button></form>}
  </div>;
}

export function MentorQueuePage({ route }: ScreenProps) {
  const selectedId = Number(route.params.id || 0);
  const queue = useApi(() => api.staffMentorConversations(), []);
  const detail = useApi(() => selectedId ? api.staffMentorConversation(selectedId) : Promise.resolve(null), [selectedId]);
  const [body, setBody] = useState(""); const [errorText, setErrorText] = useState(""); const [busy, setBusy] = useState(false);
  const dictate = useInsertDictated();
  if (!queue.data) return <PageState loading={queue.loading} error={queue.error} panel />;
  async function reply(event: FormEvent) {
    event.preventDefault(); setBusy(true); setErrorText("");
    try { await api.replyMentorConversation(selectedId, body); setBody(""); detail.reload(); queue.reload(); }
    catch (cause) { setErrorText(errorMessage(cause)); }
    finally { setBusy(false); }
  }
  return <div className={s.page}><header className={s.head}><h1 className="h1">Rozmowy z mieszkańcami</h1><p className="muted">Odpowiedzi są zapisywane w wątku, a autor dostaje powiadomienie.</p></header>
    {!queue.data.length ? <p className="empty">Brak nowych rozmów.</p> : <ul className="list">{queue.data.map((thread) => <li className="list-row" key={thread.id}>
      <span className={s.rowMain}><span className={s.rowTitle}>{thread.subject}</span><span className={s.sub}>{thread.author} · {formatDateTime(thread.createdAt)}</span></span>
      <a href={`#/panel/mentorzy/${thread.id}`}>Odpowiedz</a>
    </li>)}</ul>}
    {selectedId > 0 && (detail.data ? <section className="stack box" style={{ padding: "var(--space-6)" }} aria-labelledby="selected-thread">
      <h2 id="selected-thread" className="h2">{detail.data.subject}</h2>
      <ol className="stack">{detail.data.messages.map((message) => <li key={message.id}><strong>{message.author}</strong> · {message.authorRole}<p className="prose">{message.body}</p></li>)}</ol>
      {detail.data.status === "OPEN" && <form className="stack" onSubmit={reply}><Textarea id="staff-reply" label="Odpowiedź" required rows={4} maxLength={4000} value={body} onChange={(e) => setBody(e.target.value)} />
        <Dictation label="Odpowiedź" fieldId="staff-reply" onText={dictate(() => body, setBody, true)} />
        <p role="status" aria-live="polite">{busy ? "Wysyłam wiadomość…" : ""}</p>{errorText && <p role="alert">{errorText}</p>}<Button type="submit" disabled={busy}>{busy ? "Wysyłam…" : "Wyślij odpowiedź"}</Button></form>}
    </section> : <PageState loading={detail.loading} error={detail.error} panel />)}
  </div>;
}
