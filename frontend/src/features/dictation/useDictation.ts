// Voice dictation (port of prototype hub-dictation.js): MediaRecorder → /api/v1/transcribe → onText.
// Phases: idle → asking → recording → transcribing → idle | error. One recorder app-wide:
// starting another field stops the previous one, which still transcribes into its own field.
import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, api } from "../../api/client";

export type Phase = "idle" | "asking" | "recording" | "transcribing" | "error";
export type Levels = {
  start: number;
  now: number;
  samples: [number, number][];
};

export const LIMIT = 120;
export const WARN_AT = 110;

const MSG = {
  silence: "Nic nie usłyszeliśmy. Spróbuj mówić bliżej mikrofonu.",
  size: "Nagranie jest za długie. Nagraj krótszą wypowiedź (do 2 minut).",
  rate: "Za dużo nagrań w krótkim czasie. Spróbuj ponownie za minutę.",
  down: "Rozpoznawanie mowy jest chwilowo niedostępne. Wpisz tekst ręcznie.",
  other:
    "Nie udało się przetworzyć nagrania. Spróbuj ponownie albo wpisz tekst ręcznie.",
  permission:
    "Nie mamy dostępu do mikrofonu. Zezwól na niego w ustawieniach przeglądarki albo wpisz tekst ręcznie.",
  device: "Nie znaleźliśmy mikrofonu. Podłącz go albo wpisz tekst ręcznie.",
  unsupported: "Ta przeglądarka nie obsługuje nagrywania. Wpisz tekst ręcznie.",
};
const MIME = [
  "audio/webm;codecs=opus",
  "audio/webm",
  "audio/ogg;codecs=opus",
  "audio/mp4",
];

export const dictationSupported = () =>
  typeof window !== "undefined" &&
  "MediaRecorder" in window &&
  !!navigator.mediaDevices?.getUserMedia &&
  "AudioContext" in window;

/** Append dictated text; in multi-line fields a finished sentence starts a new paragraph. */
export function appendText(prev: string, text: string, multi = false) {
  const t = text.trim();
  const p = prev.replace(/\s+$/, "");
  if (!t) return prev;
  if (!p) return t;
  return multi && /[.!?…]$/.test(p) ? `${p}\n\n${t}` : `${p} ${t}`;
}

const errorKind = (e: unknown): keyof typeof MSG => {
  const st = e instanceof ApiError ? e.status : 0;
  return st === 400
    ? "silence"
    : st === 413
      ? "size"
      : st === 429
        ? "rate"
        : st === 503
          ? "down"
          : "other";
};

let stopActive: (() => void) | null = null;

export function useDictation(onText: (text: string) => void) {
  const [phase, setPhase] = useState<Phase>("idle");
  const [secs, setSecs] = useState(0);
  const [error, setError] = useState("");
  const session = useRef<{
    rec: MediaRecorder;
    stream: MediaStream;
    ac: AudioContext;
    timers: number[];
    levels: Levels;
    cancelled: boolean;
  } | null>(null);
  const onTextRef = useRef(onText);
  useEffect(() => {
    onTextRef.current = onText;
  });

  const fail = (kind: keyof typeof MSG) => {
    setPhase("error");
    setError(MSG[kind]);
  };

  const stop = useCallback(() => {
    if (session.current?.rec.state === "recording") session.current.rec.stop();
  }, []);
  const cancel = useCallback(() => {
    if (session.current) session.current.cancelled = true;
    stop();
    setPhase("idle");
  }, [stop]);

  const start = useCallback(async () => {
    stopActive?.();
    if (!dictationSupported()) return fail("unsupported");
    setError("");
    setPhase("asking");
    let stream: MediaStream;
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch (e) {
      const name = e instanceof DOMException ? e.name : "";
      return fail(
        name === "NotAllowedError" || name === "SecurityError"
          ? "permission"
          : "device",
      );
    }
    const mimeType = MIME.find((m) => MediaRecorder.isTypeSupported(m));
    const rec = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
    const chunks: Blob[] = [];
    // Input level for the waveform: RMS → dB → 0..1, sampled every 50 ms, last 31 s kept.
    const ac = new AudioContext();
    const an = ac.createAnalyser();
    an.fftSize = 1024;
    ac.createMediaStreamSource(stream).connect(an);
    const buf = new Float32Array(an.fftSize);
    const t0 = performance.now();
    const levels: Levels = { start: t0, now: t0, samples: [] };
    const s = {
      rec,
      stream,
      ac,
      levels,
      cancelled: false,
      timers: [] as number[],
    };
    session.current = s;
    s.timers.push(
      window.setInterval(() => {
        an.getFloatTimeDomainData(buf);
        let sum = 0;
        for (const v of buf) sum += v * v;
        const db = 20 * Math.log10(Math.sqrt(sum / buf.length) + 1e-9);
        const now = performance.now();
        levels.now = now;
        levels.samples.push([now, Math.max(0, Math.min(1, (db + 55) / 40))]);
        while (levels.samples.length && levels.samples[0][0] < now - 31000)
          levels.samples.shift();
      }, 50),
      window.setInterval(() => {
        const n = Math.floor((performance.now() - t0) / 1000);
        setSecs(n);
        if (n >= LIMIT) stop();
      }, 250),
    );
    rec.ondataavailable = (e) => e.data.size && chunks.push(e.data);
    rec.onstop = async () => {
      s.timers.forEach(clearInterval);
      stream.getTracks().forEach((t) => t.stop()); // release the mic (browser's red dot)
      ac.close().catch(() => {});
      if (stopActive === stop) stopActive = null;
      if (session.current === s) session.current = null;
      if (s.cancelled || !chunks.length) return;
      setPhase("transcribing");
      try {
        const { text } = await api.transcribe(
          new Blob(chunks, { type: rec.mimeType }),
          "pl",
        );
        if (!text.trim()) throw new ApiError(400, MSG.silence);
        setPhase("idle");
        onTextRef.current(text.trim());
      } catch (e) {
        fail(errorKind(e));
      }
    };
    stopActive = stop;
    rec.start(1000);
    setSecs(0);
    setPhase("recording");
  }, [stop]);

  // Leaving the screen mid-recording: discard and free the mic.
  useEffect(() => cancel, [cancel]);

  const toggle = useCallback(() => {
    if (phase === "recording") stop();
    else if (phase !== "asking" && phase !== "transcribing") start();
  }, [phase, start, stop]);

  const getLevels = useCallback(() => session.current?.levels ?? null, []);

  return {
    phase,
    secs,
    warn: phase === "recording" && secs >= WARN_AT,
    error,
    toggle,
    cancel,
    getLevels,
  };
}
