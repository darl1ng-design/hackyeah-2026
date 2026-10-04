// Dictation control placed under a field (prototype component "Dyktowanie").
// big: prominent CTA with live waveform (match form, idea story); small: compact mic row under any field.
import { Alert, Button } from "../../components/ds";
import { useSession } from "../../session";
import { LIMIT, dictationSupported, useDictation } from "./useDictation";
import { Waveform } from "./Waveform";
import s from "./Dictation.module.css";

type Props = {
  /** Field name for the button label, e.g. "Opis". */
  label: string;
  onText: (text: string) => void;
  big?: boolean;
  bigLabel?: string;
  bigHint?: string;
  stopLabel?: string;
  /** Extra phase after transcription (e.g. LLM parsing the story). */
  busyText?: string;
};

const clock = (n: number) =>
  `${Math.floor(n / 60)}:${String(n % 60).padStart(2, "0")}`;

export function Dictation({
  label,
  onText,
  big,
  bigLabel,
  bigHint,
  stopLabel = "Zakończ i wstaw tekst",
  busyText,
}: Props) {
  const { whisperOk } = useSession();
  const d = useDictation(onText);
  if (!whisperOk || !dictationSupported()) return null;

  const rec = d.phase === "recording";
  const phase = busyText ? "parsing" : d.phase;
  const active =
    phase === "asking" ||
    phase === "recording" ||
    phase === "transcribing" ||
    phase === "parsing";
  const status =
    {
      asking: "Prosimy o dostęp do mikrofonu…",
      recording: `Nagrywam ${clock(d.secs)}`,
      transcribing: "Zamieniamy nagranie na tekst…",
      parsing: busyText ?? "",
    }[phase as string] ?? "";
  const sub = rec
    ? d.warn
      ? `Za ${LIMIT - d.secs} s zakończymy nagrywanie.`
      : "Mów spokojnie. Zatrzymaj, gdy skończysz."
    : phase === "transcribing"
      ? "Tekst trafi do pola — przed wysłaniem możesz go poprawić."
      : "";
  const error = d.phase === "error" && (
    <Alert tone="danger" onClose={d.cancel}>
      {d.error}
    </Alert>
  );

  if (big) {
    return (
      <div className={s.big}>
        {active ? (
          <div className={s.panel} aria-live="polite">
            <div className={s.panelHead}>
              <span className={s.statusBig}>
                {rec && <span aria-hidden="true" className={s.dot} />}
                {status}
              </span>
              {(rec || phase === "asking") && (
                <Button variant="ghost" size="sm" onClick={d.cancel}>
                  Anuluj
                </Button>
              )}
            </div>
            {rec && <Waveform getLevels={d.getLevels} />}
            {sub && <p className={d.warn ? s.subWarn : s.sub}>{sub}</p>}
            {rec && (
              <div>
                <Button
                  iconLeft="square"
                  onClick={d.toggle}
                  aria-pressed="true"
                >
                  {stopLabel}
                </Button>
              </div>
            )}
          </div>
        ) : (
          <div className={s.cta}>
            <Button
              variant="outline"
              size="lg"
              iconLeft="mic"
              onClick={d.toggle}
            >
              {bigLabel ?? "Podyktuj"}
            </Button>
            {bigHint && <span className={s.hint}>{bigHint}</span>}
          </div>
        )}
        {error}
      </div>
    );
  }

  return (
    <div className={s.small}>
      <div className={s.row} aria-live="polite">
        <Button
          variant={rec ? "primary" : "ghost"}
          size="sm"
          iconLeft={rec ? "square" : "mic"}
          disabled={phase === "asking" || phase === "transcribing"}
          onClick={d.toggle}
          aria-pressed={rec ? "true" : "false"}
          aria-label={
            rec ? "Zatrzymaj nagrywanie i wstaw tekst" : `Podyktuj: ${label}`
          }
        >
          {rec ? "Zatrzymaj" : "Podyktuj"}
        </Button>
        {status && (
          <span className={d.warn ? s.statusWarn : s.status}>
            {rec && <span aria-hidden="true" className={s.dot} />}
            {d.warn ? `${status} · za ${LIMIT - d.secs} s koniec` : status}
          </span>
        )}
        {rec && (
          <button
            type="button"
            className={`link-btn ${s.cancel}`}
            onClick={d.cancel}
          >
            Anuluj
          </button>
        )}
      </div>
      {rec && <Waveform getLevels={d.getLevels} compact />}
      {error}
    </div>
  );
}
