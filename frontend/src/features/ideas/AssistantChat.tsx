// "Asystent pomysłu" aside: chat with the backend assistant, aware of the current idea form.
import { useEffect, useRef, useState } from "react";
import { api } from "../../api/client";
import type { ChatMessage, IdeaRequest } from "../../api/types";
import { Button, Icon, Tag, TextField } from "../../components/ds";
import { useSession } from "../../session";
import { DictationButton } from "../dictation/DictationButton";
import { appendText } from "../dictation/useDictation";
import s from "./NewIdeaPage.module.css";

const STARTERS = [
  "Jak opisać grupę docelową?",
  "Jak zapisać istotę pomysłu?",
  "Który etap wybrać?",
];

type Props = { ideaContext: IdeaRequest; onInsert: (text: string) => void };

export function AssistantChat({ ideaContext, onInsert }: Props) {
  const { showToast } = useSession();
  const [chat, setChat] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const log = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (log.current) log.current.scrollTop = log.current.scrollHeight;
  }, [chat, sending]);

  async function send(text?: string) {
    const message = (text ?? input).trim();
    if (!message || sending) return;
    const history = chat.slice(-20);
    setChat((c) => [...c, { role: "USER", content: message }]);
    setInput("");
    setSending(true);
    try {
      const r = await api.assistant({ message, history, ideaContext });
      setChat((c) => [...c, { role: "ASSISTANT", content: r.reply }]);
    } catch {
      showToast("Asystent nie odpowiedział. Spróbuj ponownie.", "danger");
    } finally {
      setSending(false);
    }
  }

  return (
    <aside aria-label="Asystent pomysłu" className={s.aside}>
      <div className={s.asideHead}>
        <span className={s.asideIcon}>
          <Icon name="message-circle" size={20} />
        </span>
        <span className={s.asideTitle}>
          <strong>Asystent pomysłu</strong>
          <span className="small muted">
            Zna treść formularza i pamięta rozmowę.
          </span>
        </span>
      </div>
      <div ref={log} aria-live="polite" className={s.log}>
        {!chat.length && (
          <>
            <p className="small muted">
              Zapytaj o cokolwiek, co pomoże opisać pomysł. Na przykład:
            </p>
            <div
              className="row"
              style={{ "--gap": "var(--space-2)" } as React.CSSProperties}
            >
              {STARTERS.map((t) => (
                <Tag key={t} onClick={() => send(t)}>
                  {t}
                </Tag>
              ))}
            </div>
          </>
        )}
        {chat.map((m, i) =>
          m.role === "USER" ? (
            <div key={i} className={s.userMsg}>
              {m.content}
            </div>
          ) : (
            <div key={i} className={s.botMsg}>
              <span>{m.content}</span>
              <div>
                <Button
                  variant="ghost"
                  size="sm"
                  iconLeft="corner-down-left"
                  onClick={() => onInsert(m.content)}
                >
                  Wstaw do opisu
                </Button>
              </div>
            </div>
          ),
        )}
        {sending && (
          <div role="status" className="small muted">
            Asystent pisze odpowiedź…
          </div>
        )}
      </div>
      <div
        className={s.composer}
        onKeyDown={(e) => {
          if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            send();
          }
        }}
      >
        <TextField
          id="chat-input"
          label="Twoje pytanie"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          style={{ flex: 1, minWidth: 0 }}
        />
        <DictationButton label="Podyktuj pytanie" onText={(t) => setInput((i) => appendText(i, t))} />
        <Button
          variant="secondary"
          iconLeft="send"
          disabled={sending}
          onClick={() => send()}
        >
          Wyślij
        </Button>
      </div>
    </aside>
  );
}
