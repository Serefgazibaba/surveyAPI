import { useCallback, useEffect, useRef, useState } from "react";

/**
 * Sayfa verisi yükleme hook'u. fn değiştiğinde (deps) yeniden çalışır; eski yanıtlar yok sayılır.
 * → { data, error, loading, reload, setData }
 */
export default function useAsync(fn, deps = [], { enabled = true } = {}) {
  const [state, setState] = useState({ data: null, error: null, loading: enabled });
  const callId = useRef(0);

  const run = useCallback(() => {
    const id = ++callId.current;
    setState(s => ({ ...s, loading: true, error: null }));
    return fn().then(
      data => { if (id === callId.current) setState({ data, error: null, loading: false }); return data; },
      error => { if (id === callId.current) setState(s => ({ ...s, error, loading: false })); },
    );
  }, deps); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { if (enabled) run(); }, [run, enabled]);

  const setData = useCallback(updater => setState(s => ({ ...s, data: typeof updater === "function" ? updater(s.data) : updater })), []);
  return { ...state, reload: run, setData };
}
