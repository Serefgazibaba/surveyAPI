import { useEffect } from "react";

/**
 * Genel modal. Escape ve arka plana tıklama `onClose` çağırır (busy iken kapanmaz).
 * footer: alt butonlar; wide: geniş içerik için.
 */
export default function Modal({ title, subtitle, children, footer, onClose, busy = false, wide = false }) {
  useEffect(() => {
    const onKey = e => { if (e.key === "Escape" && !busy) onClose?.(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose, busy]);
  return <div className="modal-backdrop" onClick={() => !busy && onClose?.()}>
    <div className={`modal${wide ? " wide" : ""}`} role="dialog" aria-modal="true" aria-label={title} onClick={e => e.stopPropagation()}>
      <div className="modal-title">
        <div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>
        <button type="button" aria-label="Kapat" onClick={() => !busy && onClose?.()}>×</button>
      </div>
      <div className="modal-content">{children}</div>
      {footer && <div className="modal-footer">{footer}</div>}
    </div>
  </div>;
}
