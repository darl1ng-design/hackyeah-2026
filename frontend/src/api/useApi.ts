// Fetch hook: { data, error, loading, reload, setData }. Re-runs when `deps` change; stale responses ignored.
import { useCallback, useEffect, useState, type DependencyList } from 'react';

type State<T> = { key: string; data: T | null; error: unknown };

export function useApi<T>(fn: () => Promise<T>, deps: DependencyList) {
  const [tick, setTick] = useState(0);
  const key = JSON.stringify([...deps, tick]);
  const [state, setState] = useState<State<T>>({ key: '', data: null, error: null });

  useEffect(() => {
    let live = true;
    fn().then(
      (data) => live && setState({ key, data, error: null }),
      (error) => live && setState({ key, data: null, error }),
    );
    return () => {
      live = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  const setData = useCallback((data: T) => setState((s) => ({ ...s, data })), []);
  // loading = no response yet for current deps; keep showing previous data meanwhile
  return { data: state.data, error: state.key === key ? state.error : null, loading: state.key !== key, reload, setData };
}
