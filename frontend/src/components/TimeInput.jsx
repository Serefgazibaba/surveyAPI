import { useEffect, useRef, useState } from "react";
import Icon from "./Icon.jsx";
import { nowIstanbulTime } from "../utils/datetime.js";

const pad = n => String(n).padStart(2, "0");
const HOURS = Array.from({ length: 24 }, (_, i) => pad(i));
const MINUTES = Array.from({ length: 60 }, (_, i) => pad(i));

/** Kaydırılabilir değer sütunu; seçili değer ortada vurgulanır. */
function Column({ label, values, selected, onSelect, open }) {
  const listRef = useRef(null);
  useEffect(() => {
    if (!open) return;
    const el = listRef.current?.querySelector('[aria-selected="true"]');
    // Açılışta ve seçim değişince seçili değeri sütunun ortasına getir.
    if (el) listRef.current.scrollTop = el.offsetTop - listRef.current.clientHeight / 2 + el.clientHeight / 2;
  }, [open, selected]);
  return <div className="time-col">
    <div className="time-col-label">{label}</div>
    <div className="time-col-list" ref={listRef} role="listbox" aria-label={label}>
      {values.map(v => <button key={v} type="button" role="option" aria-selected={v === selected}
        className={`time-cell${v === selected ? " selected" : ""}`} onClick={() => onSelect(v)}>{v}</button>)}
    </div>
  </div>;
}

/**
 * 24 saatlik saat seçimi (SS:DD). Alan tıklanınca saat ve dakika sütunlarından oluşan seçici açılır;
 * "Şimdi" İstanbul saatini, "Temizle" boş değeri seçer.
 * hour / minute: "00".."23" / "00".."59" veya ""; onChange({ hour, minute })
 */
export default function TimeInput({ hour = "", minute = "", onChange, disabled, invalid = false }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    if (!open) return;
    const onDown = e => { if (!ref.current?.contains(e.target)) setOpen(false); };
    const onKey = e => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => { document.removeEventListener("mousedown", onDown); document.removeEventListener("keydown", onKey); };
  }, [open]);

  const set = patch => onChange?.({ hour, minute, ...patch });
  const display = hour || minute ? `${hour || "SS"}:${minute || "DD"}` : "";

  return <div ref={ref} className={`time-input${invalid ? " has-error" : ""}${disabled ? " disabled" : ""}${open ? " open" : ""}`}>
    <button type="button" className="time-input-trigger" disabled={disabled} aria-haspopup="dialog" aria-expanded={open}
      aria-label={display ? `Saat ${display}` : "Saat seç"} onClick={() => setOpen(o => !o)}>
      <span className={display ? "" : "placeholder"}>{display || "SS:DD"}</span>
      <Icon name="clock" />
    </button>
    {open && <div className="time-popover" role="dialog" aria-label="Saat seç">
      <div className="time-cols">
        <Column label="Saat" values={HOURS} selected={hour} open={open} onSelect={h => set({ hour: h, minute: minute || "00" })} />
        <Column label="Dakika" values={MINUTES} selected={minute} open={open} onSelect={m => set({ minute: m, hour: hour || "00" })} />
      </div>
      <div className="time-actions">
        <button type="button" onClick={() => { set(nowIstanbulTime()); setOpen(false); }}>Şimdi</button>
        <button type="button" className="muted" onClick={() => { set({ hour: "", minute: "" }); setOpen(false); }}>Temizle</button>
      </div>
    </div>}
  </div>;
}
