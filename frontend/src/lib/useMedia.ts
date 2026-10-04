import { useSyncExternalStore } from 'react';

/** `useMedia('(max-width: 959px)')` → boolean, live. */
export function useMedia(query: string) {
  return useSyncExternalStore(
    (cb) => {
      const m = matchMedia(query);
      m.addEventListener('change', cb);
      return () => m.removeEventListener('change', cb);
    },
    () => matchMedia(query).matches,
  );
}
