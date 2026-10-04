import type { ReactNode } from 'react';
import { useSession } from '../../session';
import { DictationButton } from './DictationButton';
import { appendText } from './useDictation';
import s from './Dictation.module.css';

type Props = { value: string; onChange: (v: string) => void; label?: string; children: ReactNode };

/**
 * Adds voice input to any text field: dictated text is appended, with an Undo toast.
 * DS inputs are vendored, so the mic is overlaid on the field's label row instead of built in.
 */
export function WithDictation({ value, onChange, label, children }: Props) {
  const { showToast } = useSession();
  const insert = (text: string) => {
    const before = value;
    onChange(appendText(before, text));
    showToast('Wstawiono podyktowany tekst.', 'neutral', { label: 'Cofnij', run: () => onChange(before) });
  };
  return (
    <div className={s.field}>
      {children}
      <div className={s.overlay}>
        <DictationButton onText={insert} label={label} />
      </div>
    </div>
  );
}
