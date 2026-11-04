import { createContext, useCallback, useContext, useMemo, useState } from "react";
import Icon from "./Icon.jsx";
import { describeError } from "../utils/errorMessages.js";

const ToastContext = createContext(null);
let seq = 0;

/** Uygulama genelinde kısa bildirimler. useToast().success("..."), .error(apiError) */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const dismiss = useCallback(id => setToasts(list => list.filter(t => t.id !== id)), []);
  const push = useCallback((tone, text) => {
    const id = ++seq;
    setToasts(list => [...list.slice(-3), { id, tone, text }]);
    setTimeout(() => dismiss(id), tone === "error" ? 7000 : 4000);
  }, [dismiss]);
  const api = useMemo(() => ({
    success: text => push("success", text),
    info: text => push("info", text),
    error: errOrText => {
      if (typeof errOrText === "string") return push("error", errOrText);
      const info = describeError(errOrText);
      push("error", info.hint ? `${info.title}. ${info.hint}` : info.title);
    },
  }), [push]);
  return <ToastContext.Provider value={api}>
    {children}
    <div className="toast-stack" aria-live="polite">
      {toasts.map(t => <div key={t.id} className={`toast ${t.tone}`}>
        <Icon name={t.tone === "error" ? "alert" : t.tone === "success" ? "check" : "info"} />
        <span>{t.text}</span>
        <button type="button" aria-label="Kapat" onClick={() => dismiss(t.id)}>×</button>
      </div>)}
    </div>
  </ToastContext.Provider>;
}

export function useToast() {
  return useContext(ToastContext);
}
