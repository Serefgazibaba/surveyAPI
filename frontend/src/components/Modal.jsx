import { useEffect } from "react";

/**
 * Genel modal. × butonu ve Escape `onClose` çağırır (busy iken kapanmaz).
 * Arka plana tıklamak kapatmaz: metin seçerken fare dışarıda bırakıldığında form yanlışlıkla kapanıyordu.
 * footer: alt butonlar; wide: geniş içerik için.
 */
export default function Modal({ title, subtitle, children, footer, onClose, busy = false, wide = false }) {
  useEffect(() => {
    const onKey = e => { if (e.key === "Escape" && !busy) onClose?.(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose, busy]);
  return <div className="modal-backdrop">
    <div className={`modal${wide ? " wide" : ""}`} role="dialog" aria-modal="true" aria-label={title}>
      <div className="modal-title">
        <div><h2>{title}</h2>{subtitle && <p>{subtitle}</p>}</div>
        <button type="button" aria-label="Kapat" onClick={() => !busy && onClose?.()}>×</button>
      </div>
      <div className="modal-content">{children}</div>
      {footer && <div className="modal-footer">{footer}</div>}
    </div>
  </div>;
}
