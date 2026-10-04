import { useState } from "react";
import { api } from "../../api/client";
import type { Moderation } from "../../api/types";
import { useApi } from "../../api/useApi";
import { PageState } from "../../components/PageState";
import { Badge, Button, Icon, Textarea } from "../../components/ds";
import { errorMessage, fieldErrors } from "../../lib/errors";
import { formatDateTime } from "../../lib/format";
import { LABELS, MOD_TONE } from "../../lib/labels";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import s from "./Panel.module.css";

const MOD_TEXT: Record<Moderation, string> = {
  PENDING: "Pomysł czeka na decyzję. Po zatwierdzeniu będzie widoczny w banku pomysłów.",
  APPROVED: "Pomysł jest widoczny publicznie w banku pomysłów.",
  REJECTED: "Pomysł jest odrzucony — widzi go tylko autor.",
};
const MOD_TOAST: Record<Moderation, string> = {
  APPROVED: "Pomysł zatwierdzony i widoczny w banku pomysłów.",
  REJECTED: "Pomysł odrzucony. Autor dostanie powiadomienie.",
  PENDING: "Pomysł wrócił do sprawdzenia.",
};

export function QueueIdeaPage({ route }: ScreenProps) {
  const id = +route.params.id;
  const { role, showToast } = useSession();
  const isAdmin = role === "ADMIN";
  const { data, error, loading, setData } = useApi(
    () => Promise.all([api.idea(id), api.replies(id)]),
    [id],
  );
  const [reply, setReply] = useState("");
  const [replyErr, setReplyErr] = useState("");
  const [sending, setSending] = useState(false);
  const [modSaving, setModSaving] = useState(false);

  if (!data) return <PageState loading={loading} error={error} panel />;
  const [p, replies] = data;

  const sendReply = async () => {
    const body = reply.trim();
    if (!body) return setReplyErr("Wpisz treść odpowiedzi.");
    setSending(true);
    setReplyErr("");
    try {
      const r = await api.reply(p.id, body);
      setData([p, [...replies, r]]);
      setReply("");
      showToast("Odpowiedź wysłana. Autor dostanie powiadomienie.", "success");
    } catch (e) {
      const f = fieldErrors(e);
      if (f.body) setReplyErr(f.body);
      else showToast(errorMessage(e), "danger");
    } finally {
      setSending(false);
    }
  };

  const moderate = async (status: Moderation) => {
    setModSaving(true);
    try {
      const d = await api.moderate(p.id, status);
      setData([d, replies]);
      showToast(MOD_TOAST[status], "success");
    } catch (e) {
      showToast(errorMessage(e), "danger");
    } finally {
      setModSaving(false);
    }
  };

  const sections = (
    [
      ["Istota", p.essence],
      ["Grupa docelowa", p.targetGroup],
      ["Opis", p.description],
    ] as const
  ).filter(([, t]) => t);
  const st = p.moderationStatus;

  return (
    <div className={s.page}>
      <a href="#/panel/pomysly" className="icon-link">
        <Icon name="arrow-left" size={16} />
        Wróć do listy
      </a>
      <div className="row" style={{ "--gap": "var(--space-2)" } as React.CSSProperties}>
        <Badge tone={MOD_TONE[st]}>{LABELS.moderation[st]}</Badge>
        <Badge tone="neutral">Etap: {LABELS.stage[p.stage]}</Badge>
      </div>
      <h1 className="h1">{p.title}</h1>
      <span className={s.sub}>
        {p.author} · zgłoszono {formatDateTime(p.createdAt)}
      </span>
      <div className={s.cols}>
        <div className={s.colMain}>
          {sections.length > 0 && (
            <div className={`${s.card} ${s.cardLg}`}>
              {sections.map(([label, text]) => (
                <section key={label} className={s.section}>
                  <h2 className="overline">{label}</h2>
                  <p className="prose">{text}</p>
                </section>
              ))}
            </div>
          )}
          <section className={s.replies} aria-labelledby="p-replies">
            <h2 id="p-replies" className="h3">
              Odpowiedzi do autora
            </h2>
            {replies.map((r) => (
              <article key={r.id} className={s.card}>
                <div className={s.replyHead}>
                  <strong>{r.author}</strong>
                  <span className={s.sub}>{formatDateTime(r.createdAt)}</span>
                </div>
                <p className="prose">{r.body}</p>
              </article>
            ))}
            {!replies.length && <p className="muted">Nikt jeszcze nie odpowiedział.</p>}
            <form
              className={s.card}
              onSubmit={(e) => {
                e.preventDefault();
                sendReply();
              }}
            >
              <Textarea
                id="p-reply"
                label="Twoja odpowiedź"
                hint="Autor zobaczy ją na stronie pomysłu i dostanie powiadomienie."
                rows={5}
                maxLength={4000}
                value={reply}
                onChange={(e) => {
                  setReply(e.target.value);
                  setReplyErr("");
                }}
                error={replyErr || undefined}
              />
              <div>
                <Button type="submit" iconLeft="send" disabled={sending}>
                  {sending ? "Wysyłamy…" : "Wyślij odpowiedź"}
                </Button>
              </div>
            </form>
          </section>
        </div>
        <aside className={`${s.card} ${s.aside}`} aria-labelledby="p-mod">
          <h2 id="p-mod" className="h4">
            Moderacja
          </h2>
          <p className={s.modText}>{MOD_TEXT[st]}</p>
          {isAdmin ? (
            <div className={s.actions}>
              {st !== "APPROVED" && (
                <Button iconLeft="check" fullWidth disabled={modSaving} onClick={() => moderate("APPROVED")}>
                  Zatwierdź
                </Button>
              )}
              {st !== "REJECTED" && (
                <Button variant="outline" iconLeft="x" fullWidth disabled={modSaving} onClick={() => moderate("REJECTED")}>
                  Odrzuć
                </Button>
              )}
              {st !== "PENDING" && (
                <Button variant="ghost" fullWidth disabled={modSaving} onClick={() => moderate("PENDING")}>
                  Przywróć do sprawdzenia
                </Button>
              )}
            </div>
          ) : (
            <p className={s.sub}>
              Pomysły zatwierdza i odrzuca administrator. Ty możesz odpowiedzieć autorowi.
            </p>
          )}
          {st === "APPROVED" && <a href={"#/pomysly/" + p.id}>Zobacz w serwisie</a>}
        </aside>
      </div>
    </div>
  );
}
