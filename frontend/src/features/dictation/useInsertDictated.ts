import { useSession } from '../../session';
import { appendText } from './useDictation';

/** Returns a factory: insert dictated text into a field, with an Undo toast (prototype insertText). */
export function useInsertDictated() {
  const { showToast } = useSession();
  return (get: () => string, set: (v: string) => void, multi = false) =>
    (text: string) => {
      const prev = get();
      set(appendText(prev, text, multi));
      showToast('Wstawiono tekst z nagrania. Sprawdź go przed wysłaniem.', 'success', {
        label: 'Cofnij',
        run: () => set(prev),
      });
    };
}
