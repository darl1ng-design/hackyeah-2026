import { useEffect } from "react";
import { api } from "../../api/client";
import type { Notification } from "../../api/types";
import { useApi } from "../../api/useApi";
import { Icon } from "../../components/ds";
import { PageState } from "../../components/PageState";
import { formatDateTime } from "../../lib/format";
import { go } from "../../lib/router";
import { useSession } from "../../session";
import s from "./NotificationsPage.module.css";

export function NotificationsPage() {
  const { setNotifs, role } = useSession();
  const { data, error, loading } = useApi(() => api.notifications(), []);
  useEffect(() => {
    if (data) setNotifs(data); // keep header bell in sync with the fresh list
  }, [data, setNotifs]);

  async function open(n: Notification) {
    if (!n.read && data) {
      try {
        await api.markRead(n.id);
        setNotifs(data.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      } catch {
        /* still navigate; the bell refreshes on next load */
      }
    }
    switch (n.targetType) {
      case "IDEA":
        go(role === "STAFF" || role === "ADMIN" ? `/panel/pomysly/${n.targetId}` : `/pomysly/${n.targetId}?odpowiedzi=1`);
        break;
      case "MENTOR_CONVERSATION":
        go(role === "STAFF" || role === "ADMIN" ? `/panel/mentorzy/${n.targetId}` : `/mentorzy/${n.targetId}`);
        break;
      case "GRANT_APPLICATION":
        go(role === "STAFF" || role === "ADMIN"
          ? `/panel/wnioski/${n.targetId}`
          : "/moje-wnioski");
        break;
      case "TESTER_FEEDBACK":
        go(`/panel/testerzy/${n.targetId}`);
        break;
    }
  }

  const unread = data?.filter((n) => !n.read).length ?? 0;

  return (
    <div className="container">
      <div
        className="stack narrow"
        style={{ "--gap": "var(--space-5)" } as React.CSSProperties}
      >
        <h1 className="h1">Powiadomienia</h1>
        {!data ? (
          <PageState loading={loading} error={error} />
        ) : data.length ? (
          <>
            <p className="muted">
              {unread
                ? `Nieprzeczytane: ${unread}.`
                : "Wszystkie powiadomienia są przeczytane."}
            </p>
            <ul className={s.list}>
              {data.map((n) => (
                <li key={n.id} className={s.item}>
                  <button
                    type="button"
                    className={s.button}
                    onClick={() => open(n)}
                  >
                    <span
                      aria-hidden="true"
                      className={n.read ? s.dot : `${s.dot} ${s.dotUnread}`}
                    />
                    <span className={s.text}>
                      <span
                        className={
                          n.read ? s.title : `${s.title} ${s.titleUnread}`
                        }
                      >
                        {n.title}
                      </span>
                      <span className="small muted">
                        {formatDateTime(n.createdAt)} ·{" "}
                        {n.read ? "przeczytane" : "nowe"}
                      </span>
                    </span>
                    <Icon name="chevron-right" size={20} />
                  </button>
                </li>
              ))}
            </ul>
          </>
        ) : (
          <div className="empty">
            <Icon name="bell" size={40} color="var(--gray-600)" />
            <p className="muted">
              Nie masz powiadomień. Pojawią się tu informacje o Twoich
              zgłoszeniach i wiadomościach od zespołu.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
