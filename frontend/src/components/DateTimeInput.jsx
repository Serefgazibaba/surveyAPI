import { useState } from "react";
import { toZonedInput, fromZonedInput } from "../utils/datetime.js";

const pad = n => String(n).padStart(2, "0");
const HOURS = Array.from({ length: 24 }, (_, i) => pad(i));
const MINUTES = Array.from({ length: 60 }, (_, i) => pad(i));

/**
 * Tarih + saat girişi. Değer UTC ISO string'dir; kullanıcı İstanbul saatiyle girer.
 * Saat 24 saat biçimindedir: tarayıcının yerel saat seçicisi (input type="time") işletim sistemi diline göre
 * AM/PM gösterebildiği için saat ve dakika ayrı listelerden seçilir.
 * Saat otomatik doldurulmaz: tarih, saat ve dakika girilene kadar onChange(null) çağrılır;
 * yarım giriş bileşenin içinde korunur.
 * onChange(isoOrNull)
 */
export default function DateTimeInput({ value, onChange, disabled, invalid = false }) {
  const split = iso => {
    const { date, time } = toZonedInput(iso);
    const [hour = "", minute = ""] = time ? time.split(":") : [];
    return { date, hour, minute };
  };
  const [local, setLocal] = useState(() => split(value));
  const [lastValue, setLastValue] = useState(value);
  // Değer dışarıdan değiştiyse (ör. kayıttan dönen yanıt) yerel girişi ona göre güncelle.
  if (value !== lastValue) {
    setLastValue(value);
    if (value) setLocal(split(value));
  }

  const update = patch => {
    const next = { ...local, ...patch };
    setLocal(next);
    const iso = next.date && next.hour && next.minute ? fromZonedInput(next.date, `${next.hour}:${next.minute}`) : null;
    setLastValue(iso);
    onChange?.(iso);
  };

  const cls = invalid ? "has-error" : undefined;
  return <div className="two-inputs">
    <input type="date" aria-label="Tarih" className={cls} value={local.date} disabled={disabled} onChange={e => update({ date: e.target.value })} />
    <div className={`time-24${invalid ? " has-error" : ""}`}>
      <select aria-label="Saat" value={local.hour} disabled={disabled} onChange={e => update({ hour: e.target.value })}>
        <option value="">SS</option>
        {HOURS.map(h => <option key={h} value={h}>{h}</option>)}
      </select>
      <span aria-hidden="true">:</span>
      <select aria-label="Dakika" value={local.minute} disabled={disabled} onChange={e => update({ minute: e.target.value })}>
        <option value="">DD</option>
        {MINUTES.map(m => <option key={m} value={m}>{m}</option>)}
      </select>
    </div>
  </div>;
}
