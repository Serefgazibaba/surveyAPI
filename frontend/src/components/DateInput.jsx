import { useEffect, useRef, useState } from "react";
import Icon from "./Icon.jsx";
import { todayYmd } from "../utils/datetime.js";

const MONTHS = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];
const DAYS = ["Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"];
const pad = n => String(n).padStart(2, "0");

/** "YYYY-MM-DD" → "GG.AA.YYYY" */
const toDisplay = ymd => {
  if (!ymd) return "";
  const [y, m, d] = ymd.split("-");
  return `${d}.${m}.${y}`;
};

/** "GG.AA.YYYY" → "YYYY-MM-DD"; geçersiz veya eksikse null (31.02 gibi tarihler reddedilir). */
const toYmd = text => {
  const m = text.match(/^(\d{2})\.(\d{2})\.(\d{4})$/);
  if (!m) return null;
  const [, d, mo, y] = m.map(Number);
  const date = new Date(Date.UTC(y, mo - 1, d));
  if (date.getUTCFullYear() !== y || date.getUTCMonth() !== mo - 1 || date.getUTCDate() !== d) return null;
  return `${y}-${pad(mo)}-${pad(d)}`;
};

/** Yazarken noktaları otomatik ekler: "01122026" → "01.12.2026". */
const mask = raw => {
  const digits = raw.replace(/\D/g, "").slice(0, 8);
  return [digits.slice(0, 2), digits.slice(2, 4), digits.slice(4, 8)].filter(Boolean).join(".");
};

function Calendar({ value, min, onPick, onClose }) {
  const start = value || min || todayYmd();
  const [view, setView] = useState({ y: +start.slice(0, 4), m: +start.slice(5, 7) - 1 });
  const first = new Date(Date.UTC(view.y, view.m, 1));
  const offset = (first.getUTCDay() + 6) % 7; // pazartesi = 0
  const daysInMonth = new Date(Date.UTC(view.y, view.m + 1, 0)).getUTCDate();
  const cells = [...Array(offset).fill(null), ...Array.from({ length: daysInMonth }, (_, i) => i + 1)];
  const shift = d => setView(v => {
    const m = v.m + d;
    return { y: v.y + Math.floor(m / 12), m: ((m % 12) + 12) % 12 };
  });
  const today = todayYmd();

  return <div className="calendar" role="dialog" aria-label="Tarih seç" onKeyDown={e => e.key === "Escape" && onClose()}>
    <div className="calendar-head">
      <button type="button" aria-label="Önceki ay" onClick={() => shift(-1)}><Icon name="left" /></button>
      <b>{MONTHS[view.m]} {view.y}</b>
      <button type="button" aria-label="Sonraki ay" onClick={() => shift(1)}><Icon name="right" /></button>
    </div>
    <div className="calendar-grid">
      {DAYS.map(d => <span key={d} className="calendar-dow">{d}</span>)}
      {cells.map((day, i) => {
        if (!day) return <span key={`e${i}`} />;
        const ymd = `${view.y}-${pad(view.m + 1)}-${pad(day)}`;
        const disabled = min && ymd < min;
        const cls = ["calendar-day", ymd === value && "selected", ymd === today && "today"].filter(Boolean).join(" ");
        return <button key={ymd} type="button" className={cls} disabled={disabled} aria-label={`${day} ${MONTHS[view.m]} ${view.y}`}
          aria-pressed={ymd === value} onClick={() => onPick(ymd)}>{day}</button>;
      })}
    </div>
  </div>;
}

/**
 * Türkçe tarih girişi (GG.AA.YYYY): yazarak veya takvimden seçilir. Tarayıcının yerel tarih seçicisi
 * işletim sistemi diline göre biçim gösterdiği için kullanılmaz.
 * value / onChange: "YYYY-MM-DD" veya "" (boş ya da yarım/geçersiz giriş).
 * min: "YYYY-MM-DD"; takvimde öncesi seçilemez.
 */
export default function DateInput({ value = "", onChange, disabled, invalid = false, min }) {
  const [text, setText] = useState(toDisplay(value));
  const [lastValue, setLastValue] = useState(value);
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  // Değer dışarıdan değiştiyse metni ona göre güncelle (yarım girişi ezmemek için yalnızca dolu değerde).
  if (value !== lastValue) {
    setLastValue(value);
    if (value) setText(toDisplay(value));
  }

  useEffect(() => {
    if (!open) return;
    const close = e => { if (!ref.current?.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [open]);

  const emit = ymd => {
    setLastValue(ymd);
    onChange?.(ymd);
  };

  return <div ref={ref} className={`date-input${invalid ? " has-error" : ""}${disabled ? " disabled" : ""}`}>
    <input type="text" inputMode="numeric" aria-label="Tarih" placeholder="gg.aa.yyyy" maxLength={10} value={text} disabled={disabled}
      onChange={e => {
        const next = mask(e.target.value);
        setText(next);
        emit(toYmd(next) || "");
      }} />
    <button type="button" className="date-input-toggle" aria-label="Takvimi aç" aria-expanded={open} disabled={disabled}
      onClick={() => setOpen(o => !o)}><Icon name="calendar" /></button>
    {open && <Calendar value={toYmd(text)} min={min} onClose={() => setOpen(false)}
      onPick={ymd => { setText(toDisplay(ymd)); emit(ymd); setOpen(false); }} />}
  </div>;
}
