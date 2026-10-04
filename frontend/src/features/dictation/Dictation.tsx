// Dictation control placed under a field (prototype component "Dyktowanie").
// big: prominent CTA with live waveform (match form, idea story); small: compact mic row under any field.
// A11y: one always-mounted sr-only live region announces phase changes (never the ticking clock),
// Escape cancels, focus never drops to <body> when the CTA/stop buttons swap, fieldId wires
// aria-controls and gets focus after the text lands so screen-reader users can review it.
import { useEffect, useRef, type KeyboardEvent } from "react";
import { Alert, Button } from "../../components/ds";
import { useSession } from "../../session";
import {
  LIMIT,
  WARN_AT,
  dictationSupported,
  useDictation,
} from "./useDictation";
import { Waveform } from "./Waveform";
import s from "./Dictation.module.css";

type Props = {
  /** Field name for the button label, e.g. "Opis". */
  label: string;
  onText: (text: string) => void;
  /** id of the field the text goes into: aria-controls + focus after insert. */
  fieldId?: string;
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
  fieldId,
  big,
  bigLabel,
  bigHint,
  stopLabel = "Zakończ i wstaw tekst",
  busyText,
}: Props) {
  const { whisperOk } = useSession();
  const inserted = useRef(false);
  const d = useDictation((text) => {
    onText(text);
    inserted.current = true;
    if (!big && fieldId)
      requestAnimationFrame(() => document.getElementById(fieldId)?.focus());
  });
  const ctaRef = useRef<HTMLButtonElement>(null);
  const stopRef = useRef<HTMLButtonElement>(null);
  const wasActive = useRef(false);

  const rec = d.phase === "recording";
  const phase = busyText ? "parsing" : d.phase;
  const active =
    phase === "asking" ||
    phase === "recording" ||
    phase === "transcribing" ||
    phase === "parsing";

  // big: the CTA unmounts while active, so move focus to stop, and back to the CTA after.
  useEffect(() => {
    if (!big) return;
    if (rec) stopRef.current?.focus();
    else if (wasActive.current && !active) {
      // text landed → review it in the field; cancelled, failed or story-parsed → back to the CTA.
      const field =
        inserted.current && fieldId ? document.getElementById(fieldId) : null;
      (field ?? ctaRef.current)?.focus();
      inserted.current = false;
    }
    wasActive.current = active;
  }, [big, rec, active, fieldId]);

  if (!whisperOk || !dictationSupported()) return null;

  const status =
    {
      asking: "Prosimy o dostęp do mikrofonu…",
      recording: `Nagrywam ${clock(d.secs)}`,
      transcribing: "Zamieniamy nagranie na tekst…",
      parsing: busyText ?? "",
    }[phase as string] ?? "";
  const announce =
    {
      asking: "Prosimy o dostęp do mikrofonu.",
      recording: d.warn
        ? `Za ${LIMIT - WARN_AT} sekund zakończymy nagrywanie.`
        : "Nagrywanie trwa. Naciśnij ten sam przycisk, aby zakończyć, albo Escape, aby anulować.",
      transcribing: "Zamieniamy nagranie na tekst.",
      parsing: busyText ?? "",
    }[phase as string] ?? "";
  const sub = rec
    ? d.warn
      ? `Za ${LIMIT - d.secs} s zakończymy nagrywanie.`
      : "Mów spokojnie. Zatrzymaj, gdy skończysz. Escape anuluje."
    : phase === "transcribing"
      ? "Tekst trafi do pola; przed wysłaniem możesz go poprawić."
      : "";
  const error = d.phase === "error" && (
    <Alert tone="danger" onClose={d.cancel}>
      {d.error}
    </Alert>
  );
  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === "Escape" && (rec || phase === "asking")) {
      e.stopPropagation();
      d.cancel();
    }
  };
  // Phase is announced only when it changes; the warning flips once at WARN_AT.
  const live = (
    <span className="sr-only" role="status" aria-live="polite">
      {announce}
    </span>
  );

  if (big) {
    return (
      <div className={s.big} onKeyDown={onKeyDown}>
        {live}
        {active ? (
          <div className={s.panel}>
            <div className={s.panelHead}>
              <span className={s.statusBig}>
                {rec && <span aria-hidden="true" className={s.dot} />}
                {status}
              </span>
              {(rec || phase === "asking") && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={d.cancel}
                  aria-label="Anuluj nagrywanie"
                >
                  Anuluj
                </Button>
              )}
            </div>
            {rec && <Waveform getLevels={d.getLevels} />}
            {sub && <p className={d.warn ? s.subWarn : s.sub}>{sub}</p>}
            {rec && (
              <div>
                <Button
                  ref={stopRef}
                  iconLeft="square"
                  onClick={d.toggle}
                  aria-controls={fieldId}
                >
                  {stopLabel}
                </Button>
              </div>
            )}
          </div>
        ) : (
          <div className={s.cta}>
            <Button
              ref={ctaRef}
              variant="outline"
              size="lg"
              iconLeft="mic"
              onClick={d.toggle}
              aria-controls={fieldId}
              aria-describedby={bigHint ? `${ctaId(label)}-hint` : undefined}
            >
              {bigLabel ?? "Podyktuj"}
            </Button>
            {bigHint && (
              <span id={`${ctaId(label)}-hint`} className={s.hint}>
                {bigHint}
              </span>
            )}
          </div>
        )}
        {error}
      </div>
    );
  }

  const busy = phase === "asking" || phase === "transcribing";
  return (
    <div className={s.small} onKeyDown={onKeyDown}>
      {live}
      <div className={s.row}>
        {/* aria-disabled, not disabled: a disabled button drops keyboard focus mid-flow. */}
        <Button
          variant={rec ? "primary" : "ghost"}
          size="sm"
          iconLeft={rec ? "square" : "mic"}
          aria-disabled={busy || undefined}
          onClick={d.toggle}
          aria-controls={fieldId}
          aria-label={
            rec
              ? `Zatrzymaj nagrywanie i wstaw tekst: ${label}`
              : `Podyktuj: ${label}`
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
            aria-label={`Anuluj nagrywanie: ${label}`}
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

const ctaId = (label: string) =>
  "dict-" + label.toLowerCase().replace(/[^a-z0-9ąćęłńóśźż]+/g, "-");
