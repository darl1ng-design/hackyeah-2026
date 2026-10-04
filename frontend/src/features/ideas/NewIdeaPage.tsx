import { useState, type CSSProperties } from "react";
import { ApiError, api } from "../../api/client";
import { useApi } from "../../api/useApi";
import type { IdeaRequest, Stage } from "../../api/types";
import {
  Alert,
  Breadcrumbs,
  Button,
  Icon,
  RadioGroup,
  TextField,
  Textarea,
} from "../../components/ds";
import { errorMessage, fieldErrors } from "../../lib/errors";
import { safeUrl } from "../../lib/format";
import { LABELS } from "../../lib/labels";
import { go, loginHref } from "../../lib/router";
import type { ScreenProps } from "../../screens";
import { useSession } from "../../session";
import { loadMatchDesc } from "../match/matchDesc";
import { AssistantChat } from "./AssistantChat";
import s from "./NewIdeaPage.module.css";

type Form = Required<IdeaRequest>;
const STAGES = (Object.keys(LABELS.stage) as Stage[]).map((k) => ({
  value: k,
  label: LABELS.stage[k],
  hint: LABELS.stageHint[k],
}));

export function NewIdeaPage({ route }: ScreenProps) {
  const { showToast, setMe } = useSession();
  const prefill = route.query.z ? loadMatchDesc(route.query.z) : "";
  const [f, setF] = useState<Form>({
    title: "",
    essence: "",
    targetGroup: "",
    stage: "MYSL",
    description: prefill,
  });
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [sending, setSending] = useState(false);
  const { data: resources } = useApi(() => api.resources().catch(() => []), []);
  const res = (resources ?? [])
    .filter((x) => (x.kind === "CANVAS" || x.kind === "BIBLIOTEKA") && safeUrl(x.url))
    .slice(0, 3);

  const set = (k: keyof Form) => (e: { target: { value: string } }) =>
    setF((p) => ({ ...p, [k]: e.target.value }));
  const errSummary = Object.values(errs).filter(Boolean).join(" ");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!f.title.trim()) return setErrs({ title: "Wpisz tytuł pomysłu." });
    setSending(true);
    setErrs({});
    try {
      const d = await api.createIdea(f);
      showToast(
        "Pomysł wysłany. Czeka na sprawdzenie przez zespół Kompasu Małopolskiego.",
        "success",
      );
      go("/pomysly/" + d.id);
    } catch (err) {
      setSending(false);
      if (err instanceof ApiError && err.status === 401) {
        setMe(null);
        return go(loginHref(route.path));
      }
      const fe = fieldErrors(err);
      setErrs(fe);
      if (!Object.keys(fe).length) showToast(errorMessage(err), "danger");
    }
  }

  const insert = (text: string) => {
    setF((p) => ({
      ...p,
      description: (p.description ? p.description + "\n\n" : "") + text,
    }));
    showToast("Wstawiono do pola „Opis”.");
  };

  return (
    <div className={`container ${s.page}`}>
      <div
        className="stack narrow"
        style={{ "--gap": "var(--space-2)" } as CSSProperties}
      >
        <Breadcrumbs
          items={[
            { label: "Start", href: "#/" },
            { label: "Pomysły", href: "#/pomysly" },
            { label: "Zgłoś pomysł" },
          ]}
        />
        <h1 className="h1">Zgłoś pomysł</h1>
        <p className="lead">
          Tylko tytuł jest wymagany. Pomysł sprawdzi zespół Kompasu
          Małopolskiego, a odpowiedź zobaczysz w powiadomieniach.
        </p>
      </div>
      <div className={s.layout}>
        <form className={s.form} onSubmit={submit} noValidate>
          {prefill && (
            <Alert tone="info" title="Wstawiliśmy opis z Twojego dopasowania">
              Możesz go dowolnie zmienić.
            </Alert>
          )}
          {errSummary && (
            <Alert tone="danger" title="Popraw zaznaczone pola">
              {errSummary}
            </Alert>
          )}
          <TextField
            id="i-title"
            label="Tytuł pomysłu"
            required
            hint="Krótko, czym jest pomysł. Np. „Bus na telefon dla seniorów”."
            value={f.title}
            onChange={set("title")}
            error={errs.title}
          />
          <Textarea
            id="i-essence"
            label="Istota pomysłu"
            optional
            hint="Jedno–dwa zdania: problem → rozwiązanie → zmiana."
            rows={3}
            maxLength={4000}
            value={f.essence}
            onChange={set("essence")}
            error={errs.essence}
          />
          <TextField
            id="i-target"
            label="Grupa docelowa"
            optional
            hint="Komu pomaga? Np. „osoby 70+ mieszkające samotnie”."
            value={f.targetGroup}
            onChange={set("targetGroup")}
            error={errs.targetGroup}
          />
          <RadioGroup
            name="stage"
            legend="Etap"
            options={STAGES}
            value={f.stage}
            onChange={(v) => setF((p) => ({ ...p, stage: v as Stage }))}
          />
          <Textarea
            id="i-desc"
            label="Opis"
            optional
            hint="Co konkretnie zrobisz, z kim i po czym poznasz, że działa."
            rows={7}
            maxLength={4000}
            value={f.description}
            onChange={set("description")}
            error={errs.description}
          />
          <div className="row">
            <Button type="submit" size="lg" iconRight="send" disabled={sending}>
              {sending ? "Wysyłamy…" : "Wyślij pomysł"}
            </Button>
            <Button variant="ghost" size="lg" onClick={() => go("/pomysly")}>
              Anuluj
            </Button>
          </div>
          {res.length > 0 && (
            <div className={s.res}>
              <span className="overline">Przydatne materiały</span>
              {res.map((x) => (
                <a
                  key={x.id}
                  href={safeUrl(x.url)}
                  target="_blank"
                  rel="noopener"
                  className="icon-link"
                >
                  {x.name}
                  <Icon name="external-link" size={16} />
                </a>
              ))}
            </div>
          )}
        </form>
        <AssistantChat ideaContext={f} onInsert={insert} />
      </div>
    </div>
  );
}
