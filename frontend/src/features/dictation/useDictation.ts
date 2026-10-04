// Voice dictation: record with MediaRecorder, send to /api/v1/transcribe, hand text to onText.
// One recorder at a time app-wide: starting a new one stops the previous.
import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '../../api/client';
import { errorMessage } from '../../lib/errors';

export type DictationState = 'idle' | 'asking' | 'recording' | 'transcribing' | 'error';

export const MAX_SECONDS = 120;
const MIME = ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/mp4'];

export const dictationSupported = () =>
  typeof window !== 'undefined' && 'MediaRecorder' in window && !!navigator.mediaDevices?.getUserMedia;

let stopActive: (() => void) | null = null;

export function useDictation(onText: (text: string) => void, language = 'pl') {
  const [state, setState] = useState<DictationState>('idle');
  const [seconds, setSeconds] = useState(0);
  const [error, setError] = useState('');
  const rec = useRef<MediaRecorder | null>(null);
  const cancelled = useRef(false);
  const tick = useRef<number>(undefined);
  const onTextRef = useRef(onText);
  useEffect(() => {
    onTextRef.current = onText;
  });

  const stop = useCallback(() => {
    if (rec.current?.state === 'recording') rec.current.stop();
  }, []);
  const cancel = useCallback(() => {
    cancelled.current = true;
    stop();
  }, [stop]);

  const start = useCallback(async () => {
    stopActive?.();
    setError('');
    setState('asking');
    let stream: MediaStream;
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true } });
    } catch (e) {
      setState('error');
      setError(
        e instanceof DOMException && e.name === 'NotAllowedError'
          ? 'Brak zgody na mikrofon. Zezwól na dostęp w ustawieniach przeglądarki.'
          : 'Nie udało się włączyć mikrofonu.',
      );
      return;
    }
    const mimeType = MIME.find((m) => MediaRecorder.isTypeSupported(m));
    const r = new MediaRecorder(stream, mimeType ? { mimeType } : undefined);
    const chunks: Blob[] = [];
    cancelled.current = false;
    r.ondataavailable = (e) => e.data.size && chunks.push(e.data);
    r.onstop = async () => {
      clearInterval(tick.current);
      stream.getTracks().forEach((t) => t.stop()); // release the mic (browser's red dot)
      if (stopActive === stop) stopActive = null;
      rec.current = null;
      if (cancelled.current || !chunks.length) return setState('idle');
      setState('transcribing');
      try {
        const { text } = await api.transcribe(new Blob(chunks, { type: r.mimeType }), language);
        if (!text.trim()) throw new Error('Nic nie usłyszeliśmy. Spróbuj mówić bliżej mikrofonu.');
        onTextRef.current(text.trim());
        setState('idle');
      } catch (e) {
        setState('error');
        setError(errorMessage(e));
      }
    };
    rec.current = r;
    stopActive = stop;
    r.start();
    setSeconds(0);
    setState('recording');
    const t0 = Date.now();
    tick.current = window.setInterval(() => {
      const s = Math.floor((Date.now() - t0) / 1000);
      setSeconds(s);
      if (s >= MAX_SECONDS) stop();
    }, 250);
  }, [language, stop]);

  // Leaving the screen mid-recording: discard and free the mic.
  useEffect(() => cancel, [cancel]);

  return { state, seconds, error, start, stop, cancel };
}

/** Append dictated text to existing field content (never replaces what the user typed). */
export const appendText = (current: string, text: string) => {
  const base = current.trimEnd();
  return base ? `${base} ${text}` : text;
};
