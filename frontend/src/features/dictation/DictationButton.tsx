import { IconButton } from '../../components/ds';
import { MAX_SECONDS, dictationSupported, useDictation } from './useDictation';
import s from './Dictation.module.css';

type Props = { onText: (text: string) => void; label?: string };

const clock = (n: number) => `${Math.floor(n / 60)}:${String(n % 60).padStart(2, '0')}`;

/** Mic toggle: click to record, click again to stop and insert. Hidden where recording is unsupported. */
export function DictationButton({ onText, label = 'Podyktuj' }: Props) {
  const d = useDictation(onText);
  if (!dictationSupported()) return null;
  const recording = d.state === 'recording';
  const busy = d.state === 'asking' || d.state === 'transcribing';
  const status = recording
    ? `Nagrywam ${clock(d.seconds)}${d.seconds >= MAX_SECONDS - 10 ? ' — zaraz kończę' : ''}`
    : d.state === 'transcribing'
      ? 'Rozpoznaję mowę…'
      : d.state === 'error'
        ? d.error
        : '';

  return (
    <span className={s.wrap}>
      {status && (
        <span role="status" className={d.state === 'error' ? s.error : s.status}>
          {recording && <span aria-hidden="true" className={s.dot} />}
          {status}
        </span>
      )}
      {recording && (
        <button type="button" className={`link-btn ${s.cancel}`} onClick={d.cancel}>
          Anuluj
        </button>
      )}
      <IconButton
        icon={recording ? 'square' : d.state === 'transcribing' ? 'loader' : 'mic'}
        label={recording ? 'Zakończ nagrywanie i wstaw tekst' : label}
        variant={recording ? 'primary' : 'outline'}
        size="sm"
        disabled={busy}
        aria-pressed={recording}
        onClick={recording ? d.stop : d.start}
      />
    </span>
  );
}
