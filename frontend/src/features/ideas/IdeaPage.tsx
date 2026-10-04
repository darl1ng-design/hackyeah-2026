import { useEffect, type CSSProperties } from "react";
import { api } from "../../api/client";
import { useApi } from "../../api/useApi";
import type { Moderation } from "../../api/types";
import { Alert, Badge, Breadcrumbs } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { formatDate, formatDateTime } from "../../lib/format";
import { LABELS } from "../../lib/labels";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import s from "./IdeaPage.module.css";

const MOD_ALERT: Record<
  Moderation,
  { tone: "warning" | "success" | "danger"; title: string; text: string }
> = {
  PENDING: {
    tone: "warning",
    title: "Pomysł czeka na sprawdzenie",
    text: "Zespół Kompasu Małopolskiego sprawdza pomysły zwykle w ciągu 5 dni roboczych. Do tego czasu widzisz go tylko Ty.",
  },
  APPROVED: {
    tone: "success",
    title: "Pomysł jest widoczny w banku pomysłów",
    text: "Każdy może go zobaczyć na liście pomysłów.",
  },
  REJECTED: {
    tone: "danger",
    title: "Pomysł nie został zatwierdzony",
    text: "Sprawdź odpowiedź zespołu poniżej. Możesz zgłosić poprawioną wersję jako nowy pomysł.",
  },
};

export function IdeaPage({ route }: ScreenProps) {
  const { me, role } = useSession();
  const id = Number(route.params.id);
  // Replies are visible only to the author and staff; 401/403 (or any failure) just hides the section.
  const { data, loading, error } = useApi(
    () =>
      Promise.all([
        api.idea(id),
        me ? api.replies(id).catch(() => null) : Promise.resolve(null),
      ]),
    [id, me?.username],
  );
  const scrollToReplies = !!data && !!route.query.odpowiedzi;
  useEffect(() => {
    if (scrollToReplies)
      document.getElementById("odpowiedzi")?.scrollIntoView();
  }, [scrollToReplies]);

  if (!data)
    return (
      <div className="container">
        <PageState loading={loading} error={error} />
      </div>
    );
  const [d, replies] = data;
  const mod = MOD_ALERT[d.moderationStatus];

  return (
    <div className="container">
      <article
        className="stack narrow"
        style={{ "--gap": "var(--space-5)" } as CSSProperties}
      >
        <Breadcrumbs
          items={[
            { label: "Start", href: "#/" },
            { label: "Pomysły", href: "#/pomysly" },
            { label: d.title },
          ]}
        />
        {replies !== null && role === "MEMBER" && (
          <Alert tone={mod.tone} title={mod.title}>
            {mod.text}
          </Alert>
        )}
        <div className="row">
          <Badge tone="neutral" icon="flag">
            Etap: {LABELS.stage[d.stage]}
          </Badge>
        </div>
        <h1 className="h1">{d.title}</h1>
        {d.essence && <p className="lead">{d.essence}</p>}
        <span className="small muted">
          Autor: {d.author} · zgłoszono {formatDate(d.createdAt)}
        </span>
        {d.targetGroup && (
          <section className={s.section}>
            <h2 className="h4">Grupa docelowa</h2>
            <p className="prose">{d.targetGroup}</p>
          </section>
        )}
        {d.description && (
          <section className={s.section}>
            <h2 className="h4">Opis</h2>
            <p className="prose">{d.description}</p>
          </section>
        )}
        {replies !== null && (
          <section id="odpowiedzi" className={s.replies}>
            <h2 className="h3">Odpowiedzi zespołu</h2>
            {replies.map((r) => (
              <article key={r.id} className={s.reply}>
                <div className={s.replyHead}>
                  <strong>{r.author}</strong>
                  <span className="small muted">
                    {formatDateTime(r.createdAt)}
                  </span>
                </div>
                <p className="prose">{r.body}</p>
              </article>
            ))}
            {!replies.length && (
              <p className="muted">
                Nie ma jeszcze odpowiedzi. Powiadomimy Cię, gdy się pojawi.
              </p>
            )}
          </section>
        )}
        <a href="#/pomysly" className={s.back}>
          Wróć do listy pomysłów
        </a>
      </article>
    </div>
  );
}
